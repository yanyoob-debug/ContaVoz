package dio.budgeting.application;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() { super("Transação não encontrada. Atualize a listagem."); }
}
