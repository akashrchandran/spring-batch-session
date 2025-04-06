package in.akashrchandran.fibprime.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
public class PrimeTasklet implements Tasklet {
    @Value("${prime.number:11}")
    private long number;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting Prime number check for number: {}", number);
        boolean isPrime = isPrimeNumber(number);
        Random random = new Random();
        Thread.sleep(random.nextLong(5000 - 1000 + 1L) + 1000);
        log.info("Is {} a Prime number? {}", number, isPrime);
        return RepeatStatus.FINISHED;
    }
    
    private boolean isPrimeNumber(long num) {
        if (num <= 1) {
            return false;
        }
        if (num <= 3) {
            return true;
        }
        if (num % 2 == 0 || num % 3 == 0) {
            return false;
        }
        int sqrtNum = (int) Math.sqrt(num);
        for (int i = 5; i <= sqrtNum; i += 6) {
            if (num % i == 0 || num % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }
}
