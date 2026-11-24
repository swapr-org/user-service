package app.swapr.user.exception;

import java.util.List;

/** Thrown when a request has invalid fields, carrying every field error found. */
public class ValidationException extends RuntimeException {

  private final List<FieldError> errors;

  /** Creates the exception for the given field errors. */
  public ValidationException(List<FieldError> errors) {
    super("The request has invalid fields.");
    this.errors = List.copyOf(errors);
  }

  /** Returns the field errors. */
  public List<FieldError> errors() {
    return errors;
  }
}
