package com.networker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse extends ApiResponse {
    private String message;  // A mensagem que você enviou
    private String serverResponse;  // A resposta do servidor
}
