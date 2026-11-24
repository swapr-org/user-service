package app.swapr.user.controller;

import app.swapr.user.dto.ProfileResponse;
import app.swapr.user.dto.PublicProfileResponse;
import app.swapr.user.dto.UpdateProfileRequest;
import app.swapr.user.exception.NotFoundException;
import app.swapr.user.service.ProfileService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
class ProfileController {

  private final ProfileService profiles;

  ProfileController(ProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping("/me")
  ProfileResponse me(Principal principal) {
    return ProfileResponse.from(profiles.getOrCreate(userId(principal)));
  }

  @PutMapping("/me")
  ProfileResponse replaceMe(Principal principal, @RequestBody UpdateProfileRequest request) {
    return ProfileResponse.from(profiles.update(userId(principal), request));
  }

  @GetMapping("/{id}")
  PublicProfileResponse publicProfile(@PathVariable UUID id) {
    return profiles.publicProfile(id)
        .map(PublicProfileResponse::from)
        .orElseThrow(() -> new NotFoundException("Profile not found."));
  }

  private static UUID userId(Principal principal) {
    return UUID.fromString(principal.getName());
  }
}
