package com.networker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String sender;
    private String content;
    private String timestamp;
    private String type; // pode ser 'MESSAGE', 'JOIN', 'LEAVE', etc.
    private String recipient; // para mensagens privadas

    public WebSocketMessage(String sender, String content, String timestamp, String type) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }
}
