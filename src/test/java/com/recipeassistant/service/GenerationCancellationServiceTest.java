package com.recipeassistant.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerationCancellationServiceTest {

    @Test
    void requestCancel_setsFlagForActiveGeneration() {
        GenerationCancellationService service = new GenerationCancellationService();
        String clientId = "test-client";
        service.register(clientId);
        assertDoesNotThrow(() -> service.throwIfCancelled(clientId));
        service.requestCancel(clientId);
        assertThrows(GenerationCancelledException.class, () -> service.throwIfCancelled(clientId));
        service.clear(clientId);
        assertDoesNotThrow(() -> service.requestCancel(clientId));
    }
}
