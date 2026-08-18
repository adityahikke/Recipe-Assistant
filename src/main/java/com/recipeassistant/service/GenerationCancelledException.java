package com.recipeassistant.service;

public class GenerationCancelledException extends RuntimeException {

    public GenerationCancelledException() {
        super("Generation cancelled");
    }
}
