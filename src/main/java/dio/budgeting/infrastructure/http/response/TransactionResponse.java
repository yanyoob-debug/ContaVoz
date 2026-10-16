package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TransactionOutput;
import java.time.LocalDate;

public record TransactionResponse(String id, String category, String description, long amount, LocalDate date) {
    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(output.id(), output.category(), output.description(), output.value(), output.date());
    }
}
