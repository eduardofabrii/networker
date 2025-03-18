package com.networker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TcpClient {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // IP do servidor
        int serverPort = 12345; // Porta do servidor
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine().trim();

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            System.out.println("Conectado ao servidor em " + serverAddress + ":" + serverPort);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Login
            writer.println("LOGIN:" + username);
            
            // Thread para receber mensagens do servidor
            new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (Exception e) {
                    System.out.println("Conexão com o servidor perdida: " + e.getMessage());
                }
            }).start();
            
            // Instruções
            System.out.println("Comandos disponíveis:");
            System.out.println("MSG:destinatario:mensagem - Enviar mensagem privada");
            System.out.println("BROADCAST:mensagem - Enviar mensagem para todos");
            System.out.println("LISTAR_USUARIOS - Obter lista de usuários online");
            System.out.println("SAIR - Desconectar do servidor");
            
            // Loop para enviar mensagens
            String input;
            while (!(input = scanner.nextLine()).equals("SAIR")) {
                writer.println(input);
            }
            
            // Logout
            writer.println("SAIR");
            
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
