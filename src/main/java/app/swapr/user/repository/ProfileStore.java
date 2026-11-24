package app.swapr.user.repository;

import app.swapr.user.model.Profile;
import java.util.Optional;
import java.util.UUID;

/** The stored profiles that the profile service reads and writes. */
public interface ProfileStore {

  /** Inserts a profile with only an ID and radius, doing nothing when one already exists. */
  void insertIfAbsent(UUID id, int radiusKm);

  /** Returns the profile with the given ID, if it exists. */
  Optional<Profile> find(UUID id);

  /** Stores the profile, replacing the one with the same ID. */
  void save(Profile profile);

  /** Deletes the profile with the given ID, doing nothing when none exists. */
  void deleteById(UUID id);
}
