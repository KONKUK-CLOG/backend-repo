package konkuk.clog.domain.bookmark.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.bookmark.dto.BookmarkCreateRequest;
import konkuk.clog.domain.bookmark.dto.BookmarkResponse;
import konkuk.clog.domain.bookmark.service.BookmarkService;
import konkuk.clog.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private static final String USER_HEADER = "X-User-Id";

    @PostMapping
    public ApiResponse<BookmarkResponse> addBookmark(
            @RequestHeader(USER_HEADER) Long userId,
            @Valid @RequestBody BookmarkCreateRequest request) {
        return ApiResponse.success(bookmarkService.addBookmark(userId, request));
    }

    @DeleteMapping("/{bookmarkId}")
    public ApiResponse<Void> removeBookmark(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long bookmarkId) {
        bookmarkService.removeBookmark(userId, bookmarkId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<BookmarkResponse>> getBookmarks(
            @RequestHeader(USER_HEADER) Long userId) {
        return ApiResponse.success(bookmarkService.getBookmarks(userId));
    }
}



