package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ProcessVoiceCommandUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/transactions")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class VoiceCommandController {
    private final ProcessVoiceCommandUseCase useCase;

    public VoiceCommandController(ProcessVoiceCommandUseCase useCase) { this.useCase = useCase; }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mpeg")
    public ResponseEntity<Resource> process(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Envie um arquivo de áudio não vazio.");
        var audio = useCase.execute(file.getResource());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("audio.mp3").build().toString())
                .body(new ByteArrayResource(audio));
    }
}
