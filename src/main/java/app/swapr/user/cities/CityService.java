package app.swapr.user.cities;

import app.swapr.user.errors.FieldError;
import app.swapr.user.errors.ValidationException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looks up cities from the GeoNames gazetteer. */
@Service
@Transactional(readOnly = true)
public class CityService {

  private static final int MIN_QUERY_LENGTH = 2;
  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");

  private final CityDirectory directory;

  /** Creates the service over the given city directory. */
  public CityService(CityDirectory directory) {
    this.directory = directory;
  }

  List<City> search(String query) {
    String trimmed = query == null ? "" : query.strip();
    if (trimmed.codePointCount(0, trimmed.length()) < MIN_QUERY_LENGTH) {
      throw new ValidationException(List.of(new FieldError("query", "must be at least 2 characters")));
    }
    String lowered = trimmed.toLowerCase(Locale.ROOT);
    String folded = COMBINING_MARKS.matcher(Normalizer.normalize(lowered, Normalizer.Form.NFD)).replaceAll("");
    return directory.searchByPrefix(escapeLike(lowered) + "%", escapeLike(folded) + "%");
  }

  /** Returns the city with the given ID, if it exists. */
  public Optional<City> find(UUID id) {
    return directory.find(id);
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
