package com.networker.service;

import com.networker.model.WebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendMessage(WebSocketMessage message) {
        if (message.getTimestamp() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            message.setTimestamp(sdf.format(new Date()));
        }
        messagingTemplate.convertAndSend("/topic/messages", message);
    }

    public void notifyTcpConnection(String clientAddress) {
        String message;
        if (clientAddress.contains("WebSocket")) {
            message = "Novo cliente conectado através do WebSocket: " + 
                clientAddress.replace(" (WebSocket)", "");
        } else {
            message = "Novo cliente conectado: " + clientAddress;
        }
        
        WebSocketMessage webSocketMessage = new WebSocketMessage(
                "System", 
                message,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "NOTIFICATION"
        );
        messagingTemplate.convertAndSend("/topic/notifications", webSocketMessage);
    }

    public void notifyTcpMessage(String clientAddress, String receivedMessage) {
        WebSocketMessage message = new WebSocketMessage(
                clientAddress,
                receivedMessage,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "TCP_MESSAGE"
        );
        messagingTemplate.convertAndSend("/topic/tcp-messages", message);
    }
    
    public void notifyPrivateMessage(String sender, String recipient, String content) {
        WebSocketMessage message = new WebSocketMessage(
                sender,
                content,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "PRIVATE_MESSAGE"
        );
        message.setRecipient(recipient);
        messagingTemplate.convertAndSend("/topic/private-messages", message);
    }
    
    public void notifyBroadcastMessage(String sender, String content) {
        WebSocketMessage message = new WebSocketMessage(
                sender,
                content,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "BROADCAST"
        );
        messagingTemplate.convertAndSend("/topic/broadcast-messages", message);
    }

    public void notifyUserListUpdated(String[] users) {
        WebSocketMessage message = new WebSocketMessage(
                "System",
                "Users updated",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "USER_LIST"
        );
        
        // Converte a lista de usuários para uma string
        StringBuilder userList = new StringBuilder();
        for (String user : users) {
            if (userList.length() > 0) userList.append(", ");
            userList.append(user);
        }
        
        message.setContent("Connected users: " + userList.toString());
        messagingTemplate.convertAndSend("/topic/users", users);
    }
}
