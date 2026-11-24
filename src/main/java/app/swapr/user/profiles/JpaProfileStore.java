package app.swapr.user.profiles;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaProfileStore implements ProfileStore {

  private final ProfileRepository repository;

  JpaProfileStore(ProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public void insertIfAbsent(UUID id, int radiusKm) {
    repository.insertIfAbsent(id, radiusKm);
  }

  @Override
  public Optional<Profile> find(UUID id) {
    return repository.findById(id);
  }

  @Override
  public void save(Profile profile) {
    repository.save(profile);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteProfile(id);
  }
}
