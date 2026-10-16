package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.YearMonth;

@Service
public class ListTransactionsUseCase {
    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public Result execute(Category category, int page, int size) {
        return execute(category, page, size, null);
    }

    public Result execute(Category category, int page, int size, YearMonth month) {
        if (page < 0) throw new IllegalArgumentException("Página deve ser maior ou igual a zero.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Tamanho da página deve estar entre 1 e 100.");
        var result = month == null ? repository.findPage(category, page, size)
                : repository.findPage(category, page, size, month.atDay(1), month.plusMonths(1).atDay(1));
        long totalPages = result.totalElements() / size + (result.totalElements() % size == 0 ? 0 : 1);
        return new Result(result.items().stream().map(TransactionOutput::from).toList(),
                page, size, result.totalElements(), totalPages);
    }

    public record Result(List<TransactionOutput> items, int page, int size, long totalElements, long totalPages) {}
}
