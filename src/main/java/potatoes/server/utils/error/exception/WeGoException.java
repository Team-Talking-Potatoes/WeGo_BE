package potatoes.server.utils.error.exception;

import lombok.Getter;
import potatoes.server.utils.error.ErrorCode;

@Getter
public class WeGoException extends RuntimeException {

	private final ErrorCode errorCode;

	public WeGoException(String message, ErrorCode errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public WeGoException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
