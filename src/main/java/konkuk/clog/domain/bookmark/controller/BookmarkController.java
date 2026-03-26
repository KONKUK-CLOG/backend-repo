package konkuk.clog.domain.bookmark.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.bookmark.dto.BookmarkCreateRequest;
import konkuk.clog.domain.bookmark.dto.BookmarkResponse;
import konkuk.clog.domain.bookmark.service.BookmarkService;
import konkuk.clog.global.dto.ApiResponse;
import konkuk.clog.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ApiResponse<BookmarkResponse> addBookmark(
            @Valid @RequestBody BookmarkCreateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(bookmarkService.addBookmark(userId, request));
    }

    @DeleteMapping("/{bookmarkId}")
    public ApiResponse<Void> removeBookmark(@PathVariable Long bookmarkId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        bookmarkService.removeBookmark(userId, bookmarkId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<BookmarkResponse>> getBookmarks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(bookmarkService.getBookmarks(userId));
    }
}
