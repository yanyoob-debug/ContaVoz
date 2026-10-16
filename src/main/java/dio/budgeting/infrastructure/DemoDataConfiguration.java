package dio.budgeting.infrastructure;

import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoDataConfiguration {
    @Bean
    CommandLineRunner demoData(PersistTransactionUseCase persist) {
        return args -> {
            persist.execute(new PersistTransactionInput("Compras da semana", 18590, Category.GROCERIES));
            persist.execute(new PersistTransactionInput("Padaria", 2450, Category.GROCERIES));
            persist.execute(new PersistTransactionInput("Medicamentos", 6780, Category.PHARMA));
            persist.execute(new PersistTransactionInput("Combustível", 15000, Category.AUTO));
        };
    }
}
