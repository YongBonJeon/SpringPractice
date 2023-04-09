package hello.exception.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 500 -> 400
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")
public class badRequestException extends RuntimeException {

}
