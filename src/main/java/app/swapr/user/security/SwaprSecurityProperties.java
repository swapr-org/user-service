package app.swapr.user.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("swapr")
record SwaprSecurityProperties(String internalToken, List<String> corsAllowedOrigins) {

  SwaprSecurityProperties {
    corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
  }
}
