package potatoes.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import potatoes.server.dto.DeleteUserRequest;
import potatoes.server.dto.ResetPasswordRequest;
import potatoes.server.service.UserService;
import potatoes.server.utils.annotation.Authorization;

@Tag(name = "User", description = "User API")
@RequestMapping("/users")
@RequiredArgsConstructor
@RestController
public class UserController {

	private final UserService userService;

	@Operation(summary = "회원정보 수정", description = "프로필이미지, 닉네임, 설명에 대한 정보 수정")
	@PutMapping("")
	public ResponseEntity<Void> updateUserProfile(
		@RequestParam(required = false) MultipartFile profileImage,
		@RequestParam(required = false) String nickname,
		@RequestParam(required = false) String description,
		@Authorization @Parameter(hidden = true) Long userId
	) {
		userService.updateUserProfile(profileImage, nickname, description, userId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원탈퇴", description = "토큰과 패스워드를 받는다")
	@DeleteMapping("")
	public ResponseEntity<Void> deleteUser(
		@RequestBody @Valid DeleteUserRequest request,
		@Authorization @Parameter(hidden = true) Long userId
	) {
		userService.deleteUser(request, userId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "비밀번호 재설정", description = "로그인 된 상황에 대한 비밀번호 재설정")
	@PutMapping("/password")
	public ResponseEntity<Void> resetPassword(
		@RequestBody @Valid ResetPasswordRequest request,
		@Authorization @Parameter(hidden = true) Long userId
	) {
		userService.resetPassword(request, userId);
		return ResponseEntity.ok().build();
	}
}
