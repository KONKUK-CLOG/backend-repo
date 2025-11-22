package konkuk.clog.domain.comment.repository;

import java.util.List;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.comment.domain.Comment;
import konkuk.clog.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByBlog(Blog blog);

    List<Comment> findAllByAuthor(User user);
}

