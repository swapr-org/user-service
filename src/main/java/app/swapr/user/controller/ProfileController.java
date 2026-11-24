package app.swapr.user.controller;

import app.swapr.user.dto.ProfileResponse;
import app.swapr.user.dto.PublicProfileResponse;
import app.swapr.user.dto.UpdateProfileRequest;
import app.swapr.user.exception.NotFoundException;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.service.ProfileService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
class ProfileController {

  private final ProfileService profileService;

  ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping("/me")
  ProfileResponse me(Principal principal) {
    return ProfileResponse.from(profileService.getOrCreate(userId(principal)));
  }

  @PutMapping("/me")
  ProfileResponse replaceMe(Principal principal, @RequestBody UpdateProfileRequest request)
      throws ValidationException {
    return ProfileResponse.from(profileService.update(userId(principal), request));
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteMe(Principal principal) {
    profileService.delete(userId(principal));
  }

  @GetMapping("/{id}")
  PublicProfileResponse publicProfile(@PathVariable UUID id) throws NotFoundException {
    return profileService.publicProfile(id)
        .map(PublicProfileResponse::from)
        .orElseThrow(() -> new NotFoundException("Profile not found."));
  }

  private static UUID userId(Principal principal) {
    return UUID.fromString(principal.getName());
  }
}
