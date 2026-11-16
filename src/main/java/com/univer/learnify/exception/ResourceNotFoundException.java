package com.univer.learnify.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super("Не найдено: " + entity + " с id=" + id);
    }
}
