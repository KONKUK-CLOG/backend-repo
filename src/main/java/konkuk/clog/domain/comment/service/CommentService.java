package konkuk.clog.domain.comment.service;

import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.repository.BlogRepository;
import konkuk.clog.domain.comment.domain.AuthorType;
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
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long userId, CommentCreateRequest request) {
        Blog blog = getBlog(request.getBlogId());

        User user = resolveAuthor(userId, request.getAuthorType());
        String guestNickname = resolveGuestNickname(request);

        Comment comment = Comment.builder()
                .author(user)
                .authorType(request.getAuthorType())
                .guestNickname(guestNickname)
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
        if (comment.isGuestAuthor()) {
            throw new BusinessException(ErrorCode.INVALID_AUTHOR_TYPE);
        }
        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private User resolveAuthor(Long userId, AuthorType authorType) {
        if (AuthorType.GUEST.equals(authorType)) {
            return null;
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.MISSING_USER_ID_FOR_COMMENT);
        }
        return getUser(userId);
    }

    private String resolveGuestNickname(CommentCreateRequest request) {
        if (request.isGuestComment()) {
            if (!StringUtils.hasText(request.getGuestNickname())) {
                throw new BusinessException(ErrorCode.GUEST_NICKNAME_REQUIRED);
            }
            return request.getGuestNickname();
        }
        return null;
    }
}



