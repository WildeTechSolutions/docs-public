import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

@RestControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value
            = { ExpiredJwtException.class })
    protected ResponseEntity<Object> expiredToken(
            RuntimeException ex, WebRequest request) {

        ExpiredJwtException jwtException = (ExpiredJwtException) ex;

        String message = ExceptionUtils.getMessage(ex);
        log.debug(message);


        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setExceptionMessage("Expired Token");
        errorResponse.setStackTrace(ExceptionUtils.getMessage(ex));
        errorResponse.setStatus("401");

//        String dsId = request.getHeader("sm_universalid");
//
//        errorResponse.setDsId(dsId);

        return handleExceptionInternal(ex, errorResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { UnsupportedJwtException.class })
    protected ResponseEntity<Object> noToken(
            RuntimeException ex, WebRequest request) {

        String message = ExceptionUtils.getMessage(ex);
        log.debug(message);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setExceptionMessage("Not Authenticated");
        errorResponse.setStackTrace(ExceptionUtils.getMessage(ex));
        errorResponse.setStatus("401");

//        String dsId = request.getHeader("sm_universalid");
//
//        errorResponse.setDsId(dsId);

        return handleExceptionInternal(ex, errorResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ResponseStatusException.class })
    protected ResponseEntity<Object> handleNotFound(
            RuntimeException ex, WebRequest request) {

        ResponseStatusException responseStatusException = (ResponseStatusException) ex;

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        log.debug(stackTrace);


        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setExceptionMessage(responseStatusException.getReason());
        errorResponse.setStackTrace(ExceptionUtils.getStackTrace(ex));
        errorResponse.setStatus(Integer.toString(responseStatusException.getStatusCode().value()));

//        String dsId = request.getHeader("sm_universalid");
//
//        errorResponse.setDsId(dsId);

        return handleExceptionInternal(ex, errorResponse,
                new HttpHeaders(), responseStatusException.getStatusCode(), request);
    }

    @ExceptionHandler(value
            = { Exception.class })
    protected ResponseEntity<Object> handleConflict(
            RuntimeException ex, WebRequest request) {


        String stackTrace = ExceptionUtils.getStackTrace(ex);
        log.error(stackTrace);


        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setExceptionMessage(ExceptionUtils.getMessage(ex));
        errorResponse.setStackTrace(ExceptionUtils.getStackTrace(ex));
        errorResponse.setStatus("500");

//        String dsId = request.getHeader("sm_universalid");
//
//        errorResponse.setDsId(dsId);

        return handleExceptionInternal(ex, errorResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
