package com.networker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TcpMessage {
    private String serverAddress;
    private int port;
    private String message;
    private String response;
}
