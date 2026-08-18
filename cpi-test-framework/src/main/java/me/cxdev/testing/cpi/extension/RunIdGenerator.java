package me.cxdev.testing.cpi.extension;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class RunIdGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC);

    private RunIdGenerator() {
    }

    public static String generate() {
        return "RUN-" + FORMATTER.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
