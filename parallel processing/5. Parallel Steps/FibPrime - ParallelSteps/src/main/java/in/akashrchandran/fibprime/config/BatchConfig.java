package in.akashrchandran.fibprime.config;

import in.akashrchandran.fibprime.tasklet.FibonacciTasklet;
import in.akashrchandran.fibprime.tasklet.PrimeTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final FibonacciTasklet fibonacciTasklet;

    private final PrimeTasklet primeTasklet;
    
    @Bean
    public Step fibonacciStep() {
        return new StepBuilder("fibonacciStep", jobRepository)
                .tasklet(fibonacciTasklet, transactionManager)
                .build();
    }
    
    @Bean
    public Step primeStep() {
        return new StepBuilder("primeStep", jobRepository)
                .tasklet(primeTasklet, transactionManager)
                .build();
    }
    
    @Bean
    public Job parallelJob() {
        Flow fibonacciFlow = new FlowBuilder<Flow>("fibonacciFlow")
                .start(fibonacciStep())
                .build();
        
        Flow primeFlow = new FlowBuilder<Flow>("primeFlow")
                .start(primeStep())
                .build();
        
        Flow parallelFlow = new FlowBuilder<Flow>("parallelFlow")
                .split(new SimpleAsyncTaskExecutor())
                .add(fibonacciFlow, primeFlow)
                .build();
        
        return new JobBuilder("parallelJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(parallelFlow)
                .end()
                .build();
    }
}
