package com.example.serviceApp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
