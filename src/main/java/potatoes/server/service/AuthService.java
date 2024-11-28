package potatoes.server.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import potatoes.server.dto.CreateUserRequest;
import potatoes.server.dto.GetUserResponse;
import potatoes.server.dto.SignInUserRequest;
import potatoes.server.dto.UpdateUserRequest;
import potatoes.server.entity.User;
import potatoes.server.error.exception.AlreadyExistsEmail;
import potatoes.server.error.exception.InvalidSignInInformation;
import potatoes.server.error.exception.UserNotFound;
import potatoes.server.repository.UserRepository;
import potatoes.server.utils.crypto.PasswordEncoder;
import potatoes.server.utils.jwt.JwtTokenUtil;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final JwtTokenUtil jwtTokenUtil;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public void register(CreateUserRequest request) {

		Optional<User> optionalUser = userRepository.findByEmail(request.email());
		if (optionalUser.isPresent()) {
			throw new AlreadyExistsEmail();
		}

		User user = User.builder()
			.email(request.email())
			.password(passwordEncoder.encrypt(request.password()))
			.name(request.name())
			.companyName(request.companyName())
			.build();
		userRepository.save(user);
	}

	public String signIn(SignInUserRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(InvalidSignInInformation::new);

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new InvalidSignInInformation();
		}

		return jwtTokenUtil.createToken(String.valueOf(user.getId()));
	}

	public GetUserResponse find(Long id) {
		User user = userRepository.findById(id)
			.orElseThrow(UserNotFound::new);

		return GetUserResponse.fromEntity(user);
	}

	@Transactional
	public GetUserResponse update(Long id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
			.orElseThrow(UserNotFound::new);
		user.updateUserData(request.companyName(), request.image());
		return GetUserResponse.fromEntity(user);
	}
}
