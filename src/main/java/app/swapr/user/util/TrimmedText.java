package app.swapr.user.util;

/** Text with surrounding whitespace removed, and its length in Unicode code points. */
public record TrimmedText(String value, int length) {

  /** Strips the raw text and counts its code points. */
  public static TrimmedText of(String raw) {
    String value = raw.strip();
    return new TrimmedText(value, value.codePointCount(0, value.length()));
  }
}
