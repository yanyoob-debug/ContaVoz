package dio.budgeting;

import dio.budgeting.application.SummarizeCategoryUseCase;
import dio.budgeting.domain.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class CategorySummaryTests {
    @Test
    void sumsBeyondLongRangeWithoutOverflow() {
        var repository = mock(TransactionRepository.class);
        when(repository.findAllByCategory(Category.AUTO)).thenReturn(List.of(
                new Transaction("Gasto A", Long.MAX_VALUE, Category.AUTO),
                new Transaction("Gasto B", Long.MAX_VALUE, Category.AUTO)));
        var result = new SummarizeCategoryUseCase(repository).execute(Category.AUTO);
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.totalInCents()).isEqualByComparingTo(new BigDecimal("18446744073709551614"));
        assertThat(result.totalInReais()).isEqualByComparingTo(new BigDecimal("184467440737095516.14"));
    }
}
