package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import java.util.UUID;

record PublicProfileView(UUID id, String displayName, City city) {
}
