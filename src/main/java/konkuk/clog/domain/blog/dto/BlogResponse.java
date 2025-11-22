package konkuk.clog.domain.blog.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.domain.BlogStatus;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import konkuk.clog.domain.comment.dto.CommentResponse;
import konkuk.clog.domain.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlogResponse {

    private Long id;
    private UserResponse author;
    private String title;
    private String content;
    private BlogStatus status;
    private BlogVisibility visibility;
    private long viewCount;
    private String ogTitle;
    private String ogBlogUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;

    public static BlogResponse from(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .author(UserResponse.from(blog.getAuthor()))
                .title(blog.getTitle())
                .content(blog.getContent())
                .status(blog.getStatus())
                .visibility(blog.getVisibility())
                .viewCount(blog.getViewCount())
                .ogTitle(blog.getOgTitle())
                .ogBlogUrl(blog.getOgBlogUrl())
                .publishedAt(blog.getPublishedAt())
                .deletedAt(blog.getDeletedAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .comments(blog.getComments().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}


