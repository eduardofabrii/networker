package com.networker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean success = true; // Default value set to true
    private String message;
    
    // Constructor para erros
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
    
    // Construtor para sucesso
    public static ApiResponse success(String message) {
        return new ApiResponse(true, message);
    }
}
