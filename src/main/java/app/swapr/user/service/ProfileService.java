package app.swapr.user.service;

import app.swapr.user.dto.ProfileView;
import app.swapr.user.dto.PublicProfileView;
import app.swapr.user.dto.UpdateProfileRequest;
import app.swapr.user.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;

/** Creates, updates, looks up and deletes user profiles. */
public interface ProfileService {

  /** Returns the user's profile, creating it with the default radius on first use. */
  ProfileView getOrCreate(UUID userId);

  /** Validates the request and replaces the user's name, city and radius, creating the profile if needed. */
  ProfileView update(UUID userId, UpdateProfileRequest request) throws ValidationException;

  /** Returns the public view of a complete profile, or empty when the user is unknown or incomplete. */
  Optional<PublicProfileView> publicProfile(UUID id);

  /** Hard-deletes the user's profile, doing nothing when none is stored. */
  void delete(UUID userId);
}
