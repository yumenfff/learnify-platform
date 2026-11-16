package com.univer.learnify.dto.request;

import com.univer.learnify.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Role role,
        @Size(max = 2000) String bio,
        @Size(max = 500) String avatarUrl
) {
}
