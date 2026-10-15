package com.example.serviceApp.Service;

import com.example.serviceApp.Entity.CustomUserDetails;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public StompAuthenticationInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException("WebSocket authentication token is required");
            }

            try {
                String token = authorization.substring(7);
                String email = jwtService.parseToken(token).getSubject();
                CustomUserDetails user =
                        (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                ));
            } catch (JwtException exception) {
                throw new IllegalArgumentException("Invalid WebSocket authentication token");
            }
        }

        return message;
    }
}
