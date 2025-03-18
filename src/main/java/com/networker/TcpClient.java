package com.networker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class TcpClient {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // IP do servidor
        int serverPort = 12345; // Porta do servidor

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("Hello, Server!\n".getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = reader.readLine();
            System.out.println("Resposta do server: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
