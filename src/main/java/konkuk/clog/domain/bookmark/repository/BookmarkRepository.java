package konkuk.clog.domain.bookmark.repository;

import java.util.List;
import java.util.Optional;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.bookmark.domain.Bookmark;
import konkuk.clog.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUser(User user);

    Optional<Bookmark> findByUserAndBlog(User user, Blog blog);
}



