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
        WebSocketMessage message = new WebSocketMessage(
                "System", 
                "New TCP client connected: " + clientAddress,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "NOTIFICATION"
        );
        messagingTemplate.convertAndSend("/topic/notifications", message);
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
}
