package com.recipeassistant.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GenerationCancellationService {

    private final Map<String, AtomicBoolean> cancelled = new ConcurrentHashMap<>();

    public void register(String clientId) {
        cancelled.put(clientId, new AtomicBoolean(false));
    }

    public void requestCancel(String clientId) {
        AtomicBoolean flag = cancelled.get(clientId);
        if (flag != null) {
            flag.set(true);
        }
    }

    public void throwIfCancelled(String clientId) {
        AtomicBoolean flag = cancelled.get(clientId);
        if (flag != null && flag.get()) {
            throw new GenerationCancelledException();
        }
    }

    public void clear(String clientId) {
        cancelled.remove(clientId);
    }
}
