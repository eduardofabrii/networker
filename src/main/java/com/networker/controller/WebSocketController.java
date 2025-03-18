package com.networker.controller;

import com.networker.model.WebSocketMessage;
import com.networker.service.WebSocketService;
import com.networker.service.TcpService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
@Controller
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final TcpService tcpService;

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public WebSocketMessage sendMessage(WebSocketMessage message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        message.setTimestamp(sdf.format(new Date()));
        return message;
    }
    
    @MessageMapping("/ping")
    public void handlePing(WebSocketMessage message) {
        log.info("Received ping from {}", message.getSender());
        
        // Envia um pong de volta
        WebSocketMessage pong = new WebSocketMessage(
            "System",
            "Pong - Connection active for " + message.getSender(),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
            "NOTIFICATION"
        );
        webSocketService.sendMessage(pong);
        
        // Também exibe a lista de usuários conectados
        String[] users = tcpService.getConnectedUsernames();
        StringBuilder userList = new StringBuilder("Connected users: ");
        for (String user : users) {
            userList.append(user).append(", ");
        }
        log.info(userList.toString());
    }
    
    @MessageMapping("/connect")
    public void handleConnect(WebSocketMessage message) {
        log.info("Usuário conectado via WebSocket: {}", message.getSender());
        
        // Registra o usuário no serviço TCP
        tcpService.registerWebSocketUser(message.getSender());
        
        // Removido: Não notificamos mais quando alguém se conecta
        // webSocketService.notifyTcpConnection(message.getSender() + " (WebSocket)");
    }
    
    @MessageMapping("/private/{recipient}")
    public void sendPrivateMessage(@DestinationVariable String recipient, WebSocketMessage message) {
        log.debug("Handling private message from {} to {}: {}", message.getSender(), recipient, message.getContent());
        
        // Coloca a data atual se não estiver definida
        if (message.getTimestamp() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            message.setTimestamp(sdf.format(new Date()));
        }
        
        message.setType("PRIVATE_MESSAGE");
        message.setRecipient(recipient);
        
        // Tambem envia via TCP
        tcpService.sendMessageToUser(message.getSender(), recipient, message.getContent());
        
        // Tambem envia via WebSocket
        webSocketService.notifyPrivateMessage(
            message.getSender(), 
            recipient, 
            message.getContent()
        );
    }
}
