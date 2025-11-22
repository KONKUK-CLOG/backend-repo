package konkuk.clog.domain.comment.dto;

import java.time.LocalDateTime;
import konkuk.clog.domain.comment.domain.Comment;
import konkuk.clog.domain.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long blogId;
    private UserResponse author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .blogId(comment.getBlog().getId())
                .author(UserResponse.from(comment.getAuthor()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}


