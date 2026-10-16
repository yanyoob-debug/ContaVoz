package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("demo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.openai.api-key=")
class DemoProfileTests {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void startsWithoutKeyOrAiModelsAndSeedsDemoData() throws Exception {
        assertThat(context.getBeansOfType(ChatModel.class)).isEmpty();
        assertThat(context.getBeansOfType(TranscriptionModel.class)).isEmpty();
        assertThat(context.getBeansOfType(TextToSpeechModel.class)).isEmpty();
        assertThat(get("/transactions").body()).contains("\"totalElements\":4");
        assertThat(get("/transactions/GROCERIES/summary").body()).contains("\"totalInCents\":21040");
        assertThat(get("/actuator/health").body()).contains("\"status\":\"UP\"");
    }

    @Test
    void servesApiContractAndDoesNotExposeVoiceEndpoint() throws Exception {
        var contract = get("/openapi.yaml");
        assertThat(contract.statusCode()).isEqualTo(200);
        assertThat(contract.body()).contains("openapi: 3.0.3", "/transactions:");
        assertThat(context.getBeansOfType(dio.budgeting.infrastructure.http.VoiceCommandController.class)).isEmpty();
        var home = get("/");
        assertThat(home.statusCode()).isEqualTo(200);
        assertThat(home.body()).contains("Budgeting Voice", "/app.js", "/styles.css");
        assertThat(get("/styles.css").statusCode()).isEqualTo(200);
        assertThat(get("/app.js").statusCode()).isEqualTo(200);
        assertThat(get("/money.mjs").statusCode()).isEqualTo(200);
        assertThat(get("/api/config").body()).contains("\"aiEnabled\":false", "\"demo\":true");
    }
}
