package com.networker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Getter
@Setter
@Slf4j
public class TcpService {
    
    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private int serverPort = 12345;
    
    // Armazena as conexões dos clientes: username -> socket
    private Map<String, ClientConnection> connectedClients = new ConcurrentHashMap<>();
    
    @Autowired
    private WebSocketService webSocketService;
    
    public String sendMessageToServer(String serverAddress, int port, String message) {
        try (Socket socket = new Socket(serverAddress, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro: " + e.getMessage();
        }
    }
    
    public boolean sendMessageToUser(String sender, String recipient, String message) {
        log.debug("Attempting to send message from {} to {}. Current users: {}", 
                 sender, recipient, String.join(", ", connectedClients.keySet()));
                 
        ClientConnection connection = connectedClients.get(recipient);
        if (connection == null) {
            log.warn("Recipient {} not found in connected clients", recipient);
            return false;
        }
        
        try {
            PrintWriter out = connection.getWriter();
            out.println(String.format("DE %s: %s", sender, message));
            
            // Notifica via WebSocket
            webSocketService.notifyPrivateMessage(sender, recipient, message);
            log.debug("Message sent successfully from {} to {}", sender, recipient);
            return true;
        } catch (Exception e) {
            log.error("Error sending message from {} to {}", sender, recipient, e);
            return false;
        }
    }
    
    public boolean isUserConnected(String username) {
        return connectedClients.containsKey(username);
    }
    
    public void startServer() {
        if (isServerRunning) {
            return;
        }
        
        isServerRunning = true;
        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                System.out.println("Servidor TCP está rodando na porta: " + serverPort);
                
                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    CompletableFuture.runAsync(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public void stopServer() {
        isServerRunning = false;
        
        // Fecha todas as conexões
        for (ClientConnection connection : connectedClients.values()) {
            try {
                connection.getSocket().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        connectedClients.clear();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try {
            log.info("Client connected: {}", clientSocket.getInetAddress());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Autenticação: primeira mensagem deve ser "LOGIN:username"
            String loginMessage = reader.readLine();
            log.debug("Received login message: {}", loginMessage);
            
            if (loginMessage == null || !loginMessage.startsWith("LOGIN:")) {
                writer.println("ERRO: Login necessário. Formato: LOGIN:username");
                clientSocket.close();
                return;
            }
            
            String username = loginMessage.substring(6).trim();
            if (username.isEmpty()) {
                writer.println("ERRO: Username não pode ser vazio");
                clientSocket.close();
                return;
            }
            
            // Check for duplicate username
            if (connectedClients.containsKey(username)) {
                log.warn("Login attempt with duplicate username: {}", username);
                writer.println("ERRO: Nome de usuário já em uso");
                clientSocket.close();
                return;
            }
            
            // Armazena a conexão do cliente
            ClientConnection connection = new ClientConnection(clientSocket, reader, writer);
            connectedClients.put(username, connection);
            
            log.info("User {} connected successfully. Total connected: {}", 
                    username, connectedClients.size());
                    
            writer.println("SUCESSO: Logado como " + username);
            
            // Lista de usuários online
            StringBuilder userList = new StringBuilder("USUARIOS_ONLINE:");
            connectedClients.keySet().forEach(user -> userList.append(" ").append(user));
            writer.println(userList.toString());
            
            // Loop para receber mensagens do cliente
            String receivedMessage;
            while ((receivedMessage = reader.readLine()) != null) {
                System.out.println("[" + username + "]: " + receivedMessage);
                
                if (receivedMessage.equals("SAIR")) {
                    break;
                } else if (receivedMessage.startsWith("MSG:")) {
                    // Formato: MSG:recipient:message
                    String[] parts = receivedMessage.split(":", 3);
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        String content = parts[2];
                        
                        if (!sendMessageToUser(username, recipient, content)) {
                            writer.println("ERRO: Não foi possível enviar mensagem para " + recipient);
                        }
                    } else {
                        writer.println("ERRO: Formato de mensagem inválido. Use MSG:destinatario:mensagem");
                    }
                } else if (receivedMessage.equals("LISTAR_USUARIOS")) {
                    // Enviar lista atualizada de usuários
                    StringBuilder updatedList = new StringBuilder("USUARIOS_ONLINE:");
                    connectedClients.keySet().forEach(user -> updatedList.append(" ").append(user));
                    writer.println(updatedList.toString());
                } else if (receivedMessage.startsWith("BROADCAST:")) {
                    // Mensagem para todos
                    String content = receivedMessage.substring(10);
                    broadcastMessage(username, content);
                } else {
                    // Formato desconhecido
                    writer.println("ERRO: Comando inválido. Comandos válidos: MSG:destinatario:mensagem, SAIR, LISTAR_USUARIOS, BROADCAST:mensagem");
                }
                
                // Notificar WebSocket sobre a nova mensagem
                webSocketService.notifyTcpMessage(username, receivedMessage);
            }
            
            // Quando o cliente sai, removê-lo da lista sem notificar os outros
            connectedClients.remove(username);
            
            clientSocket.close();
        } catch (Exception e) {
            log.error("Error handling client connection", e);
        }
    }
    
    private void broadcastMessage(String sender, String message) {
        for (Map.Entry<String, ClientConnection> entry : connectedClients.entrySet()) {
            try {
                PrintWriter writer = entry.getValue().getWriter();
                writer.println("BROADCAST " + sender + ": " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Notificar também via WebSocket
        webSocketService.notifyBroadcastMessage(sender, message);
    }
    
    // Classe interna para armazenar a conexão de um cliente
    @Getter
    private static class ClientConnection {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        
        public ClientConnection(Socket socket, BufferedReader reader, PrintWriter writer) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }
    }
    
    public void broadcastUserList() {
        log.debug("Broadcasting user list to WebSocket clients");
        String[] users = getConnectedUsernames();
        
        // Log mais detalhado para diagnóstico
        if (users.length > 0) {
            log.info("Broadcasting user list: {}", String.join(", ", users));
        } else {
            log.warn("Broadcasting empty user list");
        }
        
        webSocketService.notifyUserListUpdated(users);
    }

    // Melhorando o método getConnectedUsernames para garantir que não haja valores nulos
    public String[] getConnectedUsernames() {
        log.debug("Getting connected usernames. Count: {}", connectedClients.size());
        
        // Verificar se há usuários conectados e log detalhado
        if (connectedClients.isEmpty()) {
            // Não vamos mais adicionar usuários de teste
            log.warn("Nenhum cliente conectado");
            return new String[0];
        }
        
        String[] usernames = connectedClients.keySet().toArray(new String[0]);
        
        if (usernames.length > 0) {
            log.debug("Returning usernames: {}", String.join(", ", usernames));
        } else {
            log.warn("Connected clients map is not empty but no usernames were extracted");
        }
        
        return usernames;
    }

    // Adicionar método para registrar usuários de WebSocket
    public void registerWebSocketUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("Tentativa de registrar usuário WebSocket com nome vazio");
            return;
        }
        
        log.info("Registrando usuário WebSocket: {}", username);
        
        // Verificar se o usuário já existe
        if (connectedClients.containsKey(username)) {
            log.warn("Nome de usuário já em uso: {}", username);
            return;
        }
        
        // Para usuários WebSocket, criamos uma conexão especial sem socket
        ClientConnection webSocketConnection = new ClientConnection(null, null, null);
        connectedClients.put(username, webSocketConnection);
        
        // Atualizar lista de usuários sem enviar mensagem
        broadcastUserList();
        
        log.info("Usuário WebSocket registrado: {}. Total de usuários: {}", 
                 username, connectedClients.size());
    }

    // Método para remover usuário WebSocket
    public void removeWebSocketUser(String username) {
        if (username != null && connectedClients.containsKey(username)) {
            log.info("Removendo usuário WebSocket: {}", username);
            connectedClients.remove(username);
            broadcastUserList();
        }
    }
}
