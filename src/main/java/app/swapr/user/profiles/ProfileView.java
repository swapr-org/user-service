package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import java.util.UUID;

record ProfileView(UUID id, String displayName, int radiusKm, City city) {
}
