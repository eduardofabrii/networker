package com.networker.controller;

import com.networker.model.ApiResponse;
import com.networker.model.MessageResponse;
import com.networker.model.ServerStatusResponse;
import com.networker.service.TcpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tcp")
public class TcpController {

    @Autowired
    private TcpService tcpService;
    
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestParam String serverAddress,
            @RequestParam int port,
            @RequestParam String message) {
        
        String serverResponse = tcpService.sendMessageToServer(serverAddress, port, message);
        MessageResponse messageResponse = new MessageResponse(message, serverResponse);
        
        return ResponseEntity.ok(messageResponse);
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
