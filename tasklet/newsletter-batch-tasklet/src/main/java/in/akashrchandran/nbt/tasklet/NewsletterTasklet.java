package in.akashrchandran.nbt.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsletterTasklet implements Tasklet {

    private final JavaMailSender mailSender;

    @Value("${newsletter.recipients}")
    private List<String> recipients;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting NewsletterTasklet execution.");
        for (String recipient : recipients) {
            log.debug("Sending email to {}", recipient);
            sendEmail(recipient);
        }
        log.info("Finished NewsletterTasklet execution.");
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
}
