package com.gts.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 인증 없이 접근 가능한 경로 (메서드 무관)
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    // GET 요청만 공개
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/v1/contents/**",
            "/api/v1/tags/**",
            "/api/v1/rss-sources",
            "/api/v1/rss/**"
    );

    // 관리자 전용 경로
    private static final List<String> ADMIN_PATHS = List.of(
            "/api/v1/collector/**",
            "/api/v1/access-logs/**",
            "/api/v1/summarize/**"
    );

    // 관리자 전용 메서드 + 경로 (해당 경로의 쓰기 작업)
    private static final List<String> ADMIN_WRITE_PATHS = List.of(
            "/api/v1/rss-sources/**"
    );

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 완전 공개 경로
        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        // 토큰 추출
        String token = extractToken(request);
        if (token == null) {
            return unauthorized(exchange.getResponse(), "인증이 필요합니다.");
        }

        // 토큰 검증
        Claims claims;
        try {
            claims = parseClaims(token);
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange.getResponse(), "만료된 토큰입니다.");
        } catch (JwtException e) {
            return unauthorized(exchange.getResponse(), "유효하지 않은 토큰입니다.");
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        // 관리자 전용 경로 검증
        if (isAdminRequired(path, method)) {
            if (!"ADMIN".equals(role)) {
                return forbidden(exchange.getResponse());
            }
        }

        // 하위 서비스로 사용자 정보 전달
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublic(String path, HttpMethod method) {
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, path)) return true;
        }
        if (HttpMethod.GET.equals(method)) {
            for (String pattern : PUBLIC_GET_PATHS) {
                if (pathMatcher.match(pattern, path)) return true;
            }
        }
        return false;
    }

    private boolean isAdminRequired(String path, HttpMethod method) {
        for (String pattern : ADMIN_PATHS) {
            if (pathMatcher.match(pattern, path)) return true;
        }
        if (!HttpMethod.GET.equals(method)) {
            for (String pattern : ADMIN_WRITE_PATHS) {
                if (pathMatcher.match(pattern, path)) return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        log.warn("[Gateway] 인증 실패: {}", message);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> forbidden(ServerHttpResponse response) {
        log.warn("[Gateway] 권한 없음: ADMIN 권한 필요");
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
