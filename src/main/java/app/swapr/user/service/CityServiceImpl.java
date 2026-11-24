package app.swapr.user.service;

import app.swapr.user.exception.FieldError;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import app.swapr.user.repository.CityRepository;
import app.swapr.user.util.TrimmedText;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class CityServiceImpl implements CityService {

  private static final int MIN_QUERY_LENGTH = 2;
  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");

  private final CityRepository cityRepository;

  CityServiceImpl(CityRepository cityRepository) {
    this.cityRepository = cityRepository;
  }

  @Override
  public List<City> search(String query) throws ValidationException {
    TrimmedText trimmed = TrimmedText.of(query == null ? "" : query);
    if (trimmed.length() < MIN_QUERY_LENGTH) {
      throw new ValidationException(
          List.of(new FieldError("query", "must be at least " + MIN_QUERY_LENGTH + " characters")));
    }
    String lowered = trimmed.value().toLowerCase(Locale.ROOT);
    String folded = COMBINING_MARKS.matcher(Normalizer.normalize(lowered, Normalizer.Form.NFD)).replaceAll("");
    return cityRepository.searchByPrefix(escapeLike(lowered) + "%", escapeLike(folded) + "%");
  }

  @Override
  public Optional<City> find(UUID id) {
    return cityRepository.findById(id);
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
