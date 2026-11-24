package app.swapr.user.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.swapr.user.cities.City;
import app.swapr.user.cities.CityService;
import app.swapr.user.cities.InMemoryCityDirectory;
import app.swapr.user.errors.FieldError;
import app.swapr.user.errors.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileServiceTest {

  private static final String ZURICH_ID = "cf5f88ac-7848-5936-bd5a-b7ef809546ad";
  private static final String LONDON_ID = "d2f34301-524e-5a2f-ade9-fd7aebdb71ac";

  private final ProfileService profiles = new ProfileService(new InMemoryProfileStore(),
      new CityService(new InMemoryCityDirectory()
          .add(new City(UUID.fromString(ZURICH_ID), "Zürich", "Zurich", "CH", 47.36667, 8.55), "Zuerich", "zurich")
          .add(new City(UUID.fromString(LONDON_ID), "London", "England", "GB", 51.50853, -0.12574), "London",
              "london")));

  @Test
  void firstUseCreatesProfileWithDefaultRadiusAndNoNameOrCity() {
    UUID userId = UUID.randomUUID();

    ProfileView profile = profiles.getOrCreate(userId);

    assertThat(profile.id()).isEqualTo(userId);
    assertThat(profile.radiusKm()).isEqualTo(25);
    assertThat(profile.displayName()).isNull();
    assertThat(profile.city()).isNull();
  }

  @Test
  void updateBeforeFirstUseStoresTrimmedNameChosenCityAndRadius() {
    UUID userId = UUID.randomUUID();

    ProfileView updated = profiles.update(userId, command("  Ben  ", ZURICH_ID, "40"));

    ProfileView stored = profiles.getOrCreate(userId);
    for (ProfileView profile : List.of(updated, stored)) {
      assertThat(profile.id()).isEqualTo(userId);
      assertThat(profile.displayName()).isEqualTo("Ben");
      assertThat(profile.radiusKm()).isEqualTo(40);
      assertThat(profile.city().name()).isEqualTo("Zürich");
      assertThat(profile.city().latitude()).isEqualTo(47.36667);
      assertThat(profile.city().longitude()).isEqualTo(8.55);
    }
  }

  @Test
  void laterUpdateReplacesEveryField() {
    UUID userId = UUID.randomUUID();
    profiles.update(userId, command("Ben", ZURICH_ID, "40"));

    ProfileView updated = profiles.update(userId, command("Benjamin", LONDON_ID, "5"));

    assertThat(updated.displayName()).isEqualTo("Benjamin");
    assertThat(updated.city().name()).isEqualTo("London");
    assertThat(updated.radiusKm()).isEqualTo(5);
  }

  @Test
  void acceptsBoundaryNameLengthAndRadii() {
    UUID userId = UUID.randomUUID();
    String fiftyChars = "a".repeat(50);

    assertThat(profiles.update(userId, command(" " + fiftyChars + " ", ZURICH_ID, "1")).displayName())
        .isEqualTo(fiftyChars);
    assertThat(profiles.update(userId, command("B", ZURICH_ID, "100")).radiusKm()).isEqualTo(100);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
  void rejectsMissingBlankOrTooLongDisplayName(String displayName) {
    assertOnlyFieldRejected(command(displayName, ZURICH_ID, "25"), "displayName");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "101", "2.5", "-1"})
  void rejectsRadiusThatIsNotWholeOrOutsideOneToHundred(String radiusKm) {
    assertOnlyFieldRejected(command("Ben", ZURICH_ID, radiusKm), "radiusKm");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000000"})
  void rejectsMissingOrUnknownCity(String cityId) {
    assertOnlyFieldRejected(command("Ben", cityId, "25"), "cityId");
  }

  @Test
  void reportsEveryInvalidFieldTogetherAndLeavesProfileUnchanged() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> profiles.update(userId, command(" ", null, "0")))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactlyInAnyOrder("displayName", "cityId", "radiusKm"));
    assertThat(profiles.getOrCreate(userId).displayName()).isNull();
  }

  @Test
  void publicProfileShowsNameAndCityOfCompleteProfile() {
    UUID userId = UUID.randomUUID();
    profiles.update(userId, command("Ben", ZURICH_ID, "40"));

    assertThat(profiles.publicProfile(userId)).hasValueSatisfying(profile -> {
      assertThat(profile.id()).isEqualTo(userId);
      assertThat(profile.displayName()).isEqualTo("Ben");
      assertThat(profile.city().name()).isEqualTo("Zürich");
    });
  }

  @Test
  void publicProfileIsEmptyForUnknownOrIncompleteProfile() {
    UUID incomplete = UUID.randomUUID();
    profiles.getOrCreate(incomplete);

    assertThat(profiles.publicProfile(incomplete)).isEmpty();
    assertThat(profiles.publicProfile(UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteAllForRemovesTheProfileAndSucceedsWhenNothingIsStored() {
    UUID userId = UUID.randomUUID();
    profiles.update(userId, command("Ben", ZURICH_ID, "40"));

    profiles.deleteAllFor(userId);
    profiles.deleteAllFor(userId);

    assertThat(profiles.publicProfile(userId)).isEmpty();
    assertThat(profiles.getOrCreate(userId).displayName()).isNull();
  }

  private void assertOnlyFieldRejected(UpdateProfileCommand command, String field) {
    assertThatThrownBy(() -> profiles.update(UUID.randomUUID(), command))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactly(field));
  }

  private static UpdateProfileCommand command(String displayName, String cityId, String radiusKm) {
    return new UpdateProfileCommand(displayName, cityId, radiusKm == null ? null : new BigDecimal(radiusKm));
  }
}
