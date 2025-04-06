package in.akashrchandran.salesdataloader.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class SalesDataPartitioner implements Partitioner {

    private final Resource resource;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();
        
        int totalLines = countLines();
        int linesPerPartition = totalLines / gridSize;
        
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            int startLine = (i * linesPerPartition) + 1;
            int endLine = (i == gridSize - 1) ? totalLines : (i + 1) * linesPerPartition;
            
            context.putInt("startLine", startLine);
            context.putInt("endLine", endLine);
            context.putInt("partition", i);
            
            result.put("partition" + i, context);
        }
        log.info("Partitions: {}", result);
        return result;
    }
    
    private int countLines() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count lines in file", e);
        }
    }
}
