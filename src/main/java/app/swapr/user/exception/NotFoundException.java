package app.swapr.user.exception;

/** Thrown when a requested resource does not exist, carrying the detail to report. */
public class NotFoundException extends RuntimeException {

  /** Creates the exception with the detail shown to the caller. */
  public NotFoundException(String detail) {
    super(detail);
  }
}
