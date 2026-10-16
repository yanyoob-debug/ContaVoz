package dio.budgeting.domain;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(TransactionId id);
    void deleteById(TransactionId id);

    List<Transaction> findAllByCategory(Category category);
    TransactionPage findPage(Category category, int page, int size);
    TransactionPage findPage(Category category, int page, int size, LocalDate start, LocalDate end);
    List<Transaction> findByCategoryAndPeriod(Category category, LocalDate start, LocalDate end);
}
