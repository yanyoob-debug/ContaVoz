package dio.budgeting.infrastructure.http;

import dio.budgeting.application.AiProcessingException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTests {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void identifiesNestedProviderFailureWithoutExposingSecrets() {
        var problem = handler.aiFailure(new AiProcessingException("transcription",
                new RuntimeException("wrapper", new RuntimeException("{\"code\":\"invalid_api_key\",\"key\":\"sk-secret\"}"))));
        assertThat(problem.getProperties()).containsEntry("reason", "invalid_api_key");
        assertThat(problem.getDetail()).contains("chave", "Nenhum gasto foi criado").doesNotContain("sk-secret");
    }

    @Test
    void distinguishesQuotaFromRateLimitAndRetainsPersistenceWarningAfterInterpretation() {
        for (String code : new String[]{"insufficient_quota", "rate_limit_exceeded", "model_not_found", "invalid_audio"}) {
            var problem = handler.aiFailure(new AiProcessingException("interpretation", new RuntimeException(code)));
            assertThat(problem.getProperties()).containsEntry("reason", code);
            assertThat(problem.getDetail()).contains("pode ter sido salvo");
        }
        var unknown = handler.aiFailure(new AiProcessingException("speech", new RuntimeException("private-provider-data")));
        assertThat(unknown.getDetail()).contains("resposta em voz", "pode ter sido salvo").doesNotContain("private-provider-data");
    }
}
