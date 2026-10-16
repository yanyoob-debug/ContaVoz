package dio.budgeting;

import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionValidationTests {
    @Test
    void toolUseCaseCannotBypassDomainValidation() {
        var repository = mock(TransactionRepository.class);
        var useCase = new PersistTransactionUseCase(repository);
        assertThatThrownBy(() -> useCase.execute(new PersistTransactionInput("Compra", -1, Category.AUTO)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void enforcesDescriptionAndCategoryAndTrimsWhitespace() {
        assertThatThrownBy(() -> new Transaction(null, 1, Category.AUTO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Transaction("x".repeat(256), 1, Category.AUTO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Transaction("Compra", 1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Transaction("  Compra  ", 1, Category.AUTO).getDescription()).isEqualTo("Compra");
    }
}
