package konkuk.clog.domain.blog.repository;

import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.domain.BlogStatus;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import konkuk.clog.domain.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    List<Blog> findAllByAuthor(User author);

    List<Blog> findAllByAuthorAndStatusAndVisibility(User author, BlogStatus status, BlogVisibility visibility);

    List<Blog> findAllByStatus(BlogStatus status);

    Optional<Blog> findByOgBlogUrl(String ogBlogUrl);
}



