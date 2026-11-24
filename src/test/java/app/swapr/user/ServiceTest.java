package app.swapr.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Starts the application against a PostGIS container with the real migrations. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
    "INTERNAL_TOKEN=test-internal-token",
    "SUPABASE_URL=https://test-project.supabase.co",
    "CORS_ALLOWED_ORIGINS=https://qa.swapr.test"
})
@ActiveProfiles("qa")
@Import(PostgisContainerConfig.class)
public @interface ServiceTest {
}
