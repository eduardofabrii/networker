package com.networker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServerStatusResponse {
    private boolean running;
    private int port;
    private String message;
    
    public ServerStatusResponse(boolean running, int port) {
        this.running = running;
        this.port = port;
    }
}
