package com.univer.learnify.dto.response;

import com.univer.learnify.entity.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        String bio,
        String avatarUrl
) {
}
