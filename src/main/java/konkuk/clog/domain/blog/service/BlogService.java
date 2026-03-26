package konkuk.clog.domain.blog.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.blog.dto.BlogPublishRequest;
import konkuk.clog.domain.blog.dto.BlogPublishResponse;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.domain.BlogStatus;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import konkuk.clog.domain.blog.dto.BlogCreateRequest;
import konkuk.clog.domain.blog.dto.BlogResponse;
import konkuk.clog.domain.blog.dto.BlogSummaryResponse;
import konkuk.clog.domain.blog.dto.BlogUpdateRequest;
import konkuk.clog.domain.blog.repository.BlogRepository;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Value("${app.blog.public-base-url:http://localhost:3000/blog}")
    private String publicBlogBaseUrl;

    @Transactional
    public BlogResponse createBlog(Long userId, BlogCreateRequest request) {
        User author = getUser(userId);

        Blog blog = Blog.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .status(defaultStatus(request.getStatus()))
                .visibility(defaultVisibility(request.getVisibility()))
                .ogTitle(request.getOgTitle())
                .ogBlogUrl(request.getOgBlogUrl())
                .build();

        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            blog.publish(LocalDateTime.now());
        }

        return BlogResponse.from(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse updateBlog(Long userId, Long blogId, BlogUpdateRequest request) {
        Blog blog = getBlog(blogId);
        validateAuthor(userId, blog);

        BlogStatus nextStatus = request.getStatus() != null ? request.getStatus() : blog.getStatus();
        BlogVisibility nextVisibility =
                request.getVisibility() != null ? request.getVisibility() : blog.getVisibility();

        blog.update(request.getTitle(), request.getContent(), nextStatus, nextVisibility,
                request.getOgTitle(), request.getOgBlogUrl());

        if (nextStatus == BlogStatus.PUBLISHED && blog.getPublishedAt() == null) {
            blog.publish(LocalDateTime.now());
        }

        if (nextStatus == BlogStatus.DELETED) {
            blog.markDeleted(LocalDateTime.now());
        }

        return BlogResponse.from(blog);
    }

    @Transactional
    public void deleteBlog(Long userId, Long blogId) {
        Blog blog = getBlog(blogId);
        validateAuthor(userId, blog);
        blog.markDeleted(LocalDateTime.now());
    }

    @Transactional
    public void publishBlog(Long userId, Long blogId) {
        Blog blog = getBlog(blogId);
        validateAuthor(userId, blog);
        blog.publish(LocalDateTime.now());
    }

    /**
     * VS Code Extension 발행 전용 — DRAFT 없이 즉시 {@link BlogStatus#PUBLISHED} 및 공개 URL 반환.
     */
    @Transactional
    public BlogPublishResponse publishFromExtension(Long userId, BlogPublishRequest request) {
        User author = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        Blog blog = Blog.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .status(BlogStatus.DRAFT)
                .visibility(request.getVisibility())
                .ogTitle(request.getTitle())
                .ogBlogUrl(null)
                .codeDiff(request.getCodeDiff())
                .codeContext(request.getCodeContext())
                .prompt(request.getPrompt())
                .chatSessionId(request.getChatSessionId())
                .build();
        blog.publish(now);
        blog = blogRepository.save(blog);
        String url = buildPublicBlogUrl(blog.getId());
        blog.update(request.getTitle(), request.getContent(), BlogStatus.PUBLISHED, request.getVisibility(),
                request.getTitle(), url);
        return new BlogPublishResponse(blog.getId(), url);
    }

    private String buildPublicBlogUrl(Long blogId) {
        String base = publicBlogBaseUrl.endsWith("/")
                ? publicBlogBaseUrl.substring(0, publicBlogBaseUrl.length() - 1)
                : publicBlogBaseUrl;
        return blogId == null ? base : base + "/" + blogId;
    }

    @Transactional
    public void increaseViewCount(Long blogId) {
        Blog blog = getBlog(blogId);
        blog.increaseViewCount();
    }

    @Transactional(readOnly = true)
    public BlogResponse getBlogDetail(Long blogId) {
        return BlogResponse.from(getBlog(blogId));
    }

    @Transactional(readOnly = true)
    public List<BlogSummaryResponse> getUserBlogs(Long userId) {
        User user = getUser(userId);
        return blogRepository.findAllByAuthor(user).stream()
                .map(BlogSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlogSummaryResponse> getPublishedBlogs() {
        return blogRepository.findAllByStatus(BlogStatus.PUBLISHED).stream()
                .map(BlogSummaryResponse::from)
                .collect(Collectors.toList());
    }

    private Blog getBlog(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOG_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateAuthor(Long userId, Blog blog) {
        if (!blog.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private BlogStatus defaultStatus(BlogStatus status) {
        return status != null ? status : BlogStatus.DRAFT;
    }

    private BlogVisibility defaultVisibility(BlogVisibility visibility) {
        return visibility != null ? visibility : BlogVisibility.PRIVATE;
    }
}



