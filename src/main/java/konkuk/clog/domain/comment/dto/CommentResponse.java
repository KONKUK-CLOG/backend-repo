package konkuk.clog.domain.comment.dto;

import java.time.LocalDateTime;
import konkuk.clog.domain.comment.domain.AuthorType;
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
    private String guestNickname;
    private AuthorType authorType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .blogId(comment.getBlog().getId())
                .author(comment.getAuthor() != null ? UserResponse.from(comment.getAuthor()) : null)
                .guestNickname(comment.getGuestNickname())
                .authorType(comment.getAuthorType())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}



