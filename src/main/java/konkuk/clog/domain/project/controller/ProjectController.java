package konkuk.clog.domain.project.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.project.dto.ProjectCreateRequest;
import konkuk.clog.domain.project.dto.ProjectFileCreateRequest;
import konkuk.clog.domain.project.dto.ProjectFileResponse;
import konkuk.clog.domain.project.dto.ProjectFileUpdateRequest;
import konkuk.clog.domain.project.dto.ProjectResponse;
import konkuk.clog.domain.project.service.ProjectService;
import konkuk.clog.global.dto.ApiResponse;
import konkuk.clog.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(projectService.createProject(userId, request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(projectService.listProjects(userId));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable String projectId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        projectService.deleteProject(userId, projectId);
        return ApiResponse.success();
    }

    @PostMapping("/{projectId}/files")
    public ApiResponse<ProjectFileResponse> addFile(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectFileCreateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(projectService.addFile(userId, projectId, request));
    }

    @GetMapping("/{projectId}/files")
    public ApiResponse<List<ProjectFileResponse>> listFiles(@PathVariable String projectId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(projectService.listFiles(userId, projectId));
    }

    @PutMapping("/{projectId}/files/{fileId}")
    public ApiResponse<ProjectFileResponse> updateFile(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @Valid @RequestBody ProjectFileUpdateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(projectService.updateFile(userId, projectId, fileId, request));
    }

    @DeleteMapping("/{projectId}/files/{fileId}")
    public ApiResponse<Void> deleteFile(
            @PathVariable String projectId, @PathVariable String fileId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        projectService.deleteFile(userId, projectId, fileId);
        return ApiResponse.success();
    }
}
