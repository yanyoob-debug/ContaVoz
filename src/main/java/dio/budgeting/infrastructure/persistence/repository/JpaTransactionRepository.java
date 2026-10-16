package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionPage;
import dio.budgeting.domain.TransactionId;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(TransactionEntityRepository transactionEntityRepository) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        var entity = TransactionEntity.from(transaction);
        return transactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return transactionEntityRepository.findById(id.uuid()).map(TransactionEntity::toDomain);
    }

    @Override
    public void deleteById(TransactionId id) {
        transactionEntityRepository.deleteById(id.uuid());
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactionEntityRepository.findAllByCategory(category)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public TransactionPage findPage(Category category, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var result = category == null ? transactionEntityRepository.findAll(pageable)
                : transactionEntityRepository.findAllByCategory(category, pageable);
        return new TransactionPage(result.getContent().stream().map(TransactionEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public TransactionPage findPage(Category category, int page, int size, LocalDate start, LocalDate end) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("date"), Sort.Order.asc("id")));
        var result = transactionEntityRepository.findByPeriod(category, start, end, pageable);
        return new TransactionPage(result.getContent().stream().map(TransactionEntity::toDomain).toList(), result.getTotalElements());
    }

    @Override
    public List<Transaction> findByCategoryAndPeriod(Category category, LocalDate start, LocalDate end) {
        return transactionEntityRepository.findByCategoryAndPeriod(category, start, end).stream().map(TransactionEntity::toDomain).toList();
    }
}
