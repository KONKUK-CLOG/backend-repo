package konkuk.clog.domain.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import konkuk.clog.domain.blog.domain.BlogStatus;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BlogCreateRequest {

    @NotBlank(message = "블로그 제목은 필수입니다.")
    @Size(max = 150, message = "블로그 제목은 150자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "블로그 내용은 필수입니다.")
    private String content;

    private BlogStatus status = BlogStatus.DRAFT;

    private BlogVisibility visibility = BlogVisibility.PRIVATE;

    private String ogTitle;

    private String ogBlogUrl;
}


