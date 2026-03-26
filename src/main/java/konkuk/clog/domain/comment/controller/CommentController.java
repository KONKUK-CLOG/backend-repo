package konkuk.clog.domain.comment.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.comment.dto.CommentCreateRequest;
import konkuk.clog.domain.comment.dto.CommentResponse;
import konkuk.clog.domain.comment.dto.CommentUpdateRequest;
import konkuk.clog.domain.comment.service.CommentService;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiResponse<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtils.tryGetCurrentUserId().orElse(null);
        return ApiResponse.success(commentService.createComment(userId, request));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(commentService.updateComment(userId, commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        commentService.deleteComment(userId, commentId);
        return ApiResponse.success();
    }

    @GetMapping("/blog/{blogId}")
    public ApiResponse<List<CommentResponse>> getCommentsByBlog(@PathVariable Long blogId) {
        return ApiResponse.success(commentService.getCommentsByBlog(blogId));
    }
}
