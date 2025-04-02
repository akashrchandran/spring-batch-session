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
public class FibonacciTasklet implements Tasklet {
    
    @Value("${fibonacci.number:10}")
    private long number;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting Fibonacci check for number: {}", number);
        long startTime = System.currentTimeMillis();
        boolean isFibonacci = isFibonacciNumber(number);
        Random random = new Random();
        Thread.sleep(random.nextLong(5000 - 1000 + 1L) + 1000);
        long endTime = System.currentTimeMillis();
        log.info("Is {} a Fibonacci number? {} (Check took {} ms)", number, isFibonacci,
                (endTime - startTime));
        return RepeatStatus.FINISHED;
    }
    
    private boolean isFibonacciNumber(long num) {
        if (num < 0) {
            return false;
        }
        if (num == 0 || num == 1) {
            return true;
        }
        int a = 0;
        int b = 1;
        while (b < num) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        return b == num;
    }
    
}
