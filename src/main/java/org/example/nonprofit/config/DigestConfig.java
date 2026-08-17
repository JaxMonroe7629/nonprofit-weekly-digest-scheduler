package org.example.nonprofit.config;

import java.net.URI;

public record DigestConfig(String apiKey, URI taskUrl, String cronExpression) {
    public static DigestConfig fromEnvironment() {
        String apiKey = required("INFRAI_API_KEY");
        URI taskUrl = URI.create(required("DIGEST_TASK_URL"));
        if (!"https".equalsIgnoreCase(taskUrl.getScheme())) {
            throw new IllegalArgumentException("DIGEST_TASK_URL must use HTTPS");
        }
        return new DigestConfig(apiKey, taskUrl, "0 9 * * 1");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
