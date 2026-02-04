package com.baghajanyan.sandbox.core.fs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DeleteConfigTest {
    @Test
    void constructor_edgeCases() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(0, Duration.ofMillis(10), Duration.ofMillis(100))),

                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, Duration.ofMillis(0), Duration.ofMillis(100))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, Duration.ofMillis(-1), Duration.ofMillis(100))),

                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, Duration.ofMillis(10), Duration.ofMillis(0))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, Duration.ofMillis(10), Duration.ofMillis(-1))),

                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, null, Duration.ofMillis(100))),

                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DeleteConfig(3, Duration.ofMillis(10), null)));
    }
}
