package in.akashrchandran.salesdataloader.config;

import in.akashrchandran.salesdataloader.entity.SalesData;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.processor.SalesDataItemProcessor;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    
    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("batch-thread-pool-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public FlatFileItemReader<SalesDataDto> reader() {
        FlatFileItemReader<SalesDataDto> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data/sales.csv"));
        reader.setLinesToSkip(1);
        DefaultLineMapper<SalesDataDto> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("index", "invoiceNo", "stockCode", "description", "quantity", 
                           "invoiceDate", "unitPrice", "customerId", "country");
        BeanWrapperFieldSetMapper<SalesDataDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesDataDto.class);
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public SalesDataItemProcessor processor() {
        return new SalesDataItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<SalesData> writer() {
        JdbcBatchItemWriter<SalesData> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO sales_data (index, invoice_no, stock_code, description, quantity, " +
                      "invoice_date, unit_price, customer_id, country) " +
                      "VALUES (:index, :invoiceNo, :stockCode, :description, :quantity, " +
                      ":invoiceDate, :unitPrice, :customerId, :country)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        return writer;
    }

    @Bean
    public AsyncItemProcessor<SalesDataDto, SalesData> asyncProcessor() {
        AsyncItemProcessor<SalesDataDto, SalesData> asyncProcessor = new AsyncItemProcessor<>();
        asyncProcessor.setDelegate(processor());
        asyncProcessor.setTaskExecutor(threadPoolTaskExecutor());
        return asyncProcessor;
    }

    @Bean
    public AsyncItemWriter<SalesData> asyncWriter() {
        AsyncItemWriter<SalesData> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer());
        return asyncWriter;
    }

    @Bean
    public Step asyncStep() {
        return new StepBuilder("asyncStep", jobRepository)
                .<SalesDataDto, Future<SalesData>>chunk(1000, transactionManager)
                .reader(reader())
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .build();
    }

    @Bean
    public Job asyncSalesDataImportJob() {
        return new JobBuilder("asyncSalesDataImportJob", jobRepository)
                .start(asyncStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
