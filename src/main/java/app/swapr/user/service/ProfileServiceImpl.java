package app.swapr.user.service;

import app.swapr.user.dto.ProfileResponse;
import app.swapr.user.dto.PublicProfileResponse;
import app.swapr.user.dto.UpdateProfileRequest;
import app.swapr.user.exception.FieldError;
import app.swapr.user.exception.NotFoundException;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import app.swapr.user.model.Profile;
import app.swapr.user.repository.ProfileRepository;
import app.swapr.user.util.TrimmedText;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ProfileServiceImpl implements ProfileService {

  private static final int DEFAULT_RADIUS_KM = 25;
  private static final int MAX_NAME_LENGTH = 50;
  private static final BigDecimal MIN_RADIUS = BigDecimal.ONE;
  private static final BigDecimal MAX_RADIUS = BigDecimal.valueOf(100);

  private final ProfileRepository profileRepository;
  private final CityService cityService;

  ProfileServiceImpl(ProfileRepository profileRepository, CityService cityService) {
    this.profileRepository = profileRepository;
    this.cityService = cityService;
  }

  @Override
  public ProfileResponse getOrCreate(UUID userId) {
    return ProfileResponse.from(loadOrCreate(userId));
  }

  @Override
  public ProfileResponse update(UUID userId, UpdateProfileRequest request) throws ValidationException {
    List<FieldError> errors = new ArrayList<>();
    String displayName = validName(request.displayName(), errors);
    City city = validCity(request.cityId(), errors);
    Integer radiusKm = validRadius(request.radiusKm(), errors);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    Profile updated = loadOrCreate(userId).replaced(displayName, city, radiusKm, Instant.now());
    profileRepository.save(updated);
    return ProfileResponse.from(updated);
  }

  @Override
  @Transactional(readOnly = true)
  public PublicProfileResponse publicProfile(UUID id) throws NotFoundException {
    return profileRepository.findById(id)
        .filter(Profile::isComplete)
        .map(PublicProfileResponse::from)
        .orElseThrow(() -> new NotFoundException("Profile not found."));
  }

  @Override
  public void delete(UUID userId) {
    profileRepository.deleteProfile(userId);
  }

  private Profile loadOrCreate(UUID userId) {
    profileRepository.insertIfAbsent(userId, DEFAULT_RADIUS_KM);
    return profileRepository.findById(userId).orElseThrow();
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
    Optional<City> city = parseUuid(raw).flatMap(cityService::find);
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
