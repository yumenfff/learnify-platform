package com.univer.learnify.dto.request;

import jakarta.validation.constraints.Size;


public record UpdateUserRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 2000) String bio,
        @Size(max = 500) String avatarUrl
) {
}
