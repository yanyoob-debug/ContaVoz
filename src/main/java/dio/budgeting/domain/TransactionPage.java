package dio.budgeting.domain;

import java.util.List;

public record TransactionPage(List<Transaction> items, long totalElements) {}
