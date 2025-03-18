package com.networker.controller;

import com.networker.model.ApiResponse;
import com.networker.model.MessageResponse;
import com.networker.model.ServerStatusResponse;
import com.networker.service.TcpService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tcp")
@Slf4j
public class TcpController {

    private final TcpService tcpService;
    
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestParam String serverAddress,
            @RequestParam int port,
            @RequestParam String message) {
        
        String serverResponse = tcpService.sendMessageToServer(serverAddress, port, message);
        MessageResponse messageResponse = new MessageResponse(message, serverResponse);
        
        return ResponseEntity.ok(messageResponse);
    }
    
    @PostMapping("/message")
    public ResponseEntity<ApiResponse> sendMessageToUser(
            @RequestParam String sender, 
            @RequestParam String recipient, 
            @RequestParam String message) {
            
        log.debug("Sending message: from {} to {} - content: {}", sender, recipient, message);
        boolean sent = tcpService.sendMessageToUser(sender, recipient, message);
        
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("Message sent successfully"));
        } else {
            log.warn("Failed to send message from {} to {}", sender, recipient);
            return ResponseEntity.ok(ApiResponse.error("Failed to send message. User might be offline."));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<String>> getConnectedUsers() {
        try {
            String[] users = tcpService.getConnectedUsernames();
            log.debug("Returning connected users: {}", Arrays.toString(users));
            
            // Forçar atualização na lista de usuários via WebSocket
            tcpService.broadcastUserList();
            
            if (users.length == 0) {
                log.warn("No connected users found");
            }
            
            return ResponseEntity.ok(Arrays.asList(users));
        } catch (Exception e) {
            log.error("Error retrieving connected users", e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error for better UI handling
        }
    }
    
    @GetMapping("/debug/users")
    public ResponseEntity<Map<String, Object>> getDebugUserInfo() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            String[] users = tcpService.getConnectedUsernames();
            debug.put("users", users);
            debug.put("count", users.length);
            debug.put("serverRunning", tcpService.isServerRunning());
            debug.put("port", tcpService.getServerPort());
            debug.put("timestamp", new Date().toString());
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            return ResponseEntity.ok(debug);
        }
    }
    
    @GetMapping("/users/{username}/status")
    public ResponseEntity<ApiResponse> checkUserStatus(@PathVariable String username) {
        boolean isConnected = tcpService.isUserConnected(username);
        
        if (isConnected) {
            return ResponseEntity.ok(ApiResponse.success("User is online"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("User is offline"));
        }
    }
    
    @PostMapping("/server/start")
    public ResponseEntity<ServerStatusResponse> startServer(@RequestParam(required = false) Integer port) {
        if (port != null) {
            tcpService.setServerPort(port);
        }
        
        tcpService.startServer();
        
        ServerStatusResponse response = new ServerStatusResponse(true, tcpService.getServerPort());
        response.setMessage("Server started successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/server/stop")
    public ResponseEntity<ApiResponse> stopServer() {
        tcpService.stopServer();
        
        ApiResponse response = new ApiResponse(true, "Server stopped successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/server/status")
    public ResponseEntity<ServerStatusResponse> getServerStatus() {
        ServerStatusResponse status = new ServerStatusResponse(
            tcpService.isServerRunning(),
            tcpService.getServerPort()
        );
        
        return ResponseEntity.ok(status);
    }

    // Classe para receber o nome de usuário em requisições POST
    static class UsernameRequest {
        private String username;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }

    @PostMapping("/users/register")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody UsernameRequest request) {
        String username = request.getUsername();
        log.info("Registrando usuário WebSocket: {}", username);
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Nome de usuário não pode ser vazio"));
        }
        
        if (tcpService.isUserConnected(username)) {
            return ResponseEntity.ok(ApiResponse.error("Nome de usuário já está em uso"));
        }
        
        tcpService.registerWebSocketUser(username);
        
        return ResponseEntity.ok(ApiResponse.success("Usuário registrado com sucesso"));
    }

    @PostMapping("/users/unregister")
    public ResponseEntity<ApiResponse> unregisterUser(@RequestBody UsernameRequest request) {
        String username = request.getUsername();
        log.info("Removendo usuário WebSocket: {}", username);
        
        tcpService.removeWebSocketUser(username);
        
        return ResponseEntity.ok(ApiResponse.success("Usuário removido com sucesso"));
    }
}
