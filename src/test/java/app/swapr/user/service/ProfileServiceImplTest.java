package app.swapr.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.swapr.user.dto.ProfileView;
import app.swapr.user.dto.UpdateProfileRequest;
import app.swapr.user.exception.FieldError;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import app.swapr.user.model.Profile;
import app.swapr.user.repository.ProfileRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final City ZURICH = new City(UUID.randomUUID(), "Zürich", "Zurich", "CH", 47.36667, 8.55);
  private static final City LONDON = new City(UUID.randomUUID(), "London", "England", "GB", 51.50853, -0.12574);

  @Mock
  private ProfileRepository repository;

  @Mock
  private CityService cities;

  private ProfileService profiles;

  @BeforeEach
  void setUp() {
    profiles = new ProfileServiceImpl(repository, cities);
  }

  @Test
  void getOrCreateInsertsDefaultRadiusBeforeReadingTheProfile() {
    when(repository.findById(USER_ID)).thenReturn(Optional.of(blank()));

    ProfileView profile = profiles.getOrCreate(USER_ID);

    InOrder order = inOrder(repository);
    order.verify(repository).insertIfAbsent(USER_ID, 25);
    order.verify(repository).findById(USER_ID);
    assertThat(profile).isEqualTo(new ProfileView(USER_ID, null, 25, null));
  }

  @Test
  void updateCreatesTheProfileIfNeededAndSavesTrimmedNameCityAndRadius() {
    when(cities.find(ZURICH.id())).thenReturn(Optional.of(ZURICH));
    when(repository.findById(USER_ID)).thenReturn(Optional.of(blank()));

    ProfileView updated = profiles.update(USER_ID, request("  Ben  ", ZURICH.id().toString(), "40"));

    verify(repository).insertIfAbsent(USER_ID, 25);
    assertThat(saved()).extracting(Profile::displayName, Profile::city, Profile::radiusKm)
        .containsExactly("Ben", ZURICH, 40);
    assertThat(updated).isEqualTo(new ProfileView(USER_ID, "Ben", 40, ZURICH));
  }

  @Test
  void updateReplacesEveryFieldOfAnExistingProfile() {
    when(cities.find(LONDON.id())).thenReturn(Optional.of(LONDON));
    when(repository.findById(USER_ID)).thenReturn(Optional.of(complete()));

    profiles.update(USER_ID, request("Benjamin", LONDON.id().toString(), "5"));

    assertThat(saved()).extracting(Profile::id, Profile::displayName, Profile::city, Profile::radiusKm)
        .containsExactly(USER_ID, "Benjamin", LONDON, 5);
  }

  @Test
  void acceptsBoundaryNameLengthAndRadii() {
    when(cities.find(ZURICH.id())).thenReturn(Optional.of(ZURICH));
    when(repository.findById(USER_ID)).thenReturn(Optional.of(blank()));
    String fiftyChars = "a".repeat(50);

    assertThat(profiles.update(USER_ID, request(" " + fiftyChars + " ", ZURICH.id().toString(), "1")).displayName())
        .isEqualTo(fiftyChars);
    assertThat(profiles.update(USER_ID, request("B", ZURICH.id().toString(), "100")).radiusKm()).isEqualTo(100);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
  void rejectsMissingBlankOrTooLongDisplayName(String displayName) {
    when(cities.find(ZURICH.id())).thenReturn(Optional.of(ZURICH));

    assertOnlyFieldRejected(request(displayName, ZURICH.id().toString(), "25"), "displayName");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "101", "2.5", "-1"})
  void rejectsRadiusThatIsNotWholeOrOutsideOneToHundred(String radiusKm) {
    when(cities.find(ZURICH.id())).thenReturn(Optional.of(ZURICH));

    assertOnlyFieldRejected(request("Ben", ZURICH.id().toString(), radiusKm), "radiusKm");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000000"})
  void rejectsMissingMalformedOrUnknownCity(String cityId) {
    assertOnlyFieldRejected(request("Ben", cityId, "25"), "cityId");
  }

  @Test
  void reportsEveryInvalidFieldTogetherWithoutTouchingStoredProfiles() {
    assertThatThrownBy(() -> profiles.update(USER_ID, request(" ", null, "0")))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactlyInAnyOrder("displayName", "cityId", "radiusKm"));
    verifyNoInteractions(repository);
  }

  @Test
  void publicProfileShowsNameAndCityOfCompleteProfile() {
    when(repository.findById(USER_ID)).thenReturn(Optional.of(complete()));

    assertThat(profiles.publicProfile(USER_ID)).hasValueSatisfying(profile -> {
      assertThat(profile.id()).isEqualTo(USER_ID);
      assertThat(profile.displayName()).isEqualTo("Ben");
      assertThat(profile.city()).isEqualTo(ZURICH);
    });
  }

  @Test
  void publicProfileIsEmptyForUnknownOrIncompleteProfile() {
    UUID unknown = UUID.randomUUID();
    when(repository.findById(USER_ID)).thenReturn(Optional.of(blank()));
    when(repository.findById(unknown)).thenReturn(Optional.empty());

    assertThat(profiles.publicProfile(USER_ID)).isEmpty();
    assertThat(profiles.publicProfile(unknown)).isEmpty();
  }

  @Test
  void deleteRemovesTheUsersProfile() {
    profiles.delete(USER_ID);

    verify(repository).deleteProfile(USER_ID);
  }

  private void assertOnlyFieldRejected(UpdateProfileRequest request, String field) {
    assertThatThrownBy(() -> profiles.update(USER_ID, request))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactly(field));
    verifyNoInteractions(repository);
  }

  private Profile saved() {
    ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  private static Profile blank() {
    return new Profile(USER_ID, null, null, 25, null, null);
  }

  private static Profile complete() {
    return new Profile(USER_ID, "Ben", ZURICH, 40, null, null);
  }

  private static UpdateProfileRequest request(String displayName, String cityId, String radiusKm) {
    return new UpdateProfileRequest(displayName, cityId, radiusKm == null ? null : new BigDecimal(radiusKm));
  }
}
