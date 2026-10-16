package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import java.time.LocalDate;
import org.springframework.ai.tool.annotation.ToolParam;

public record PersistTransactionInput(@ToolParam(description = "Descrição do gasto") String description,
                                      @ToolParam(description = "Valor do gasto (em centavos)") long amount,
                                      @ToolParam(description = "Categoria de uma transação") Category category,
                                      @ToolParam(description = "Data do gasto no formato AAAA-MM-DD; omitir usa a data atual no cadastro", required = false) LocalDate date) {
    public PersistTransactionInput(String description, long amount, Category category) {
        this(description, amount, category, null);
    }
}
