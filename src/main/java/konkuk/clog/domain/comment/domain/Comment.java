package konkuk.clog.domain.comment.domain;

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
import jakarta.persistence.Table;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.global.jpa.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
```mermaid
classDiagram
    class Blog
    class User
    class Comment {
        +Long id
        +AuthorType authorType
        +String guestNickname
        +String content
    }
    class AuthorType {
        <<enumeration>>
        GUEST
        USER
    }

    Blog "1" <-- "many" Comment : blog
    User "1" <-- "many" Comment : author
```
*/
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments")
@Entity
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 20)
    private AuthorType authorType;

    @Column(name = "guest_nick_name", length = 50)
    private String guestNickname;

    @Column(name = "comment_content", nullable = false, length = 1000)
    private String content;

    @Builder
    private Comment(Blog blog, User author, AuthorType authorType, String guestNickname,
            String content) {
        this.blog = blog;
        this.author = author;
        this.authorType = authorType;
        this.guestNickname = guestNickname;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isGuestAuthor() {
        return AuthorType.GUEST.equals(this.authorType);
    }
}


