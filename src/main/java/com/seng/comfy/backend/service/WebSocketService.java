package com.seng.comfy.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketService {

    private final WebSocketClient webSocketClient;
    private WebSocketSession session;
    private CompletableFuture<String> messageFuture;
    @Autowired
    public WebSocketService(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    public void connect(String uri) throws ExecutionException, InterruptedException {
        messageFuture = new CompletableFuture<>();

        session = webSocketClient.doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                // Process the received message
                onMessageReceived(message.getPayload());

                messageFuture.complete(message.getPayload());
                messageFuture = new CompletableFuture<>();
            }
        }, uri).get();

        System.out.println("CONNECTED ");
    }

    public void sendMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        } else {
            System.out.println("WebSocket connection is not open");
        }

        System.out.println("Message Sent: " + message);
    }

    public void disconnect() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    public CompletableFuture<String> receiveMessage(int i, TimeUnit timeUnit) {
        return messageFuture;
    }
    private void onMessageReceived(String message) {
        // Your custom logic here
        // For example, you can parse the message, check its content,
        // and perform specific actions based on the message data
        System.out.println("Received message: " + message);
        // Add more logic as needed
    }
}
