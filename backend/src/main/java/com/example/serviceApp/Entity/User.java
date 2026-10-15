package com.example.serviceApp.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role = Role.USER;

    private LocalDateTime createdAt = LocalDateTime.now();
}
