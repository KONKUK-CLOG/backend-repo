package konkuk.clog.domain.user.domain;

import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.bookmark.domain.Bookmark;
import konkuk.clog.domain.comment.domain.Comment;
import konkuk.clog.global.jpa.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "github_access_token_encrypted")
    private String githubAccessTokenEncrypted;

    @Column(name = "github_token_expires_at")
    private LocalDateTime githubTokenExpiresAt;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Blog> blogs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Comment> comments = new ArrayList<>();

    @Builder
    private User(String name, String nickname, String email, String socialId, String passwordHash) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.socialId = socialId;
        this.passwordHash = passwordHash;
    }

    public void updateGithubToken(String encryptedToken, LocalDateTime expiresAt) {
        this.githubAccessTokenEncrypted = encryptedToken;
        this.githubTokenExpiresAt = expiresAt;
    }

    public void updateProfile(String name, String nickname) {
        this.name = name;
        this.nickname = nickname;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}

