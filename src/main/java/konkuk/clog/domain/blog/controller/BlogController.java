package konkuk.clog.domain.blog.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.blog.dto.BlogCreateRequest;
import konkuk.clog.domain.blog.dto.BlogPublishRequest;
import konkuk.clog.domain.blog.dto.BlogPublishResponse;
import konkuk.clog.domain.blog.dto.BlogResponse;
import konkuk.clog.domain.blog.dto.BlogSummaryResponse;
import konkuk.clog.domain.blog.dto.BlogUpdateRequest;
import konkuk.clog.domain.blog.service.BlogService;
import konkuk.clog.global.dto.ApiResponse;
import konkuk.clog.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public ApiResponse<BlogResponse> createBlog(@Valid @RequestBody BlogCreateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(blogService.createBlog(userId, request));
    }

    @PostMapping("/extension/publish")
    public ApiResponse<BlogPublishResponse> publishFromExtension(
            @Valid @RequestBody BlogPublishRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(blogService.publishFromExtension(userId, request));
    }

    @PutMapping("/{blogId}")
    public ApiResponse<BlogResponse> updateBlog(
            @PathVariable Long blogId,
            @Valid @RequestBody BlogUpdateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(blogService.updateBlog(userId, blogId, request));
    }

    @DeleteMapping("/{blogId}")
    public ApiResponse<Void> deleteBlog(@PathVariable Long blogId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        blogService.deleteBlog(userId, blogId);
        return ApiResponse.success();
    }

    @PostMapping("/{blogId}/publish")
    public ApiResponse<Void> publishBlog(@PathVariable Long blogId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        blogService.publishBlog(userId, blogId);
        return ApiResponse.success();
    }

    @PostMapping("/{blogId}/view")
    public ApiResponse<Void> increaseView(@PathVariable Long blogId) {
        blogService.increaseViewCount(blogId);
        return ApiResponse.success();
    }

    /** 숫자 id 만 — {@code /generate} 등과 충돌하지 않도록 제한. */
    @GetMapping("/{blogId:\\d+}")
    public ApiResponse<BlogResponse> getBlog(@PathVariable Long blogId) {
        Long viewer = SecurityUtils.tryGetCurrentUserId().orElse(null);
        return ApiResponse.success(blogService.getBlogDetail(blogId, viewer));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<BlogSummaryResponse>> getUserBlogs(@PathVariable Long userId) {
        Long viewer = SecurityUtils.tryGetCurrentUserId().orElse(null);
        return ApiResponse.success(blogService.getUserBlogsForProfile(userId, viewer));
    }

    @GetMapping("/published")
    public ApiResponse<List<BlogSummaryResponse>> getPublishedBlogs() {
        return ApiResponse.success(blogService.getPublishedBlogs());
    }
}
