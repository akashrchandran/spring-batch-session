package in.akashrchandran.salesdataloader.config;

import in.akashrchandran.salesdataloader.partitioner.SalesDataPartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@Configuration
@Profile("master")
@Slf4j
@RequiredArgsConstructor
public class MasterConfig {

    private final JobRepository jobRepository;

    @Bean
    public DirectChannelSpec requests() {
        return MessageChannels.direct();
    }

    @Bean
    public DirectChannelSpec replies() {
        return MessageChannels.direct();
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
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setBeforePublishPostProcessors(message -> {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody()));
                Object deserializedObject = ois.readObject();
                log.info("Sending Partition Message to RabbitMQ: {}",deserializedObject);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return message;
        });
        return template;
    }

    @Bean
    public Step masterStep(JobExplorer jobExplorer, BeanFactory beanFactory,
                           MessageChannel requests, MessageChannel replies,
                           Partitioner partitioner) {
        return new RemotePartitioningManagerStepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitioner)
                .pollInterval(5000)
                .timeout(5000)
                .jobExplorer(jobExplorer)
                .beanFactory(beanFactory)
                .outputChannel(requests)
                .inputChannel(replies)
                .gridSize(10)
                .build();
    }

    @Bean
    @StepScope
    public Partitioner salesDataPartitioner() {
        return new SalesDataPartitioner(new ClassPathResource("data/sales.csv"));
    }

    @Bean
    public Job partitionSalesDataImportJob(Step masterStep) {
        return new JobBuilder("partitionSalesDataImportJob", jobRepository)
                .start(masterStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
