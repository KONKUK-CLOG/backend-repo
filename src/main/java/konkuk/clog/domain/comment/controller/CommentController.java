package konkuk.clog.domain.comment.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.comment.dto.CommentCreateRequest;
import konkuk.clog.domain.comment.dto.CommentResponse;
import konkuk.clog.domain.comment.dto.CommentUpdateRequest;
import konkuk.clog.domain.comment.service.CommentService;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private static final String USER_HEADER = "X-User-Id";

    @PostMapping
    public ApiResponse<CommentResponse> createComment(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.createComment(userId, request));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ApiResponse.success(commentService.updateComment(userId, commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
        return ApiResponse.success();
    }

    @GetMapping("/blog/{blogId}")
    public ApiResponse<List<CommentResponse>> getCommentsByBlog(@PathVariable Long blogId) {
        return ApiResponse.success(commentService.getCommentsByBlog(blogId));
    }
}



