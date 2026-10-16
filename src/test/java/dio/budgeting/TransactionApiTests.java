package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import dio.budgeting.infrastructure.persistence.repository.TransactionEntityRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiTests {
    @LocalServerPort int port;
    @Autowired TransactionEntityRepository repository;
    @MockitoBean TranscriptionModel transcriptionModel;
    @MockitoBean TextToSpeechModel textToSpeechModel;
    @MockitoBean ChatModel chatModel;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void cleanDatabase() { repository.deleteAll(); }

    private HttpResponse<String> request(String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void createsListsAndSummarizesOnlyRequestedCategory() throws Exception {
        assertThat(request("/transactions", """
                {"description":"Mercado", "amount":1250, "category":"GROCERIES"}
                """).statusCode()).isEqualTo(201);
        request("/transactions", """
                {"description":"Pão", "amount":500, "category":"GROCERIES"}
                """);
        request("/transactions", """
                {"description":"Remédio", "amount":900, "category":"PHARMA"}
                """);
        var list = request("/transactions/GROCERIES", null);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("Mercado", "Pão").doesNotContain("Remédio");
        var summary = request("/transactions/GROCERIES/summary", null);
        assertThat(summary.statusCode()).isEqualTo(200);
        assertThat(summary.body()).contains("\"count\":2", "\"totalInCents\":1750", "\"totalInReais\":17.50");
        assertThat(request("/transactions/AUTO/summary", null).body())
                .contains("\"count\":0", "\"totalInCents\":0", "\"totalInReais\":0.00");
    }

    @Test
    void rejectsInvalidTransactionsWithoutPersisting() throws Exception {
        for (var body : new String[]{
                "{\"description\":\" \",\"amount\":100,\"category\":\"AUTO\"}",
                "{\"description\":\"Gasolina\",\"amount\":0,\"category\":\"AUTO\"}",
                "{\"description\":\"Gasolina\",\"amount\":-100,\"category\":\"AUTO\"}",
                "{\"description\":\"Gasolina\",\"amount\":100}",
                "{\"description\":\"Gasolina\",\"amount\":100,\"category\":\"INVALID\"}"}) {
            var response = request("/transactions", body);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
        }
        assertThat(repository.count()).isZero();
        assertThat(request("/transactions/INVALID/summary", null).statusCode()).isEqualTo(400);
    }

    @Test
    void audioFlowsThroughTranscriptionChatAndSpeech() throws Exception {
        when(transcriptionModel.transcribe(any(org.springframework.core.io.Resource.class))).thenReturn("Quanto gastei no mercado?");
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Total: zero reais.")))));
        byte[] mp3 = new byte[]{73, 68, 51, 1, 2, 3};
        when(textToSpeechModel.call("Total: zero reais.")).thenReturn(mp3);
        var body = "--audio-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"comando.wav\"\r\nContent-Type: audio/wav\r\n\r\nexample-audio\r\n--audio-boundary--\r\n";
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/transactions/ai"))
                .header("Content-Type", "multipart/form-data; boundary=audio-boundary")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("audio/mpeg");
        assertThat(response.body()).isEqualTo(mp3);
        verify(transcriptionModel).transcribe(any(org.springframework.core.io.Resource.class));
        verify(chatModel).call(any(Prompt.class));
        verify(textToSpeechModel).call("Total: zero reais.");
    }

    @Test
    void paginatesAndFiltersWithoutRepeatingItems() throws Exception {
        request("/transactions", "{\"description\":\"Mercado A\",\"amount\":100,\"category\":\"GROCERIES\"}");
        request("/transactions", "{\"description\":\"Mercado B\",\"amount\":200,\"category\":\"GROCERIES\"}");
        request("/transactions", "{\"description\":\"Farmácia\",\"amount\":300,\"category\":\"PHARMA\"}");
        var first = request("/transactions?category=GROCERIES&page=0&size=1", null);
        var second = request("/transactions?category=GROCERIES&page=1&size=1", null);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("\"totalElements\":2", "\"totalPages\":2").doesNotContain("Farmácia");
        var idPattern = java.util.regex.Pattern.compile("\"id\":\"([^\"]+)\"");
        var firstId = idPattern.matcher(first.body());
        var secondId = idPattern.matcher(second.body());
        assertThat(firstId.find()).isTrue();
        assertThat(secondId.find()).isTrue();
        assertThat(firstId.group(1)).isNotEqualTo(secondId.group(1));
        assertThat(request("/transactions?page=2&size=1&category=GROCERIES", null).body()).contains("\"items\":[]");
        assertThat(request("/transactions", null).body()).contains("\"totalElements\":3");
    }

    @Test
    void rejectsInvalidPaginationAndFractionalMoney() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=101", "page=abc", "category=UNKNOWN")) {
            assertThat(request("/transactions?" + query, null).statusCode()).isEqualTo(400);
        }
        assertThat(request("/transactions", "{\"description\":\"Compra\",\"amount\":12.50,\"category\":\"AUTO\"}")
                .statusCode()).isEqualTo(400);
        assertThat(repository.count()).isZero();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = dio.budgeting.domain.Category.class, names = {"LEISURE", "UNEXPECTED"})
    void createsFiltersAndSummarizesNewCategories(dio.budgeting.domain.Category category) throws Exception {
        var created = request("/transactions", "{\"description\":\"Gasto de teste\",\"amount\":2590,\"category\":\"" + category.name() + "\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(request("/transactions?category=" + category.name(), null).body())
                .contains("\"totalElements\":1", "\"category\":\"" + category.name() + "\"");
        assertThat(request("/transactions/" + category.name() + "/summary", null).body())
                .contains("\"count\":1", "\"totalInCents\":2590", "\"totalInReais\":25.90");
        assertThat(request("/transactions/GROCERIES/summary", null).body()).contains("\"count\":0");
    }

    private HttpResponse<String> mutate(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void editsAndDeletesTransactionUpdatingCategoryTotals() throws Exception {
        var created = request("/transactions", "{\"description\":\"Compra\",\"amount\":100,\"category\":\"GROCERIES\"}");
        var id = java.util.regex.Pattern.compile("\"id\":\"([^\"]+)\"").matcher(created.body());
        assertThat(id.find()).isTrue();
        var path = "/transactions/" + id.group(1);
        var updated = mutate("PUT", path, "{\"description\":\"Cinema\",\"amount\":2590,\"category\":\"LEISURE\"}");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains(id.group(1), "Cinema", "\"amount\":2590");
        assertThat(repository.count()).isEqualTo(1);
        assertThat(request("/transactions/GROCERIES/summary", null).body()).contains("\"count\":0");
        assertThat(request("/transactions/LEISURE/summary", null).body()).contains("\"totalInCents\":2590");
        var deleted = mutate("DELETE", path, null);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(deleted.body()).isEmpty();
        assertThat(repository.count()).isZero();
        assertThat(request("/transactions/LEISURE/summary", null).body()).contains("\"count\":0");
        assertThat(mutate("DELETE", path, null).statusCode()).isEqualTo(404);
        assertThat(mutate("PUT", path, "{\"description\":\"Compra\",\"amount\":100,\"category\":\"AUTO\"}").statusCode()).isEqualTo(404);
    }

    @Test
    void invalidEditDoesNotChangeExistingTransaction() throws Exception {
        var entity = repository.save(new dio.budgeting.infrastructure.persistence.entity.TransactionEntity(
                java.util.UUID.randomUUID(), "Original", 100, dio.budgeting.domain.Category.AUTO));
        var path = "/transactions/" + entity.getId();
        assertThat(mutate("PUT", path, "{\"description\":\"Alterado\",\"amount\":0,\"category\":\"AUTO\"}").statusCode()).isEqualTo(400);
        var stored = repository.findById(entity.getId()).orElseThrow();
        assertThat(stored.getDescription()).isEqualTo("Original");
        assertThat(stored.getAmount()).isEqualTo(100);
        assertThat(mutate("DELETE", "/transactions/invalid-id", null).statusCode()).isEqualTo(400);
    }

    @Test
    void monthlyQueriesRespectMonthBoundariesAndCategories() throws Exception {
        request("/transactions", "{\"description\":\"Primeiro dia\",\"amount\":100,\"category\":\"LEISURE\",\"date\":\"2026-10-01\"}");
        request("/transactions", "{\"description\":\"Último dia\",\"amount\":200,\"category\":\"LEISURE\",\"date\":\"2026-10-31\"}");
        request("/transactions", "{\"description\":\"Mês anterior\",\"amount\":400,\"category\":\"LEISURE\",\"date\":\"2026-09-30\"}");
        request("/transactions", "{\"description\":\"Próximo mês\",\"amount\":800,\"category\":\"LEISURE\",\"date\":\"2026-11-01\"}");
        request("/transactions", "{\"description\":\"Outra categoria\",\"amount\":1600,\"category\":\"PHARMA\",\"date\":\"2026-10-15\"}");
        var summary = request("/transactions/LEISURE/summary?month=2026-10", null);
        assertThat(summary.statusCode()).isEqualTo(200);
        assertThat(summary.body()).contains("\"count\":2", "\"totalInCents\":300");
        var list = request("/transactions?category=LEISURE&month=2026-10&size=1", null);
        assertThat(list.body()).contains("\"totalElements\":2", "\"totalPages\":2", "Último dia", "\"date\":\"2026-10-31\"")
                .doesNotContain("Mês anterior", "Próximo mês", "Outra categoria");
        assertThat(request("/transactions?month=2026-10", null).body()).contains("\"totalElements\":3");
        assertThat(request("/transactions/LEISURE/summary?month=2026-12", null).body()).contains("\"count\":0");
    }

    @Test
    void changingDateMovesExpenseToAnotherMonth() throws Exception {
        var created = request("/transactions", "{\"description\":\"Cinema\",\"amount\":3000,\"category\":\"LEISURE\",\"date\":\"2026-10-01\"}");
        var id = java.util.regex.Pattern.compile("\"id\":\"([^\"]+)\"").matcher(created.body());
        assertThat(id.find()).isTrue();
        var edited = mutate("PUT", "/transactions/" + id.group(1), "{\"description\":\"Cinema\",\"amount\":3500,\"category\":\"LEISURE\",\"date\":\"2026-11-01\"}");
        assertThat(edited.statusCode()).isEqualTo(200);
        assertThat(edited.body()).contains("\"date\":\"2026-11-01\"");
        assertThat(request("/transactions/LEISURE/summary?month=2026-10", null).body()).contains("\"count\":0");
        assertThat(request("/transactions/LEISURE/summary?month=2026-11", null).body()).contains("\"totalInCents\":3500");
    }

    @Test
    void rejectsInvalidDatesAndMonthsAndAcceptsLeapDay() throws Exception {
        assertThat(request("/transactions", "{\"description\":\"Data inválida\",\"amount\":100,\"category\":\"AUTO\",\"date\":\"2026-02-30\"}").statusCode()).isEqualTo(400);
        assertThat(repository.count()).isZero();
        for (String month : List.of("2026-13", "2026-1", "invalid", "0000-01")) {
            assertThat(request("/transactions?month=" + month, null).statusCode()).isEqualTo(400);
            assertThat(request("/transactions/AUTO/summary?month=" + month, null).statusCode()).isEqualTo(400);
        }
        assertThat(request("/transactions", "{\"description\":\"Ano bissexto\",\"amount\":100,\"category\":\"AUTO\",\"date\":\"2024-02-29\"}").statusCode()).isEqualTo(201);
        assertThat(request("/transactions/AUTO/summary?month=2024-02", null).body()).contains("\"count\":1");
    }

    @Test
    void undatedLegacyRecordsRemainAccessibleAndCreationDefaultsToToday() throws Exception {
        repository.save(new dio.budgeting.infrastructure.persistence.entity.TransactionEntity(java.util.UUID.randomUUID(), "Legado", 100, dio.budgeting.domain.Category.AUTO));
        assertThat(request("/transactions?month=2026-10", null).body()).contains("\"totalElements\":0");
        assertThat(request("/transactions", null).body()).contains("Legado", "\"date\":null");
        var created = request("/transactions", "{\"description\":\"Hoje\",\"amount\":100,\"category\":\"AUTO\"}");
        var today = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        assertThat(created.body()).contains("\"date\":\"" + today + "\"");
    }

    private HttpResponse<String> sendAudio(String audio) throws Exception {
        String body = "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\nContent-Type: audio/wav\r\n\r\n"
                + audio + "\r\n--boundary--\r\n";
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/transactions/ai"))
                .header("Content-Type", "multipart/form-data; boundary=boundary")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void rejectsEmptyAudioWithoutCallingProvider() throws Exception {
        assertThat(sendAudio("").statusCode()).isEqualTo(400);
        verifyNoInteractions(transcriptionModel, chatModel, textToSpeechModel);
    }

    @Test
    void providerFailureReturnsStageWithoutLeakingDetails() throws Exception {
        when(transcriptionModel.transcribe(any(org.springframework.core.io.Resource.class)))
                .thenThrow(new RuntimeException("internal-provider-secret"));
        var response = sendAudio("example");
        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body()).contains("\"stage\":\"transcription\"").doesNotContain("internal-provider-secret");
        verifyNoInteractions(chatModel, textToSpeechModel);
    }

    @Test
    void speechFailureWarnsAboutPossiblePersistedTransaction() throws Exception {
        when(transcriptionModel.transcribe(any(org.springframework.core.io.Resource.class))).thenReturn("Consultar gastos");
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Total: zero reais.")))));
        when(textToSpeechModel.call("Total: zero reais.")).thenThrow(new RuntimeException("provider unavailable"));
        var response = sendAudio("example");
        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body()).contains("\"stage\":\"speech\"", "pode ter sido salvo");
    }
}
