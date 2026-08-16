package com.example.taskmanager.exception;

public class InvalidTaskDataException extends RuntimeException {

    public InvalidTaskDataException(String message) {
        super(message);
    }
}
