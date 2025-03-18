package com.networker.service;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

@Service
@Getter
@Setter
public class TcpService {
    
    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private int serverPort = 12345;
    
    public String sendMessageToServer(String serverAddress, int port, String message) {
        try (Socket socket = new Socket(serverAddress, port)) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write((message + "\n").getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    public void startServer() {
        if (isServerRunning) {
            return;
        }
        
        isServerRunning = true;
        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                System.out.println("Server TCP está rodando na porta: " + serverPort);
                
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
            OutputStream outputStream = clientSocket.getOutputStream();

            String receivedMessage = reader.readLine();
            System.out.println("Recebido: " + receivedMessage);

            String response = "Mensagem recebida: " + receivedMessage + "\n";
            outputStream.write(response.getBytes());

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
