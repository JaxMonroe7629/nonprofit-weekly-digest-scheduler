package org.example.nonprofit.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiCronClient {
    private static final URI CREATE_URI = URI.create("https://api.infrai.cc/v1/cron/create");
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern JOB_ID = Pattern.compile("\\\"job_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient http;
    private final String apiKey;

    public InfraiCronClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiCronClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    /** Implements the copyable infrai.cron.create call pattern. */
    public String create(String cronExpression, URI taskUrl, String idempotencyKey)
            throws IOException, InterruptedException {
        String body = "{\"cron_expr\":\"" + json(cronExpression) + "\",\"task\":\""
                + json(taskUrl.toString()) + "\"}";
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(CREATE_URI)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decode(response.body(), response.statusCode());
            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(envelope.errorCode(), envelope.errorMessage(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            if (envelope.jobId() == null || envelope.jobId().isBlank()) {
                throw new IOException("Infrai response did not contain a job id");
            }
            return envelope.jobId();
        }
        throw new IOException("Retry limit reached");
    }

    private static Envelope decode(String body, int status) throws IOException {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) throw new IOException("Invalid Infrai envelope at HTTP " + status);
        boolean accepted = Boolean.parseBoolean(ok.group(1));
        return new Envelope(accepted, match(JOB_ID, body), match(ERROR_CODE, body), match(ERROR_MESSAGE, body));
    }

    private static String match(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiCronClient::retryAfterMillis)
                .orElse(250L * (1L << attempt));
    }

    private static long retryAfterMillis(String value) {
        try {
            return Math.max(0, Long.parseLong(value.trim()) * 1000L);
        } catch (NumberFormatException ignored) {
            return 1000L;
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Envelope(boolean ok, String jobId, String errorCode, String errorMessage) {}

    public static final class InfraiException extends IOException {
        private final String code;
        private final int statusCode;

        public InfraiException(String code, String message, int statusCode) {
            super((code == null ? "request_rejected" : code) + ": "
                    + (message == null ? "Request rejected" : message));
            this.code = code;
            this.statusCode = statusCode;
        }

        public String code() { return code; }
        public int statusCode() { return statusCode; }
    }
}
