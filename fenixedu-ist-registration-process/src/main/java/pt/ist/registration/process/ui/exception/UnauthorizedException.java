package pt.ist.registration.process.ui.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Not found")  // 404
public class UnauthorizedException extends RuntimeException {

}
