package app.swapr.user.repository;

import app.swapr.user.model.City;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaCityDirectory implements CityDirectory {

  private final CityRepository repository;

  JpaCityDirectory(CityRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<City> searchByPrefix(String prefix, String foldedPrefix) {
    return repository.searchByPrefix(prefix, foldedPrefix);
  }

  @Override
  public Optional<City> find(UUID id) {
    return repository.findById(id);
  }
}
