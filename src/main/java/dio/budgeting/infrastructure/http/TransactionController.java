package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeCategoryUseCase;
import dio.budgeting.application.ManageTransactionUseCase;
import dio.budgeting.domain.TransactionId;
import java.util.UUID;
import java.time.YearMonth;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final PersistTransactionUseCase persist;
    private final ListTransactionsByCategoryUseCase listByCategory;
    private final SummarizeCategoryUseCase summarize;
    private final ListTransactionsUseCase list;
    private final ManageTransactionUseCase manage;

    public TransactionController(PersistTransactionUseCase persist, ListTransactionsByCategoryUseCase listByCategory,
            SummarizeCategoryUseCase summarize, ListTransactionsUseCase list, ManageTransactionUseCase manage) {
        this.persist = persist;
        this.listByCategory = listByCategory;
        this.summarize = summarize;
        this.list = list;
        this.manage = manage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        return TransactionResponse.from(persist.execute(request.toInput()));
    }

    @GetMapping
    public PageResponse listTransactions(@RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String month) {
        var result = list.execute(category, page, size, parseMonth(month));
        return new PageResponse(result.items().stream().map(TransactionResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(@PathVariable UUID id, @RequestBody TransactionRequest request) {
        return TransactionResponse.from(manage.update(new TransactionId(id), request.toInput()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable UUID id) {
        manage.delete(new TransactionId(id));
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listByCategory.execute(category).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/{category}/summary")
    public SummarizeCategoryUseCase.Summary summarize(@PathVariable Category category, @RequestParam(required = false) String month) {
        return summarize.execute(category, parseMonth(month));
    }

    private YearMonth parseMonth(String month) {
        if (month == null) return null;
        if (!month.matches("[0-9]{4}-(0[1-9]|1[0-2])") || month.startsWith("0000")) {
            throw new IllegalArgumentException("Informe o mês no formato AAAA-MM, por exemplo 2026-10.");
        }
        return YearMonth.parse(month);
    }

    public record PageResponse(List<TransactionResponse> items, int page, int size, long totalElements, long totalPages) {}
}
