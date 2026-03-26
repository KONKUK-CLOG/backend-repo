package konkuk.clog.domain.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Extension 발행 완료 — 클라이언트는 blogUrl 만 노출하면 된다. */
@Getter
@AllArgsConstructor
public class BlogPublishResponse {

    private final Long blogId;
    private final String blogUrl;
}
