package com.victadore.webmafia.mafia_web_of_lies.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for sending messages to clients
        // Messages with destinations prefixed with /game will be routed to this broker
        config.enableSimpleBroker("/game");
        
        // Set prefix for messages from clients to application
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Register the "/mafia-websocket" endpoint, enabling SockJS fallback options
        registry.addEndpoint("/mafia-websocket")
                .setAllowedOrigins("*") // For development - restrict in production
                .withSockJS();
    }
}
