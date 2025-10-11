package tsp.analysis;

import tsp.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Exports solution data for 2D visualization in Python.
 */
public class VisualizationExporter {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    /**
     * Export visualization data for all best solutions.
     */
    public static void exportVisualizationData(Instance instance, 
                                             Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions,
                                             Path outputPath) throws IOException {
        
        Map<String, Object> visualizationData = new HashMap<>();
        
        // Instance information
        Map<String, Object> instanceInfo = new HashMap<>();
        instanceInfo.put("name", instance.getName());
        instanceInfo.put("total_nodes", instance.getTotalNodes());
        instanceInfo.put("required_nodes", instance.getRequiredNodes());
        
        // Node data (coordinates and costs)
        List<Map<String, Object>> nodeData = new ArrayList<>();
        Node[] nodes = instance.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("id", i);
            nodeInfo.put("x", nodes[i].getX());
            nodeInfo.put("y", nodes[i].getY());
            nodeInfo.put("cost", nodes[i].getCost());
            nodeData.add(nodeInfo);
        }
        
        // Best solutions data
        Map<String, Object> solutionsData = new HashMap<>();
        for (Map.Entry<String, GreedyExperimentRunner.BestSolutionInfo> entry : bestSolutions.entrySet()) {
            String algorithmName = entry.getKey();
            GreedyExperimentRunner.BestSolutionInfo info = entry.getValue();
            Solution solution = info.getBestResult().getSolution();
            
            Map<String, Object> solutionData = new HashMap<>();
            solutionData.put("algorithm", algorithmName);
            solutionData.put("objective_value", solution.getObjectiveValue());
            solutionData.put("path_length", solution.getPathLength());
            solutionData.put("node_costs", solution.getNodeCosts());
            solutionData.put("selected_nodes", new ArrayList<>(solution.getSelectedNodes()));
            solutionData.put("route", solution.getRoute());
            solutionData.put("is_validated", info.isValidated());
            solutionData.put("validation_report", info.getValidation().getReport());
            
            // Add coordinates for visualization
            List<Map<String, Object>> routeCoordinates = new ArrayList<>();
            for (Integer nodeId : solution.getRoute()) {
                Node node = nodes[nodeId];
                Map<String, Object> coord = new HashMap<>();
                coord.put("node_id", nodeId);
                coord.put("x", node.getX());
                coord.put("y", node.getY());
                coord.put("cost", node.getCost());
                routeCoordinates.add(coord);
            }
            solutionData.put("route_coordinates", routeCoordinates);
            
            solutionsData.put(algorithmName, solutionData);
        }
        
        // Combine all data
        visualizationData.put("instance", instanceInfo);
        visualizationData.put("nodes", nodeData);
        visualizationData.put("best_solutions", solutionsData);
        
        // Export to JSON
        objectMapper.writeValue(outputPath.toFile(), visualizationData);
    }
    
    /**
     * Export summary statistics for the report.
     */
    public static void exportSummaryStatistics(List<AlgorithmResult> results, 
                                             Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions,
                                             Path outputPath) throws IOException {
        
        Map<String, Object> summaryData = new HashMap<>();
        
        // Group results by algorithm
        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : results) {
            String baseAlgorithmName = extractBaseAlgorithmName(result.getAlgorithmName());
            groupedResults.computeIfAbsent(baseAlgorithmName, k -> new ArrayList<>()).add(result);
        }
        
        // Calculate statistics for each algorithm
        Map<String, Map<String, Object>> algorithmStats = new HashMap<>();
        for (Map.Entry<String, List<AlgorithmResult>> entry : groupedResults.entrySet()) {
            String algorithmName = entry.getKey();
            List<AlgorithmResult> algorithmResults = entry.getValue();
            
            DoubleSummaryStatistics objectiveStats = algorithmResults.stream()
                .mapToDouble(AlgorithmResult::getObjectiveValue)
                .summaryStatistics();
            
            LongSummaryStatistics timeStats = algorithmResults.stream()
                .mapToLong(AlgorithmResult::getComputationTimeMs)
                .summaryStatistics();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_runs", algorithmResults.size());
            stats.put("min_objective", objectiveStats.getMin());
            stats.put("max_objective", objectiveStats.getMax());
            stats.put("avg_objective", objectiveStats.getAverage());
            stats.put("avg_time_ms", timeStats.getAverage());
            
            // Add best solution info
            GreedyExperimentRunner.BestSolutionInfo bestInfo = bestSolutions.get(algorithmName);
            if (bestInfo != null) {
                stats.put("best_solution_nodes", bestInfo.getNodeIndices());
                stats.put("best_solution_validated", bestInfo.isValidated());
            }
            
            algorithmStats.put(algorithmName, stats);
        }
        
        summaryData.put("algorithm_statistics", algorithmStats);
        summaryData.put("total_results", results.size());
        
        // Export to JSON
        objectMapper.writeValue(outputPath.toFile(), summaryData);
    }
    
    private static String extractBaseAlgorithmName(String fullName) {
        int lastUnderscore = fullName.lastIndexOf("_start");
        if (lastUnderscore > 0) {
            return fullName.substring(0, lastUnderscore);
        }
        return fullName;
    }
}