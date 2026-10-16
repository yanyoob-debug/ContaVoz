package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;

import java.util.List;
import java.util.UUID;

public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findAllByCategory(Category category);
    Page<TransactionEntity> findAllByCategory(Category category, Pageable pageable);

    @Query("select t from TransactionEntity t where (:category is null or t.category = :category) and t.date >= :start and t.date < :end")
    Page<TransactionEntity> findByPeriod(Category category, LocalDate start, LocalDate end, Pageable pageable);

    @Query("select t from TransactionEntity t where t.category = :category and t.date >= :start and t.date < :end")
    List<TransactionEntity> findByCategoryAndPeriod(Category category, LocalDate start, LocalDate end);
}
