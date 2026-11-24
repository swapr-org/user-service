package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import app.swapr.user.cities.CityService;
import app.swapr.user.errors.FieldError;
import app.swapr.user.errors.ValidationException;
import app.swapr.user.text.TrimmedText;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, updates and looks up user profiles. */
@Service
@Transactional
public class ProfileService {

  private static final int DEFAULT_RADIUS_KM = 25;
  private static final int MAX_NAME_LENGTH = 50;
  private static final BigDecimal MIN_RADIUS = BigDecimal.ONE;
  private static final BigDecimal MAX_RADIUS = BigDecimal.valueOf(100);

  private final ProfileStore store;
  private final CityService cities;

  ProfileService(ProfileStore store, CityService cities) {
    this.store = store;
    this.cities = cities;
  }

  ProfileView getOrCreate(UUID userId) {
    return loadOrCreate(userId).view();
  }

  ProfileView update(UUID userId, UpdateProfileCommand command) {
    List<FieldError> errors = new ArrayList<>();
    String displayName = validName(command.displayName(), errors);
    City city = validCity(command.cityId(), errors);
    Integer radiusKm = validRadius(command.radiusKm(), errors);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    Profile updated = loadOrCreate(userId).replaced(displayName, city, radiusKm, Instant.now());
    store.save(updated);
    return updated.view();
  }

  @Transactional(readOnly = true)
  Optional<PublicProfileView> publicProfile(UUID id) {
    return store.find(id).filter(Profile::isComplete).map(Profile::publicView);
  }

  /** Hard-deletes the user's profile, doing nothing when none is stored. */
  public void deleteAllFor(UUID userId) {
    store.deleteById(userId);
  }

  private Profile loadOrCreate(UUID userId) {
    store.insertIfAbsent(userId, DEFAULT_RADIUS_KM);
    return store.find(userId).orElseThrow();
  }

  private static String validName(String raw, List<FieldError> errors) {
    if (raw == null) {
      errors.add(new FieldError("displayName", "is required"));
      return null;
    }
    TrimmedText name = TrimmedText.of(raw);
    if (name.length() < 1 || name.length() > MAX_NAME_LENGTH) {
      errors.add(new FieldError("displayName",
          "must be 1 to " + MAX_NAME_LENGTH + " characters after trimming spaces"));
      return null;
    }
    return name.value();
  }

  private City validCity(String raw, List<FieldError> errors) {
    if (raw == null) {
      errors.add(new FieldError("cityId", "is required"));
      return null;
    }
    Optional<City> city = parseUuid(raw).flatMap(cities::find);
    if (city.isEmpty()) {
      errors.add(new FieldError("cityId", "must be the ID of an existing city"));
      return null;
    }
    return city.get();
  }

  private static Integer validRadius(BigDecimal raw, List<FieldError> errors) {
    if (raw == null) {
      errors.add(new FieldError("radiusKm", "is required"));
      return null;
    }
    boolean whole = raw.stripTrailingZeros().scale() <= 0;
    if (!whole || raw.compareTo(MIN_RADIUS) < 0 || raw.compareTo(MAX_RADIUS) > 0) {
      errors.add(new FieldError("radiusKm", "must be a whole number from " + MIN_RADIUS + " to " + MAX_RADIUS));
      return null;
    }
    return raw.intValueExact();
  }

  private static Optional<UUID> parseUuid(String raw) {
    try {
      return Optional.of(UUID.fromString(raw));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
