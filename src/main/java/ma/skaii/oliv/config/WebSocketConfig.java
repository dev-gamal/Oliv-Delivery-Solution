package ma.skaii.oliv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Canal de diffusion serveur -> clients (Expéditeur, Destinataire, Dispatch)
        registry.enableSimpleBroker("/topic");
        // Préfixe pour les messages envoyés par les clients vers le serveur
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173", // React Vite
                        "http://localhost:3000"
                )
                .withSockJS(); // Fallback automatique si le réseau mobile bloque WebSocket pur
    }
}