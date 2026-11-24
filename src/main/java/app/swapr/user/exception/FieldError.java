package app.swapr.user.exception;

/** One invalid request field, named by its JSON property. */
public record FieldError(String field, String message) {
}
