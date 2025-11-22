package konkuk.clog.domain.bookmark.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookmarkCreateRequest {

    @NotNull(message = "블로그 ID는 필수입니다.")
    private Long blogId;
}


