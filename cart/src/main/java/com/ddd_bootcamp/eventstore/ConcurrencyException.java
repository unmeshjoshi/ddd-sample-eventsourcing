package com.ddd_bootcamp.eventstore;

public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException() {
        super("Concurrency conflict: the aggregate has been modified by another process");
    }
} 