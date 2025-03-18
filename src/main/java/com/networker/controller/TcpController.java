package com.networker.controller;

import com.networker.model.ApiResponse;
import com.networker.model.MessageResponse;
import com.networker.model.ServerStatusResponse;
import com.networker.service.TcpService;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tcp")
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
            
        boolean sent = tcpService.sendMessageToUser(sender, recipient, message);
        
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("Message sent successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Failed to send message. User might be offline."));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<String>> getConnectedUsers() {
        String[] users = tcpService.getConnectedUsernames();
        return ResponseEntity.ok(Arrays.asList(users));
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
}
