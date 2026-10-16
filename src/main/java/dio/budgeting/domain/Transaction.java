package dio.budgeting.domain;

import lombok.Getter;
import java.time.LocalDate;
import java.time.ZoneId;

@Getter
public class Transaction {
    private TransactionId id;
    private String description;
    private long amount;
    private Category category;
    private LocalDate date;

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category);
    }

    public Transaction(TransactionId id, String description, long amount, Category category) {
        this(id, description, amount, category, LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    }

    public Transaction(String description, long amount, Category category, LocalDate date) {
        this(new TransactionId(), description, amount, category, date);
    }

    public Transaction(TransactionId id, String description, long amount, Category category, LocalDate date) {
        if (id == null || id.uuid() == null) throw new IllegalArgumentException("Identificador obrigatório.");
        if (description == null || description.isBlank() || description.strip().length() > 255) {
            throw new IllegalArgumentException("Descrição deve conter de 1 a 255 caracteres.");
        }
        if (amount <= 0) throw new IllegalArgumentException("Valor em centavos deve ser maior que zero.");
        if (category == null) throw new IllegalArgumentException("Categoria obrigatória.");
        this.id = id;
        this.description = description.strip();
        this.amount = amount;
        this.category = category;
        this.date = date;
    }
}
