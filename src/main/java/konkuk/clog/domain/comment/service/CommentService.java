package konkuk.clog.domain.comment.service;

import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.repository.BlogRepository;
import konkuk.clog.domain.comment.domain.Comment;
import konkuk.clog.domain.comment.dto.CommentCreateRequest;
import konkuk.clog.domain.comment.dto.CommentResponse;
import konkuk.clog.domain.comment.dto.CommentUpdateRequest;
import konkuk.clog.domain.comment.repository.CommentRepository;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long userId, CommentCreateRequest request) {
        User user = getUser(userId);
        Blog blog = getBlog(request.getBlogId());

        Comment comment = Comment.builder()
                .author(user)
                .blog(blog)
                .content(request.getContent())
                .build();

        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getComment(commentId);
        validateAuthor(userId, comment);
        comment.updateContent(request.getContent());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getComment(commentId);
        validateAuthor(userId, comment);
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByBlog(Long blogId) {
        Blog blog = getBlog(blogId);
        return commentRepository.findAllByBlog(blog).stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Blog getBlog(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOG_NOT_FOUND));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAuthor(Long userId, Comment comment) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
    }
}


