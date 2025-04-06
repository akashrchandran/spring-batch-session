package in.akashrchandran.salesdataloader.config;

import in.akashrchandran.salesdataloader.dto.SalesDataDto;
import in.akashrchandran.salesdataloader.entity.SalesData;
import in.akashrchandran.salesdataloader.partitioner.SalesDataPartitioner;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.QueueChannelSpec;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("master")
@RequiredArgsConstructor
public class MasterConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public DirectChannelSpec requests() {
        return MessageChannels.direct();
    }

    @Bean
    public PollableChannel replies() {
        return new QueueChannel();
    }

    @Bean
    public IntegrationFlow requestsFlow(MessageChannel requests, AmqpTemplate amqpTemplate) {
        return IntegrationFlow
                .from(requests)
                .handle(Amqp.outboundAdapter(amqpTemplate).routingKey("requests"))
                .get();
    }

    @Bean
    public IntegrationFlow repliesFlow(MessageChannel replies,
                                       ConnectionFactory connectionFactory) {
        var messageConverter = new SimpleMessageConverter();
        messageConverter.addAllowedListPatterns("*");
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, "replies").messageConverter(messageConverter))
                .channel(replies)
                .get();
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
    public Step masterStep(MessageChannel requests, PollableChannel replies,
                           ItemReader<SalesDataDto> reader) {
        return new RemoteChunkingManagerStepBuilder<>("masterStep", jobRepository)
                .chunk(10000)
                .reader(reader)
                .transactionManager(transactionManager)
                .outputChannel(requests)
                .inputChannel(replies)
                .build();
    }

    @Bean
    public Job partitionSalesDataImportJob(Step masterStep) {
        return new JobBuilder("chunkedSalesDataImportJob", jobRepository)
                .start(masterStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
