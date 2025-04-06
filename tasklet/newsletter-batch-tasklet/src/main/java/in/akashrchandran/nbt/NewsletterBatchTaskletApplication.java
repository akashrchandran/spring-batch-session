package in.akashrchandran.nbt;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewsletterBatchTaskletApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsletterBatchTaskletApplication.class, args);
    }

}