package in.akashrchandran.nbt.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsletterTasklet implements Tasklet {

    @Value("${newsletter.recipients.file}")
    private String recipientsFilePath;

    @Value("${newsletter.max.concurrent:10}")
    private int maxConcurrent;

    private final JavaMailSender mailSender;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting NewsletterTasklet execution with async processing.");
        // Load recipients from file
        List<String> recipients = readRecipientsFromFile();
        int totalRecipients = recipients.size();

        // Create a fixed thread pool with size limit
        ExecutorService executor = Executors.newFixedThreadPool(maxConcurrent);

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        // Create async tasks for all recipients
        for (String recipient : recipients) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    sendEmail(recipient);
                } catch (Exception e) {
                    log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
                }
            }, executor);
            allFutures.add(future);
        }

        // Wait for all emails to complete
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

        // Update job metrics
        contribution.incrementWriteCount(totalRecipients);

        // Shutdown the executor service
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Finished NewsletterTasklet execution. Total emails sent: {}", totalRecipients);
        return RepeatStatus.FINISHED;
    }

    private void sendEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("yourEmail@domain.com");
        message.setTo(to);
        message.setSubject("Newsletter");
        message.setText("This is the content of the newsletter.");
        mailSender.send(message);
        log.info("Email sent successfully to {}", to);
    }

    private List<String> readRecipientsFromFile() {
        try {
            Resource resource = new ClassPathResource(recipientsFilePath);
            if (resource.exists()) {
                try (Stream<String> lines = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                        .lines()) {
                    log.info("Successfully loaded recipients from resources: {}", recipientsFilePath);
                    return lines.filter(line -> line != null && !line.isBlank())
                            .collect(Collectors.toList());
                }
            } else {
                log.warn("Resource file '{}' does not exist", recipientsFilePath);
            }
        } catch (Exception e) {
            log.error("Error reading recipients from file '{}': {}", recipientsFilePath, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

}