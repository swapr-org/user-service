package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import app.swapr.user.cities.CityService;
import app.swapr.user.errors.FieldError;
import app.swapr.user.errors.ValidationException;
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
public class ProfileService {

  private static final int MAX_NAME_LENGTH = 50;
  private static final BigDecimal MIN_RADIUS = BigDecimal.ONE;
  private static final BigDecimal MAX_RADIUS = BigDecimal.valueOf(100);

  private final ProfileRepository repository;
  private final CityService cities;

  ProfileService(ProfileRepository repository, CityService cities) {
    this.repository = repository;
    this.cities = cities;
  }

  @Transactional
  ProfileView getOrCreate(UUID userId) {
    return loadOrCreate(userId).view();
  }

  @Transactional
  ProfileView update(UUID userId, UpdateProfileCommand command) {
    List<FieldError> errors = new ArrayList<>();
    String displayName = validName(command.displayName(), errors);
    City city = validCity(command.cityId(), errors);
    Integer radiusKm = validRadius(command.radiusKm(), errors);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    Profile profile = loadOrCreate(userId);
    profile.replace(displayName, city, radiusKm, Instant.now());
    return profile.view();
  }

  @Transactional(readOnly = true)
  Optional<PublicProfileView> publicProfile(UUID id) {
    return repository.findById(id)
        .map(Profile::view)
        .filter(profile -> profile.displayName() != null && profile.city() != null)
        .map(profile -> new PublicProfileView(profile.id(), profile.displayName(), profile.city()));
  }

  /** Hard-deletes the user's profile, doing nothing when none is stored. */
  @Transactional
  public void deleteAllFor(UUID userId) {
    repository.deleteProfile(userId);
  }

  private Profile loadOrCreate(UUID userId) {
    repository.insertDefaultIfAbsent(userId);
    return repository.findById(userId).orElseThrow();
  }

  private static String validName(String raw, List<FieldError> errors) {
    if (raw == null) {
      errors.add(new FieldError("displayName", "is required"));
      return null;
    }
    String name = raw.strip();
    int length = name.codePointCount(0, name.length());
    if (length < 1 || length > MAX_NAME_LENGTH) {
      errors.add(new FieldError("displayName", "must be 1 to 50 characters after trimming spaces"));
      return null;
    }
    return name;
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
      errors.add(new FieldError("radiusKm", "must be a whole number from 1 to 100"));
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
