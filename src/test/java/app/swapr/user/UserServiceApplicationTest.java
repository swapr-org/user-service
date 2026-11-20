package app.swapr.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
    "INTERNAL_TOKEN=test-internal-token",
    "SUPABASE_URL=https://test-project.supabase.co",
    "CORS_ALLOWED_ORIGINS=https://qa.swapr.test"
})
@ActiveProfiles("qa")
@Testcontainers
class UserServiceApplicationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
      DockerImageName.parse(composeImage()).asCompatibleSubstituteFor("postgres"));

  @Autowired
  private HealthEndpoint health;

  @Test
  void reportsHealthyIncludingDatabase() {
    HealthDescriptor result = health.health();

    assertThat(result.getStatus()).isEqualTo(Status.UP);
    assertThat(((CompositeHealthDescriptor) result).getComponents())
        .containsKey("db")
        .extractingByKey("db")
        .extracting(HealthDescriptor::getStatus)
        .isEqualTo(Status.UP);
  }

  @Test
  void livenessIgnoresDatabase() {
    HealthDescriptor liveness = health.healthForPath("liveness");

    assertThat(liveness.getStatus()).isEqualTo(Status.UP);
    assertThat(((CompositeHealthDescriptor) liveness).getComponents()).doesNotContainKey("db");
  }

  private static String composeImage() {
    try {
      Matcher matcher = Pattern.compile("image:\\s*(\\S+)").matcher(Files.readString(Path.of("docker-compose.yml")));
      if (!matcher.find()) {
        throw new IllegalStateException("No image in docker-compose.yml");
      }
      return matcher.group(1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
