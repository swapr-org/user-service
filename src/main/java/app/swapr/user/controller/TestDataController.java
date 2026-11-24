package app.swapr.user.controller;

import app.swapr.user.service.ProfileService;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("qa")
class TestDataController {

  private final ProfileService profiles;

  TestDataController(ProfileService profiles) {
    this.profiles = profiles;
  }

  @DeleteMapping("/internal/v1/test-data/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteUser(@PathVariable UUID id) {
    profiles.deleteAllFor(id);
  }
}
