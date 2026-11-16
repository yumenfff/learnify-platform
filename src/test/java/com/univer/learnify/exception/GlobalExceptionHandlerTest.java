package com.univer.learnify.exception;

import com.univer.learnify.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_returns404WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ResourceNotFoundException("Курс", 1L));

        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("id=1"));
    }

    @Test
    void conflict_returns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleConflict(new ConflictException("Дубликат"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Дубликат", response.getBody().message());
    }
}
