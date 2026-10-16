package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
public class SummarizeCategoryUseCase {
    private final TransactionRepository repository;

    public SummarizeCategoryUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Tool(name = "summarize-category", description = "Consulta quantidade e total de gastos de uma categoria em TODOS os períodos, em centavos e reais")
    public Summary execute(@ToolParam(description = "Categoria dos gastos") Category category) {
        return execute(category, null);
    }

    @Tool(name = "summarize-category-by-month", description = "Consulta quantidade e total dos gastos de uma categoria em um mês específico")
    public Summary byMonth(@ToolParam(description = "Categoria dos gastos") Category category,
                           @ToolParam(description = "Mês no formato AAAA-MM, por exemplo 2026-10") String month) {
        return execute(category, YearMonth.parse(month));
    }

    public Summary execute(Category category, YearMonth month) {
        if (category == null) throw new IllegalArgumentException("Categoria obrigatória.");
        var transactions = month == null ? repository.findAllByCategory(category)
                : repository.findByCategoryAndPeriod(category, month.atDay(1), month.plusMonths(1).atDay(1));
        // BigDecimal evita overflow ao somar múltiplos valores long.
        var total = transactions.stream()
                .map(transaction -> BigDecimal.valueOf(transaction.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(category, transactions.size(), total, total.movePointLeft(2));
    }

    public record Summary(Category category, long count, BigDecimal totalInCents, BigDecimal totalInReais) {}
}
