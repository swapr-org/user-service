package app.swapr.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class PostgisContainerConfig {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(DockerImageName.parse(composeImage()).asCompatibleSubstituteFor("postgres"));
  }

  static String composeImage() {
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
