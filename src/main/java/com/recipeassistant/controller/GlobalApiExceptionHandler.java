package com.recipeassistant.controller;

import com.recipeassistant.model.RecipeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.recipeassistant.controller")
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);
    private static final String GENERIC_ERROR = "An error occurred. Please try again later.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RecipeResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new RecipeResponse(false, null, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RecipeResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request.");
        return ResponseEntity.badRequest().body(new RecipeResponse(false, null, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RecipeResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid request body."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RecipeResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled API error", ex);
        return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
    }
}
