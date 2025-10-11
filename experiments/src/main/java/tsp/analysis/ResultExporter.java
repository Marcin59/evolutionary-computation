package tsp.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for exporting experiment results to various formats for analysis.
 */
public class ResultExporter {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    /**
     * Export results to JSON format for Python analysis.
     */
    public static void exportToJson(List<AlgorithmResult> results, Path outputPath) throws IOException {
        List<Map<String, Object>> jsonData = results.stream()
            .map(ResultExporter::resultToMap)
            .collect(Collectors.toList());
        
        Map<String, Object> export = new HashMap<>();
        export.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        export.put("results", jsonData);
        export.put("summary", createSummary(results));
        
        objectMapper.writeValue(outputPath.toFile(), export);
    }
    
    /**
     * Export results to CSV format.
     */
    public static void exportToCsv(List<AlgorithmResult> results, Path outputPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("algorithm,instance,objective_value,path_length,node_costs,computation_time_ms,selected_nodes,total_nodes\n");
        
        // Data rows
        for (AlgorithmResult result : results) {
            csv.append(String.format("%s,%s,%.2f,%.2f,%.2f,%d,%d,%d\n",
                result.getAlgorithmName(),
                result.getInstanceName(),
                result.getObjectiveValue(),
                result.getPathLength(),
                result.getNodeCosts(),
                result.getComputationTimeMs(),
                result.getSelectedNodes(),
                result.getTotalNodes()
            ));
        }
        
        Files.writeString(outputPath, csv.toString());
    }
    
    /**
     * Export detailed solution information.
     */
    public static void exportSolutionDetails(List<AlgorithmResult> results, Path outputPath) throws IOException {
        List<Map<String, Object>> detailedData = results.stream()
            .map(result -> {
                Map<String, Object> data = resultToMap(result);
                data.put("selected_nodes", new ArrayList<>(result.getSolution().getSelectedNodes()));
                data.put("route", result.getSolution().getRoute());
                return data;
            })
            .collect(Collectors.toList());
        
        objectMapper.writeValue(outputPath.toFile(), detailedData);
    }
    
    /**
     * Convert AlgorithmResult to Map for JSON serialization.
     */
    private static Map<String, Object> resultToMap(AlgorithmResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("algorithm", result.getAlgorithmName());
        map.put("instance", result.getInstanceName());
        map.put("objective_value", result.getObjectiveValue());
        map.put("path_length", result.getPathLength());
        map.put("node_costs", result.getNodeCosts());
        map.put("computation_time_ms", result.getComputationTimeMs());
        map.put("selected_nodes_count", result.getSelectedNodes());
        map.put("total_nodes", result.getTotalNodes());
        return map;
    }
    
    /**
     * Create summary statistics for the results.
     */
    private static Map<String, Object> createSummary(List<AlgorithmResult> results) {
        Map<String, Object> summary = new HashMap<>();
        
        // Group by algorithm
        Map<String, List<AlgorithmResult>> byAlgorithm = results.stream()
            .collect(Collectors.groupingBy(AlgorithmResult::getAlgorithmName));
        
        Map<String, Map<String, Object>> algorithmStats = new HashMap<>();
        
        for (Map.Entry<String, List<AlgorithmResult>> entry : byAlgorithm.entrySet()) {
            String algorithm = entry.getKey();
            List<AlgorithmResult> algorithmResults = entry.getValue();
            
            DoubleSummaryStatistics objectiveStats = algorithmResults.stream()
                .mapToDouble(AlgorithmResult::getObjectiveValue)
                .summaryStatistics();
            
            LongSummaryStatistics timeStats = algorithmResults.stream()
                .mapToLong(AlgorithmResult::getComputationTimeMs)
                .summaryStatistics();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("runs", algorithmResults.size());
            stats.put("objective_mean", objectiveStats.getAverage());
            stats.put("objective_min", objectiveStats.getMin());
            stats.put("objective_max", objectiveStats.getMax());
            stats.put("time_mean_ms", timeStats.getAverage());
            stats.put("time_min_ms", timeStats.getMin());
            stats.put("time_max_ms", timeStats.getMax());
            
            algorithmStats.put(algorithm, stats);
        }
        
        summary.put("total_runs", results.size());
        summary.put("algorithms", algorithmStats);
        
        return summary;
    }
}