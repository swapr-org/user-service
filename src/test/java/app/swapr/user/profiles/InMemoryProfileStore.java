package app.swapr.user.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryProfileStore implements ProfileStore {

  private final Map<UUID, Profile> profiles = new HashMap<>();

  @Override
  public void insertIfAbsent(UUID id, int radiusKm) {
    profiles.putIfAbsent(id, new Profile(id, null, null, radiusKm, null, null));
  }

  @Override
  public Optional<Profile> find(UUID id) {
    return Optional.ofNullable(profiles.get(id));
  }

  @Override
  public void save(Profile profile) {
    profiles.put(profile.view().id(), profile);
  }

  @Override
  public void deleteById(UUID id) {
    profiles.remove(id);
  }
}
