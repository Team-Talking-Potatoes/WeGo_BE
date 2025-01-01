package potatoes.server.utils.stomp;

import java.security.Principal;

import lombok.Getter;

@Getter
public class StompUserPrincipal implements Principal {
	private Long userId;
	private String sessionId;

	public StompUserPrincipal(Long userId, String sessionId) {
		this.userId = userId;
		this.sessionId = sessionId;
	}

	@Override
	public String getName() {
		return "";
	}
}
