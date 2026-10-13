package com.infrasight.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.springframework.stereotype.Component;

import com.infrasight.dto.CheckOutcome;
import com.infrasight.entity.MonitoredEndpoint;

/**
 * Performs one real HTTP/HTTPS GET. Redirects are followed manually (at most 5) so that every
 * hop is validated. The response body is never read; time is measured until the response headers arrive.
 */
@Component
public class HttpChecker {

    private static final int MAX_REDIRECTS = 5;

    private final UrlValidator urlValidator;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public HttpChecker(UrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    public CheckOutcome check(MonitoredEndpoint endpoint) {
        int timeoutSeconds = endpoint.getTimeoutSeconds();
        long start = System.nanoTime();
        long deadline = start + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        Integer expected = endpoint.getExpectedStatus();
        boolean followRedirects = expected == null || expected < 300 || expected >= 400;
        Integer lastStatus = null;

        try {
            URI uri = urlValidator.validate(endpoint.getUrl());
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return failure(lastStatus, null, "Timed out after " + timeoutSeconds + " s");
                }
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .GET()
                        .timeout(Duration.ofNanos(remaining))
                        .header("User-Agent", "InfraSight/1.0")
                        .header("Accept", "*/*")
                        .build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                try (InputStream ignored = response.body()) {
                    // Closing the stream without reading it discards the body.
                }
                int code = response.statusCode();
                lastStatus = code;

                if (followRedirects && isRedirect(code)) {
                    Optional<String> location = response.headers().firstValue("Location");
                    if (location.isPresent()) {
                        if (hop == MAX_REDIRECTS) {
                            return failure(code, elapsedMs, "Too many redirects");
                        }
                        uri = urlValidator.validate(uri.resolve(location.get()).toString());
                        continue;
                    }
                }

                boolean ok = expected != null ? code == expected : (code >= 200 && code < 400);
                if (ok) {
                    return new CheckOutcome(true, code, elapsedMs, null);
                }
                String reason = expected != null
                        ? "Expected HTTP " + expected + " but received " + code
                        : "Unexpected HTTP status " + code;
                return failure(code, elapsedMs, reason);
            }
            return failure(lastStatus, null, "Too many redirects");
        } catch (UnsafeUrlException ex) {
            return failure(lastStatus, null, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(lastStatus, null, "The check was interrupted");
        } catch (IOException ex) {
            return failure(lastStatus, null, describe(ex, timeoutSeconds));
        } catch (RuntimeException ex) {
            return failure(lastStatus, null, "The request could not be sent (" + ex.getClass().getSimpleName() + ")");
        }
    }

    private static boolean isRedirect(int code) {
        return code == 301 || code == 302 || code == 303 || code == 307 || code == 308;
    }

    private static CheckOutcome failure(Integer status, Long elapsedMs, String message) {
        return new CheckOutcome(false, status, elapsedMs, message);
    }

    private static String describe(Throwable ex, int timeoutSeconds) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof HttpTimeoutException) {
                return "Timed out after " + timeoutSeconds + " s";
            }
            if (t instanceof UnknownHostException) {
                return "Host name could not be resolved";
            }
            if (t instanceof SSLException) {
                return "TLS/SSL error: the connection could not be secured";
            }
            if (t instanceof ConnectException) {
                return "Connection refused or host unreachable";
            }
        }
        return "Connection failed (" + ex.getClass().getSimpleName() + ")";
    }
}
