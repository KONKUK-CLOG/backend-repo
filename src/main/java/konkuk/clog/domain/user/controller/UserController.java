package konkuk.clog.domain.user.controller;

import jakarta.validation.Valid;
import konkuk.clog.domain.user.dto.GithubTokenUpdateRequest;
import konkuk.clog.domain.user.dto.UserCreateRequest;
import konkuk.clog.domain.user.dto.UserResponse;
import konkuk.clog.domain.user.service.UserService;
import konkuk.clog.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.registerUser(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.findUser(userId));
    }

    @PatchMapping("/{userId}/github-token")
    public ApiResponse<Void> updateGithubToken(@PathVariable Long userId,
            @Valid @RequestBody GithubTokenUpdateRequest request) {
        userService.updateGithubToken(userId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}/github-token")
    public ApiResponse<Void> revokeGithubToken(@PathVariable Long userId) {
        userService.revokeGithubToken(userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success();
    }
}



