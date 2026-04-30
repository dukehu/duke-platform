package com.duke.framework.exception;

import com.duke.framework.common.Result;
import com.duke.framework.common.ResultCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnClass(AuthenticationException.class)
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }
}
