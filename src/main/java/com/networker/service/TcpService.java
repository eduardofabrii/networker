package com.networker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

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
        ClientConnection connection = connectedClients.get(recipient);
        if (connection == null) {
            return false; // Destinatário não encontrado
        }
        
        try {
            PrintWriter out = connection.getWriter();
            out.println(String.format("DE %s: %s", sender, message));
            
            // Notifica via WebSocket
            webSocketService.notifyPrivateMessage(sender, recipient, message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Autenticação: primeira mensagem deve ser "LOGIN:username"
            String loginMessage = reader.readLine();
            if (loginMessage == null || !loginMessage.startsWith("LOGIN:")) {
                writer.println("ERRO: Login necessário. Formato: LOGIN:username");
                clientSocket.close();
                return;
            }
            
            String username = loginMessage.substring(6).trim();
            if (connectedClients.containsKey(username)) {
                writer.println("ERRO: Nome de usuário já em uso");
                clientSocket.close();
                return;
            }
            
            // Armazena a conexão do cliente
            ClientConnection connection = new ClientConnection(clientSocket, reader, writer);
            connectedClients.put(username, connection);
            
            writer.println("SUCESSO: Logado como " + username);
            webSocketService.notifyTcpConnection(username + " (" + clientSocket.getInetAddress() + ")");
            
            // Lista de usuários online
            StringBuilder userList = new StringBuilder("USUARIOS_ONLINE:");
            connectedClients.keySet().forEach(user -> userList.append(" ").append(user));
            writer.println(userList.toString());
            
            // Enviar mensagem para todos sobre o novo usuário
            broadcastMessage("SISTEMA", username + " entrou no chat");
            
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
            
            // Quando o cliente sai, removê-lo da lista e notificar os outros
            connectedClients.remove(username);
            broadcastMessage("SISTEMA", username + " saiu do chat");
            
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
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
    
    public String[] getConnectedUsernames() {
        return connectedClients.keySet().toArray(new String[0]);
    }
}
