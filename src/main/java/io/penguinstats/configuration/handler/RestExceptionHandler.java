package io.penguinstats.configuration.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.penguinstats.util.CookieUtil;
import io.penguinstats.util.ValidationUtil;
import io.penguinstats.util.exception.BusinessException;
import io.penguinstats.util.exception.ServiceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.WebUtils;

import io.penguinstats.controller.v2.response.ErrorResponseWrapper;
import io.penguinstats.enums.ErrorCode;
import io.penguinstats.util.exception.DatabaseException;
import io.penguinstats.util.exception.NotFoundException;
import io.penguinstats.util.exception.PenguinException;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ControllerAdvice
public class RestExceptionHandler {

    private static final String SERVICE_UNAVAILABLE = "Service unavailable, you can report to penguin-statistics.";

    @Autowired
    private CookieUtil cookieUtil;


    @ExceptionHandler({BusinessException.class})
    public ResponseEntity<ErrorResponseWrapper> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String userID = cookieUtil.readUserIDFromCookie(request);
        log.info("business exception: uid={} msg={}", userID, ex.getMessage());

        ErrorResponseWrapper errorResponse = new ErrorResponseWrapper(
                ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.BUSINESS_EXCEPTION, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public final ResponseEntity<? extends ErrorResponseWrapper> handleBindException(Exception ex,
                                                                                    HttpServletRequest request) {
        ErrorResponseWrapper apiResponse = new ErrorResponseWrapper(ErrorCode.INVALID_PARAMETER, ex.getMessage());
        String validationErrorStr = "";
        if (ex instanceof BindException) {
            validationErrorStr = ValidationUtil.fieldErrorToString(((BindException) ex).getBindingResult()
                    .getFieldErrors());
            apiResponse.setMessage(validationErrorStr);
        } else if (ex instanceof MethodArgumentNotValidException) {
            validationErrorStr = ValidationUtil.fieldErrorToString((((MethodArgumentNotValidException) ex)
                    .getBindingResult().getFieldErrors()));
            apiResponse.setMessage(validationErrorStr);
        }
        String userID = cookieUtil.readUserIDFromCookie(request);
        log.info("validation error: uid={} validationInfo={}", userID, validationErrorStr);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ErrorResponseWrapper> handleServiceException(ServiceException ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("service exception", ex);

        ErrorResponseWrapper errorResponse = new ErrorResponseWrapper(
                ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.SERVICE_EXCEPTION, SERVICE_UNAVAILABLE);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * For unexpected exceptions, because there are so many users currently using our API,
     * stack traces should not be returned to users.
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponseWrapper> handle(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("unexpected exception", ex);

        ErrorResponseWrapper errorResponse = new ErrorResponseWrapper(ErrorCode.UNKNOWN, SERVICE_UNAVAILABLE);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}
