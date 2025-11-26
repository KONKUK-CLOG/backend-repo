package konkuk.clog.domain.blog.dto;

import java.time.LocalDateTime;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.domain.BlogStatus;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlogSummaryResponse {

    private Long id;
    private String title;
    private BlogStatus status;
    private BlogVisibility visibility;
    private long viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static BlogSummaryResponse from(Blog blog) {
        return BlogSummaryResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .status(blog.getStatus())
                .visibility(blog.getVisibility())
                .viewCount(blog.getViewCount())
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .build();
    }
}



