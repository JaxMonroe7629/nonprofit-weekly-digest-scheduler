package org.example.nonprofit;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.example.nonprofit.client.InfraiCronClient;
import org.example.nonprofit.config.DigestConfig;

public final class DigestScheduler {
    private DigestScheduler() {}

    public static void main(String[] args) throws Exception {
        DigestConfig config = DigestConfig.fromEnvironment();
        InfraiCronClient cron = new InfraiCronClient(config.apiKey());
        String stableInput = config.cronExpression() + "|" + config.taskUrl();
        String idempotencyKey = "nonprofit-weekly-digest-"
                + UUID.nameUUIDFromBytes(stableInput.getBytes(StandardCharsets.UTF_8));
        String jobId = cron.create(config.cronExpression(), config.taskUrl(), idempotencyKey);
        System.out.println("Scheduled nonprofit weekly digest: " + jobId);
    }
}
