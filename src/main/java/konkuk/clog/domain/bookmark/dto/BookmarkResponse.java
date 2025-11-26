package konkuk.clog.domain.bookmark.dto;

import java.time.LocalDateTime;
import konkuk.clog.domain.blog.dto.BlogSummaryResponse;
import konkuk.clog.domain.bookmark.domain.Bookmark;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkResponse {

    private Long id;
    private BlogSummaryResponse blog;
    private LocalDateTime createdAt;

    public static BookmarkResponse from(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .blog(BlogSummaryResponse.from(bookmark.getBlog()))
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}



