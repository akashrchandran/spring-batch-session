package in.akashrchandran.nbt.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Job newsletterJob(JobRepository jobRepository, Step step) {
        return new JobBuilder("newsletter-job", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Step newsletterStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               Tasklet tasklet) {
        return new StepBuilder("newsletter-step", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
