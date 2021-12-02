package info.skyblond.archivedag.model.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Permission denied")
public class PermissionDeniedException extends RuntimeException{
    public PermissionDeniedException(String message) {
        super(message);
    }
}
