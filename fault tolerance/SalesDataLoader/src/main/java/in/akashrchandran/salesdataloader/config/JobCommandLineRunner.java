package in.akashrchandran.salesdataloader.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobCommandLineRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job job;

    @Override
    public void run(String... args) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("job.id", 1L)
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
    }
}
