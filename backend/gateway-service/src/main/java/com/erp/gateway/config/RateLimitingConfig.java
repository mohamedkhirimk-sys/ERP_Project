package com.erp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class RateLimitingConfig {

    static class InMemoryRateLimiter implements RateLimiter<Object> {
        private final Map<String, Window> windows = new ConcurrentHashMap<>();
        private final long maxRequests;
        private final long windowMillis;

        InMemoryRateLimiter(long maxRequests, Duration windowDuration) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowDuration.toMillis();
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            long now = System.currentTimeMillis();
            Window window = windows.computeIfAbsent(id, k -> new Window(now));

            synchronized (window) {
                if (now - window.start > windowMillis) {
                    window.start = now;
                    window.count.set(0);
                }
                long count = window.count.incrementAndGet();
                boolean allowed = count <= maxRequests;
                long remaining = Math.max(0, maxRequests - count);
                return Mono.just(new Response(allowed, Map.of(
                        "remaining", String.valueOf(remaining),
                        "limit", String.valueOf(maxRequests)
                )));
            }
        }

        @Override
        public Map<String, Object> getConfig() {
            return Collections.emptyMap();
        }

        @Override
        public Class<Object> getConfigClass() {
            return Object.class;
        }

        @Override
        public Object newConfig() {
            return new Object();
        }

        private static class Window {
            long start;
            AtomicLong count = new AtomicLong(0);

            Window(long start) {
                this.start = start;
            }
        }
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        var config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Bean
    public RateLimiter<Object> inMemoryRateLimiter() {
        return new InMemoryRateLimiter(100, Duration.ofMinutes(1));
    }

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder, RateLimiter<Object> rateLimiter, KeyResolver ipKeyResolver) {
        return builder.routes()
                .route("identity-auth-route", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://identity-service"))
                .route("identity-route", r -> r
                        .path("/api/identity/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://identity-service"))
                .route("inventory-route", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://inventory-service"))
                .route("product-route", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://product-service"))
                .route("payment-route", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://payment-service"))
                .route("order-route", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://order-service"))
                .route("sales-customers-route", r -> r
                        .path("/api/customers/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://sales-service"))
                .route("sales-invoices-route", r -> r
                        .path("/api/invoices/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://sales-service"))
                .route("hrm-employees-route", r -> r
                        .path("/api/employees/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://hrm-service"))
                .route("hrm-attendance-route", r -> r
                        .path("/api/attendance/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://hrm-service"))
                .route("hrm-leaves-route", r -> r
                        .path("/api/leaves/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://hrm-service"))
                .route("finance-accounts-route", r -> r
                        .path("/api/accounts/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://finance-service"))
                .route("finance-journal-route", r -> r
                        .path("/api/journal-entries/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://finance-service"))
                .route("finance-payroll-route", r -> r
                        .path("/api/payroll/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://finance-service"))
                .route("procurement-vendors-route", r -> r
                        .path("/api/vendors/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://procurement-service"))
                .route("procurement-purchase-orders-route", r -> r
                        .path("/api/purchase-orders/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://procurement-service"))
                .route("procurement-goods-received-route", r -> r
                        .path("/api/goods-received/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://procurement-service"))
                .route("reporting-route", r -> r
                        .path("/api/reports/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri("lb://reporting-service"))
                .build();
    }
}
