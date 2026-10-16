package dio.budgeting.application;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class ProcessVoiceCommandUseCase {
    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel speechModel;

    public ProcessVoiceCommandUseCase(TranscriptionModel transcriptionModel, ChatClient.Builder builder,
            TextToSpeechModel speechModel, PersistTransactionUseCase persist, ListTransactionsByCategoryUseCase list,
            SummarizeCategoryUseCase summarize, @Value("classpath:prompts/system-message.st") Resource prompt) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.speechModel = speechModel;
        this.chatClient = builder.defaultSystem(prompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(persist, list, summarize).build();
    }

    public byte[] execute(Resource audio) {
        String text = stage("transcription", () -> transcriptionModel.transcribe(audio));
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Nenhum comando foi identificado no áudio.");
        String response = stage("interpretation", () -> chatClient.prompt()
                .user("Data atual em America/Sao_Paulo: " + LocalDate.now(ZoneId.of("America/Sao_Paulo")) + "\nComando: " + text)
                .call().content());
        if (response == null || response.isBlank()) throw new AiProcessingException("interpretation", null);
        byte[] result = stage("speech", () -> speechModel.call(response));
        if (result == null || result.length == 0) throw new AiProcessingException("speech", null);
        return result;
    }

    private <T> T stage(String name, Supplier<T> operation) {
        try { return operation.get(); }
        catch (RuntimeException exception) { throw new AiProcessingException(name, exception); }
    }
}
