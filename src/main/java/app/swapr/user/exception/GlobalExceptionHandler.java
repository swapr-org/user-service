package app.swapr.user.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(ValidationException.class)
  ProblemDetail invalid(ValidationException exception, HttpServletRequest request) {
    ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    problem.setProperty("errors", exception.errors());
    return problem;
  }

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail notFound(NotFoundException exception, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }
}
