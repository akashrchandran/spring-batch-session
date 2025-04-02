package in.akashrchandran.salesdataloader.config;

import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.exception.RetryAbleException;
import in.akashrchandran.salesdataloader.exception.SkipAbleException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.processor.SalesDataItemProcessor;
import in.akashrchandran.salesdataloader.listener.StepSkipListener;
import in.akashrchandran.salesdataloader.listener.RetryItemProcessListener;

import javax.sql.DataSource;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.classify.Classifier;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.ExceptionClassifierSkipPolicy;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.batch.item.file.FlatFileParseException;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final RetryItemProcessListener retryListener;

    @Bean
    public FlatFileItemReader<SalesDataDto> reader() {
        FlatFileItemReader<SalesDataDto> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data/sales_short.csv"));
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
    public StepSkipListener stepSkipListener() {
        return new StepSkipListener();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .<SalesDataDto, SalesData>chunk(100, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .faultTolerant()
                .retryLimit(3)
                .skipLimit(10)
                .retry(RetryAbleException.class)
                .noRetry(SkipAbleException.class)
                .skip(SkipAbleException.class)
                .listener(stepSkipListener())
                .listener(retryListener)
                .build();
    }

    @Bean
    public Job importSalesJob() {
        return new JobBuilder("importSalesJob", jobRepository)
                .start(step1())
                .build();
    }
}
