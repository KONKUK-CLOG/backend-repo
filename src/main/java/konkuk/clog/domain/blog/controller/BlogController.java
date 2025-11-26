package konkuk.clog.domain.blog.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.blog.dto.BlogCreateRequest;
import konkuk.clog.domain.blog.dto.BlogResponse;
import konkuk.clog.domain.blog.dto.BlogSummaryResponse;
import konkuk.clog.domain.blog.dto.BlogUpdateRequest;
import konkuk.clog.domain.blog.service.BlogService;
import konkuk.clog.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    private static final String USER_HEADER = "X-User-Id";

    @PostMapping
    public ApiResponse<BlogResponse> createBlog(
            @RequestHeader(USER_HEADER) Long userId,
            @Valid @RequestBody BlogCreateRequest request) {
        return ApiResponse.success(blogService.createBlog(userId, request));
    }

    @PutMapping("/{blogId}")
    public ApiResponse<BlogResponse> updateBlog(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long blogId,
            @Valid @RequestBody BlogUpdateRequest request) {
        return ApiResponse.success(blogService.updateBlog(userId, blogId, request));
    }

    @DeleteMapping("/{blogId}")
    public ApiResponse<Void> deleteBlog(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long blogId) {
        blogService.deleteBlog(userId, blogId);
        return ApiResponse.success();
    }

    @PostMapping("/{blogId}/publish")
    public ApiResponse<Void> publishBlog(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long blogId) {
        blogService.publishBlog(userId, blogId);
        return ApiResponse.success();
    }

    @PostMapping("/{blogId}/view")
    public ApiResponse<Void> increaseView(@PathVariable Long blogId) {
        blogService.increaseViewCount(blogId);
        return ApiResponse.success();
    }

    @GetMapping("/{blogId}")
    public ApiResponse<BlogResponse> getBlog(@PathVariable Long blogId) {
        return ApiResponse.success(blogService.getBlogDetail(blogId));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<BlogSummaryResponse>> getUserBlogs(@PathVariable Long userId) {
        return ApiResponse.success(blogService.getUserBlogs(userId));
    }

    @GetMapping("/published")
    public ApiResponse<List<BlogSummaryResponse>> getPublishedBlogs() {
        return ApiResponse.success(blogService.getPublishedBlogs());
    }
}



