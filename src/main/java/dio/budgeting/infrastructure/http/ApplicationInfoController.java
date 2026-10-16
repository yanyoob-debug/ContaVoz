package dio.budgeting.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationInfoController {
    private final boolean aiEnabled;
    private final boolean demo;

    public ApplicationInfoController(@Value("${app.ai.enabled:true}") boolean aiEnabled, Environment environment) {
        this.aiEnabled = aiEnabled;
        this.demo = environment.acceptsProfiles(Profiles.of("demo"));
    }

    @GetMapping("/api/config")
    public Info info() { return new Info(aiEnabled, demo); }

    public record Info(boolean aiEnabled, boolean demo) {}
}
