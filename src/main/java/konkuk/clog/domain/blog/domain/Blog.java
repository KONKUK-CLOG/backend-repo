package konkuk.clog.domain.blog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import konkuk.clog.domain.bookmark.domain.Bookmark;
import konkuk.clog.domain.comment.domain.Comment;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.global.jpa.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "blogs", uniqueConstraints = @UniqueConstraint(columnNames = "og_blog_url"))
@Entity
public class Blog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogVisibility visibility;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "og_title")
    private String ogTitle;

    @Column(name = "og_blog_url", length = 512)
    private String ogBlogUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Extension/Lambda 생성 시 diff 원본(선택). */
    @Column(name = "code_diff", columnDefinition = "TEXT")
    private String codeDiff;

    /** 첨부 코드 맥락(파일·라인 등 텍스트). */
    @Column(name = "code_context", columnDefinition = "TEXT")
    private String codeContext;

    /** 유저 프롬프트 원문. */
    @Column(columnDefinition = "TEXT")
    private String prompt;

    /** MongoDB 채팅 세션 ObjectId 문자열. */
    @Column(name = "chat_session_id", length = 32)
    private String chatSessionId;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Bookmark> bookmarks = new ArrayList<>();

    @Builder
    private Blog(User author, String title, String content, BlogStatus status,
            BlogVisibility visibility, String ogTitle, String ogBlogUrl,
            String codeDiff, String codeContext, String prompt, String chatSessionId) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.status = status != null ? status : BlogStatus.DRAFT;
        this.visibility = visibility != null ? visibility : BlogVisibility.PUBLIC;
        this.ogTitle = ogTitle;
        this.ogBlogUrl = ogBlogUrl;
        this.codeDiff = codeDiff;
        this.codeContext = codeContext;
        this.prompt = prompt;
        this.chatSessionId = chatSessionId;
        this.viewCount = 0L;
    }

    public void update(String title, String content, BlogStatus status, BlogVisibility visibility,
            String ogTitle, String ogBlogUrl) {
        this.title = title;
        this.content = content;
        this.status = status;
        this.visibility = visibility;
        this.ogTitle = ogTitle;
        this.ogBlogUrl = ogBlogUrl;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void publish(LocalDateTime publishedAt) {
        this.status = BlogStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.deletedAt = null;
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.status = BlogStatus.DELETED;
        this.deletedAt = deletedAt;
    }
}


