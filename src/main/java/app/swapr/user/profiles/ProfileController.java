package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import app.swapr.user.errors.NotFoundException;
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
  OwnProfileResponse me(Principal principal) {
    return OwnProfileResponse.of(profiles.getOrCreate(userId(principal)));
  }

  @PutMapping("/me")
  OwnProfileResponse replaceMe(Principal principal, @RequestBody UpdateProfileCommand command) {
    return OwnProfileResponse.of(profiles.update(userId(principal), command));
  }

  @GetMapping("/{id}")
  PublicProfileResponse publicProfile(@PathVariable UUID id) {
    return profiles.publicProfile(id)
        .map(PublicProfileResponse::of)
        .orElseThrow(() -> new NotFoundException("Profile not found."));
  }

  private static UUID userId(Principal principal) {
    return UUID.fromString(principal.getName());
  }

  record OwnProfileResponse(UUID id, String displayName, int radiusKm, OwnCity city) {

    static OwnProfileResponse of(ProfileView profile) {
      City city = profile.city();
      OwnCity ownCity = city == null ? null : new OwnCity(city.id(), city.name(), city.region(), city.countryCode());
      return new OwnProfileResponse(profile.id(), profile.displayName(), profile.radiusKm(), ownCity);
    }
  }

  record OwnCity(UUID id, String name, String region, String countryCode) {
  }

  record PublicProfileResponse(UUID id, String displayName, PublicCity city) {

    static PublicProfileResponse of(PublicProfileView profile) {
      City city = profile.city();
      return new PublicProfileResponse(profile.id(), profile.displayName(),
          new PublicCity(city.name(), city.region(), city.countryCode()));
    }
  }

  record PublicCity(String name, String region, String countryCode) {
  }
}
