package com.daffiqtrie.gateway.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-101)
public class UserActivityLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLoggingFilter.class);
    private static final List<String> MDC_FIELDS = List.of(
            "event_type", "activity", "request_id", "http_method", "request_path",
            "route_target", "status_code", "outcome", "principal", "exception_type");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/.well-known")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-ID", requestId);

        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            int statusCode = response.getStatus();
            if (failure != null && statusCode < 400) {
                statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            putActivityFields(request, requestId, statusCode, failure);
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put("duration_ms", Long.toString(durationMs));
            if (failure == null && statusCode < 400) {
                log.info("User activity request completed");
            } else {
                log.info("User activity request failed");
            }
            clearActivityFields();
        }
    }

    private void putActivityFields(HttpServletRequest request, String requestId, int statusCode,
            Throwable failure) {
        MDC.put("event_type", "user_activity");
        MDC.put("activity", "http_request");
        MDC.put("request_id", requestId);
        MDC.put("http_method", request.getMethod());
        MDC.put("request_path", request.getRequestURI());
        MDC.put("route_target", routeTarget(request.getRequestURI()));
        MDC.put("status_code", Integer.toString(statusCode));
        MDC.put("outcome", statusCode < 400 ? "success" : "failure");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            MDC.put("principal", authentication.getName());
        }
        if (failure != null) {
            MDC.put("exception_type", failure.getClass().getSimpleName());
        }
    }

    private String routeTarget(String path) {
        if (path.startsWith("/auth/")) {
            return "AUTH-SERVICE";
        }
        if (path.startsWith("/api/produk")) {
            return "PRODUK";
        }
        if (path.startsWith("/api/order")) {
            return "ORDER";
        }
        if (path.startsWith("/api/pelanggan")) {
            return "PELANGGAN";
        }
        if (path.equals("/send")) {
            return "PRODUSER";
        }
        return "UNKNOWN";
    }

    private void clearActivityFields() {
        MDC_FIELDS.forEach(MDC::remove);
        MDC.remove("duration_ms");
    }
}
