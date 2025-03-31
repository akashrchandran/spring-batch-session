package in.akashrchandran.salesdataloader.config;

import in.akashrchandran.salesdataloader.entity.SalesData;
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

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

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
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .<SalesDataDto, SalesData>chunk(10, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public Job importSalesJob() {
        return new JobBuilder("importSalesJob", jobRepository)
                .start(step1())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
