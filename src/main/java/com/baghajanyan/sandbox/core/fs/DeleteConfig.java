package com.baghajanyan.sandbox.core.fs;

import java.time.Duration;

public record DeleteConfig(int maxRetries, Duration retryDelay, Duration terminationTimeout) {
    public DeleteConfig {
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("retryDelay must be positive");
        }
        if (terminationTimeout == null || terminationTimeout.isNegative() || terminationTimeout.isZero()) {
            throw new IllegalArgumentException("terminationTimeout must be positive");
        }
    }
}
