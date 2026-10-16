package dio.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import dio.budgeting.application.AiProcessingException;
import dio.budgeting.application.TransactionNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TransactionNotFoundException.class)
    ProblemDetail transactionNotFound(TransactionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
    @ExceptionHandler(AiProcessingException.class)
    ProblemDetail aiFailure(AiProcessingException exception) {
        String reason = aiReason(exception);
        String explanation = switch (reason) {
            case "invalid_api_key" -> "A OpenAI recusou a chave da API. Confira a chave usada ao iniciar o teste.";
            case "insufficient_quota" -> "A OpenAI informou falta de cota ou limite de uso atingido. Confira o saldo e os limites do projeto na plataforma da API.";
            case "rate_limit_exceeded" -> "A OpenAI informou excesso de solicitações. Aguarde antes de tentar outro comando.";
            case "model_not_found" -> "O modelo de IA configurado não está disponível para este projeto. Confira o acesso aos modelos.";
            case "invalid_audio" -> "A OpenAI não conseguiu ler o arquivo de áudio. Grave novamente ou escolha um arquivo válido.";
            default -> "Não foi possível concluir o comando com IA.";
        };
        String step = switch (exception.stage()) {
            case "transcription" -> "Falha ao transformar o áudio em texto. ";
            case "interpretation" -> "Falha ao interpretar ou executar o comando. ";
            case "speech" -> "Falha ao gerar a resposta em voz. ";
            default -> "";
        };
        String saved = "transcription".equals(exception.stage())
                ? " Nenhum gasto foi criado por este envio."
                : " Consulte as transações antes de reenviar: um gasto pode ter sido salvo.";
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                step + explanation + saved);
        problem.setTitle("Falha no processamento com IA");
        problem.setProperty("stage", exception.stage());
        problem.setProperty("reason", reason);
        // Only controlled codes are logged: provider messages may contain credentials or user data.
        log.warn("Falha de IA: etapa={}, motivo={}", exception.stage(), reason);
        return problem;
    }

    private String aiReason(Throwable failure) {
        for (int depth = 0; failure != null && depth < 16; depth++, failure = failure.getCause()) {
            String message = failure.getMessage();
            if (message == null) continue;
            for (String code : new String[]{"invalid_api_key", "insufficient_quota", "rate_limit_exceeded", "model_not_found"}) {
                if (message.contains(code)) return code;
            }
            if (message.contains("unsupported_format") || message.contains("invalid_audio")) return "invalid_audio";
        }
        return "unknown";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidValue(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformedRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Requisição inválida. Use amount inteiro em centavos, date no formato AAAA-MM-DD com data válida e categoria GROCERIES, PHARMA, AUTO, LEISURE ou UNEXPECTED.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail oversizedAudio(MaxUploadSizeExceededException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite de 10 MB.");
    }
}
