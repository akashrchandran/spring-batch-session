package in.akashrchandran.salesdataloader.config;


import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.processor.SalesDataItemProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("worker")
@RequiredArgsConstructor
public class WorkerConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public PollableChannel requests() {
    return new QueueChannel();
    }

    @Bean
    public DirectChannelSpec replies() {
        return MessageChannels.direct();
    }

    @Bean
    public IntegrationFlow repliesFlow(MessageChannel replies, AmqpTemplate amqpTemplate) {
        return IntegrationFlow
                .from(replies)
                .handle(Amqp.outboundAdapter(amqpTemplate).routingKey("replies"))
                .get();
    }

    @Bean
    public IntegrationFlow requestsFlow(MessageChannel requests, ConnectionFactory connectionFactory) {
        var messageConverter = new SimpleMessageConverter();
        messageConverter.addAllowedListPatterns("*");
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, "requests").messageConverter(messageConverter))
                .channel(requests)
                .get();
    }

    @Bean
    public SalesDataItemProcessor processor() {
        return new SalesDataItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<SalesData> writer() {
        JdbcBatchItemWriter<SalesData> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO sales_data (index, invoice_no, stock_code, description, " +
                "quantity, " +
                "invoice_date, unit_price, customer_id, country) " +
                "VALUES (:index, :invoiceNo, :stockCode, :description, :quantity, " +
                ":invoiceDate, :unitPrice, :customerId, :country)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        return writer;
    }

    @Bean
    public IntegrationFlow workerStep(MessageChannel requests,
                           ItemProcessor<SalesDataDto, SalesData> processor,
                           ItemWriter<SalesData> writer,
                           MessageChannel replies) {
        return new RemoteChunkingWorkerBuilder<SalesDataDto, SalesData>()
                .inputChannel(requests)
                .outputChannel(replies)
                .itemProcessor(processor)
                .itemWriter(writer)
                .build();
    }
}
