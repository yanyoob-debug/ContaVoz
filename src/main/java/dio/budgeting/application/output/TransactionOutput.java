package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;
import java.time.LocalDate;


public record TransactionOutput(String id, String description, String category, long value, LocalDate date) {
    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                transaction.getAmount(), transaction.getDate());
    }
}
