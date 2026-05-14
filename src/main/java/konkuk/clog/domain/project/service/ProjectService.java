package konkuk.clog.domain.project.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.chat.document.ChatSession;
import konkuk.clog.domain.chat.repository.ChatMessageRepository;
import konkuk.clog.domain.chat.repository.ChatSessionRepository;
import konkuk.clog.domain.project.document.Project;
import konkuk.clog.domain.project.document.ProjectFile;
import konkuk.clog.domain.project.dto.ProjectCreateRequest;
import konkuk.clog.domain.project.dto.ProjectFileCreateRequest;
import konkuk.clog.domain.project.dto.ProjectFileResponse;
import konkuk.clog.domain.project.dto.ProjectFileUpdateRequest;
import konkuk.clog.domain.project.dto.ProjectResponse;
import konkuk.clog.domain.project.repository.ProjectFileRepository;
import konkuk.clog.domain.project.repository.ProjectRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${app.project.max-files-per-project:20}")
    private int maxFilesPerProject;

    @Value("${app.project.max-file-bytes:51200}")
    private int maxFileBytes;

    @Transactional
    public ProjectResponse createProject(Long userId, ProjectCreateRequest request) {
        Instant now = Instant.now();
        Project p = Project.builder()
                .userId(userId)
                .name(request.getName().trim())
                .description(StringUtils.hasText(request.getDescription())
                        ? request.getDescription().trim()
                        : null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return ProjectResponse.from(projectRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(Long userId) {
        return projectRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteProject(Long userId, String projectId) {
        Project project = projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        List<ChatSession> sessions = chatSessionRepository.findAllByUserIdAndProjectId(userId, project.getId());
        for (ChatSession s : sessions) {
            chatMessageRepository.deleteBySessionId(s.getId());
        }
        chatSessionRepository.deleteAll(sessions);

        projectFileRepository.deleteAllByProjectId(project.getId());
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectFileResponse addFile(Long userId, String projectId, ProjectFileCreateRequest request) {
        ensureProjectOwned(userId, projectId);

        long count = projectFileRepository.countByProjectId(projectId);
        if (count >= maxFilesPerProject) {
            throw new BusinessException(ErrorCode.PROJECT_FILE_LIMIT_EXCEEDED);
        }

        String path = normalizePath(request.getFilePath());
        if (projectFileRepository.existsByProjectIdAndFilePath(projectId, path)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_FILE_PATH);
        }

        validateContentSize(request.getContent());

        Instant now = Instant.now();
        ProjectFile file = ProjectFile.builder()
                .projectId(projectId)
                .filePath(path)
                .language(StringUtils.hasText(request.getLanguage()) ? request.getLanguage().trim() : null)
                .content(request.getContent())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return ProjectFileResponse.from(projectFileRepository.save(file));
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> listFiles(Long userId, String projectId) {
        ensureProjectOwned(userId, projectId);
        return projectFileRepository.findAllByProjectIdOrderByFilePathAsc(projectId).stream()
                .map(ProjectFileResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectFileResponse updateFile(
            Long userId, String projectId, String fileId, ProjectFileUpdateRequest request) {
        ensureProjectOwned(userId, projectId);

        ProjectFile file = projectFileRepository
                .findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_FILE_NOT_FOUND));

        if (StringUtils.hasText(request.getFilePath())) {
            String newPath = normalizePath(request.getFilePath());
            if (!newPath.equals(file.getFilePath())) {
                projectFileRepository
                        .findByProjectIdAndFilePath(projectId, newPath)
                        .filter(other -> !other.getId().equals(fileId))
                        .ifPresent(other -> {
                            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_FILE_PATH);
                        });
                file.setFilePath(newPath);
            }
        }

        if (request.getLanguage() != null) {
            file.setLanguage(StringUtils.hasText(request.getLanguage())
                    ? request.getLanguage().trim()
                    : null);
        }

        if (request.getContent() != null) {
            validateContentSize(request.getContent());
            file.setContent(request.getContent());
        }

        file.setUpdatedAt(Instant.now());
        return ProjectFileResponse.from(projectFileRepository.save(file));
    }

    @Transactional
    public void deleteFile(Long userId, String projectId, String fileId) {
        ensureProjectOwned(userId, projectId);
        ProjectFile file = projectFileRepository
                .findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_FILE_NOT_FOUND));
        projectFileRepository.delete(file);
    }

    private void ensureProjectOwned(Long userId, String projectId) {
        projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private static String normalizePath(String path) {
        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (!StringUtils.hasText(p)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_FILE_PATH);
        }
        return p;
    }

    private void validateContentSize(String content) {
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxFileBytes) {
            throw new BusinessException(ErrorCode.PROJECT_FILE_TOO_LARGE);
        }
    }
}
