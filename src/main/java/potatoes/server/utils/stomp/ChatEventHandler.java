package potatoes.server.utils.stomp;

import static potatoes.server.error.ErrorCode.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import potatoes.server.entity.ChatUser;
import potatoes.server.entity.User;
import potatoes.server.error.exception.WeGoException;
import potatoes.server.repository.ChatRepository;
import potatoes.server.repository.ChatUserRepository;
import potatoes.server.utils.jwt.JwtTokenUtil;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatEventHandler {

	private final ChatUserRepository chatUserRepository;
	private final ChatRepository chatRepository;
	private final JwtTokenUtil jwtTokenProvider;

	public void handleSubscribe(StompHeaderAccessor headerAccessor) {
		StompUserPrincipal user = (StompUserPrincipal)headerAccessor.getUser();
		String destination = headerAccessor.getDestination();
		log.info(destination);

		// destination에 따른 분기
		String[] destinationSplit = destination.substring(1).split("/");
		log.info("STOMP 구독 요청: {}", Arrays.toString(destinationSplit));

		if (destinationSplit[1].equals("chat")) {
			chatTopicVerification(destinationSplit, user.getUserId());
		} else if (destinationSplit[1].equals("alarm")) {
			alarmTopicVerification(destinationSplit, user.getUserId());
		}

	}

	public void handleConnect(StompHeaderAccessor headerAccessor) {
		Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
		String accessToken = (String)sessionAttributes.get("accessToken");
		if (accessToken == null) {
			throw new WeGoException(TOKEN_NOT_FOUND);
		}

		if (!jwtTokenProvider.validateToken(accessToken)) {
			throw new WeGoException(UNAUTHORIZED);
		}

		long userId = Long.parseLong(jwtTokenProvider.getPayload(accessToken));
		headerAccessor.setUser(new StompUserPrincipal(userId, headerAccessor.getSessionId()));
	}

	// chat관련 토픽 검증 - 구독을 요청한 유저가 해당 채팅방에 참가했는지 검증
	private void chatTopicVerification(String[] destinationSplit, Long userId) {
		long chatId;
		if (destinationSplit.length == 3 && !destinationSplit[2].equals("read")) {
			chatId = Long.parseLong(destinationSplit[2]);
		} else if (destinationSplit.length == 4 && destinationSplit[2].equals("read")) {
			chatId = Long.parseLong(destinationSplit[3]);
		} else {
			throw new WeGoException(STOMP_SUBSCRIBE_FAILED);
		}

		if (!chatRepository.existsById(chatId)) {
			throw new WeGoException(CHAT_NOT_FOUND);
		}

		List<ChatUser> chatUserList = chatUserRepository.findAllChatUserByChatID(chatId);
		User sender = null;
		for (ChatUser chatUser : chatUserList) {
			if (chatUser.getUser().getId().equals(userId)) {
				sender = chatUser.getUser();
				break;
			}
		}
		if (sender == null) {
			throw new WeGoException(USER_NOT_FOUND);
		}
	}

	private void alarmTopicVerification(String[] destinationSplit, Long userId) {
		Long destinationUserId = Long.valueOf(destinationSplit[2]);
		if (!destinationUserId.equals(userId)) {
			throw new WeGoException(STOMP_SUBSCRIBE_FAILED);
		}
	}

}
