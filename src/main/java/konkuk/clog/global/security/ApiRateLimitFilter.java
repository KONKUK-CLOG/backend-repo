package konkuk.clog.global.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import konkuk.clog.global.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP(또는 신뢰 가능한 X-Forwarded-For) 기준 분당 요청 수 제한.
 * <p>Lambda/채팅 경로는 별도 낮은 한도로 AWS 비용·DoS 남용을 완화한다.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> llmBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        LimitKind kind = classifyPath(request.getRequestURI());
        Bucket bucket = bucketFor(clientKey, kind);

        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private enum LimitKind {
        LLM,
        AUTH,
        GENERAL
    }

    private LimitKind classifyPath(String uri) {
        if (uri.startsWith("/api/chat/send") || uri.startsWith("/api/blogs/generate")) {
            return LimitKind.LLM;
        }
        if (uri.startsWith("/api/auth")) {
            return LimitKind.AUTH;
        }
        return LimitKind.GENERAL;
    }

    private Bucket bucketFor(String clientKey, LimitKind kind) {
        String mapKey = clientKey + ":" + kind.name();
        return switch (kind) {
            case LLM -> llmBuckets.computeIfAbsent(mapKey, k -> newBucket(securityProperties.getRateLimitLlmPerMinute()));
            case AUTH -> authBuckets.computeIfAbsent(mapKey, k -> newBucket(securityProperties.getRateLimitAuthPerMinute()));
            case GENERAL -> generalBuckets.computeIfAbsent(mapKey, k -> newBucket(securityProperties.getRateLimitGeneralPerMinute()));
        };
    }

    private static Bucket newBucket(int perMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (securityProperties.isTrustXForwardedFor()) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) {
                return xf.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
