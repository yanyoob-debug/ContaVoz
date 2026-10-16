package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageTransactionUseCase {
    private final TransactionRepository repository;

    public ManageTransactionUseCase(TransactionRepository repository) { this.repository = repository; }

    @Transactional
    public TransactionOutput update(TransactionId id, PersistTransactionInput input) {
        var existing = repository.findById(id).orElseThrow(TransactionNotFoundException::new);
        var updated = new Transaction(id, input.description(), input.amount(), input.category(),
                input.date() == null ? existing.getDate() : input.date());
        return TransactionOutput.from(repository.save(updated));
    }

    @Transactional
    public void delete(TransactionId id) {
        repository.findById(id).orElseThrow(TransactionNotFoundException::new);
        repository.deleteById(id);
    }
}
