package konkuk.clog.domain.bookmark.service;

import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.blog.domain.Blog;
import konkuk.clog.domain.blog.repository.BlogRepository;
import konkuk.clog.domain.bookmark.domain.Bookmark;
import konkuk.clog.domain.bookmark.dto.BookmarkCreateRequest;
import konkuk.clog.domain.bookmark.dto.BookmarkResponse;
import konkuk.clog.domain.bookmark.repository.BookmarkRepository;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookmarkResponse addBookmark(Long userId, BookmarkCreateRequest request) {
        User user = getUser(userId);
        Blog blog = getBlog(request.getBlogId());

        bookmarkRepository.findByUserAndBlog(user, blog).ifPresent(bookmark -> {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        });

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .blog(blog)
                .build();

        return BookmarkResponse.from(bookmarkRepository.save(bookmark));
    }

    @Transactional
    public void removeBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = getBookmark(bookmarkId);
        if (!bookmark.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(Long userId) {
        User user = getUser(userId);
        return bookmarkRepository.findAllByUser(user).stream()
                .map(BookmarkResponse::from)
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

    private Bookmark getBookmark(Long bookmarkId) {
        return bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));
    }
}



