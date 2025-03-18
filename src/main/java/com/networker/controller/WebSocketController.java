package com.networker.controller;

import com.networker.model.WebSocketMessage;
import com.networker.service.WebSocketService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
@Controller
public class WebSocketController {

    private final WebSocketService webSocketService;

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public WebSocketMessage sendMessage(WebSocketMessage message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        message.setTimestamp(sdf.format(new Date()));
        return message;
    }
}
