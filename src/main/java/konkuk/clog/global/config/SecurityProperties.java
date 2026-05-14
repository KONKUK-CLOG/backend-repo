package konkuk.clog.global.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 관련 설정 — CORS 허용 출처, 프록시 뒤 클라이언트 IP 신뢰, API 레이트 리밋.
 * <p>운영에서는 {@code APP_CORS_ALLOWED_ORIGINS} 를 실제 도메인 목록으로만 제한한다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** 쉼표로 구분된 허용 Origin (예: https://clog.example.com). 와일드카드 https://* 는 사용하지 않는다. */
    private String corsAllowedOrigins = "";

    /** 로드밸런서 뒤에서 X-Forwarded-For 를 신뢰할지 (내부망 LB 에서만 true 권장). */
    private boolean trustXForwardedFor = false;

    private int rateLimitGeneralPerMinute = 60;

    private int rateLimitAuthPerMinute = 20;

    /** Lambda/LLM 호출 — 비용 폭주 방지용 엄격한 한도 */
    private int rateLimitLlmPerMinute = 8;

    public List<String> parsedCorsOrigins() {
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
