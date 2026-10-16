package dio.budgeting.application;

public class AiProcessingException extends RuntimeException {
    private final String stage;

    public AiProcessingException(String stage, Throwable cause) {
        super("Falha no processamento de áudio: " + stage, cause);
        this.stage = stage;
    }

    public String stage() { return stage; }
}
