package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.UpdateProfileRequest;
import dev.promise4.GgUd.controller.dto.UserResponse;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        log.debug("GET /api/v1/users/me - userId: {}", userId);
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 내 프로필 수정
     */
    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "닉네임 및 프로필 이미지를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        log.debug("PATCH /api/v1/users/me - userId: {}", userId);
        User user = userService.updateProfile(userId, request.getNickname(), request.getProfileImageUrl());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 프로필 이미지 업로드
     */
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드", description = "사용자 프로필 이미지를 S3에 업로드합니다. (최대 5MB, jpg/png/gif)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 파일"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserResponse> uploadProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {

        log.debug("POST /api/v1/users/me/profile-image - userId: {}", userId);
        User user = userService.uploadProfileImage(userId, image);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
