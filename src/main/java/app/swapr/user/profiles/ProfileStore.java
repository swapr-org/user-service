package app.swapr.user.profiles;

import java.util.Optional;
import java.util.UUID;

interface ProfileStore {

  void insertIfAbsent(UUID id, int radiusKm);

  Optional<Profile> find(UUID id);

  void save(Profile profile);

  void deleteById(UUID id);
}
