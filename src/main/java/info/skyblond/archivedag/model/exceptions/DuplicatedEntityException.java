package info.skyblond.archivedag.model.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Duplicated")
public class DuplicatedEntityException extends RuntimeException{
    public DuplicatedEntityException(String entityName) {
        super("Duplicated " + entityName);
    }
}
