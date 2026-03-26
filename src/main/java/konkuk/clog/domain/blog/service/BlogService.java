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

    /**
     * 조회수 증가 — 익명 호출 가능하므로 공개·링크 공개 글만 허용(비공개 글 조회수 스팸 방지).
     */
    @Transactional
    public void increaseViewCount(Long blogId) {
        Blog blog = getBlog(blogId);
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (blog.getVisibility() == BlogVisibility.PRIVATE) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        blog.increaseViewCount();
    }

    private boolean canViewBlog(Blog blog, Long viewerUserId) {
        if (blog.getStatus() == BlogStatus.DELETED) {
            return false;
        }
        Long authorId = blog.getAuthor().getId();
        if (blog.getStatus() == BlogStatus.DRAFT) {
            return viewerUserId != null && authorId.equals(viewerUserId);
        }
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            if (blog.getVisibility() == BlogVisibility.PUBLIC
                    || blog.getVisibility() == BlogVisibility.LINKED) {
                return true;
            }
            return viewerUserId != null && authorId.equals(viewerUserId);
        }
        return false;
    }

    /**
     * 블로그 단건 조회 — 비공개·초안·삭제 글은 작성자만 조회 가능(그 외에는 404 로 응답해 존재 여부 유출을 줄임).
     */
    @Transactional(readOnly = true)
    public BlogResponse getBlogDetail(Long blogId, Long viewerUserId) {
        Blog blog = getBlog(blogId);
        if (!canViewBlog(blog, viewerUserId)) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        return BlogResponse.from(blog);
    }

    /**
     * 특정 사용자의 글 목록 — 본인이면 전체, 타인이면 공개(PUBLIC)·발행된 글만.
     */
    @Transactional(readOnly = true)
    public List<BlogSummaryResponse> getUserBlogsForProfile(Long profileUserId, Long viewerUserId) {
        User author = getUser(profileUserId);
        if (viewerUserId != null && viewerUserId.equals(profileUserId)) {
            return blogRepository.findAllByAuthor(author).stream()
                    .map(BlogSummaryResponse::from)
                    .collect(Collectors.toList());
        }
        return blogRepository
                .findAllByAuthorAndStatusAndVisibility(author, BlogStatus.PUBLISHED, BlogVisibility.PUBLIC)
                .stream()
                .map(BlogSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /** 메인 피드 — 전체 공개(PUBLIC) 발행 글만 (비공개·링크 전용 제외). */
    @Transactional(readOnly = true)
    public List<BlogSummaryResponse> getPublishedBlogs() {
        return blogRepository.findAllByStatus(BlogStatus.PUBLISHED).stream()
                .filter(b -> b.getVisibility() == BlogVisibility.PUBLIC)
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



