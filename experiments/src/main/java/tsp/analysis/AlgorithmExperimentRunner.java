package tsp.analysis;

import tsp.core.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generic experiment runner for different algorithm types.
 * This class provides a unified interface for running experiments and organizing results.
 */
public class AlgorithmExperimentRunner {
    
    /**
     * Run experiments for a given algorithm type and instance.
     * 
     * @param instance The TSP instance to solve
     * @param algorithmType The type of algorithm (e.g., "greedy", "genetic", "simulated_annealing")
     * @param resultsDir The directory to save results
     * @throws Exception if experiments fail
     */
    public static void runExperiments(Instance instance, String algorithmType, Path resultsDir) throws Exception {
        System.out.println("Running " + algorithmType + " experiments for " + instance.getName() + "...");
        
        // Create algorithm-specific results directory
        Path algorithmResultsDir = resultsDir.resolve(algorithmType);
        if (!algorithmResultsDir.toFile().exists()) {
            algorithmResultsDir.toFile().mkdirs();
            System.out.println("Created algorithm results directory: " + algorithmResultsDir);
        }
        
        List<AlgorithmResult> results;
        Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions;
        
        // Switch on algorithm type to run appropriate experiments
        switch (algorithmType.toLowerCase()) {
            case "greedy":
                results = GreedyExperimentRunner.runGreedyExperiments(instance);
                bestSolutions = GreedyExperimentRunner.analyzeBestSolutions(results);
                break;
            
            case "regret":
                results = RegretExperimentRunner.runRegretExperiments(instance);
                bestSolutions = RegretExperimentRunner.analyzeBestSolutions(results);
                break;
            
            // Future algorithm types can be added here:
            // case "genetic":
            //     results = GeneticExperimentRunner.runGeneticExperiments(instance);
            //     bestSolutions = GeneticExperimentRunner.analyzeBestSolutions(results);
            //     break;
            // 
            // case "simulated_annealing":
            //     results = SimulatedAnnealingExperimentRunner.runSAExperiments(instance);
            //     bestSolutions = SimulatedAnnealingExperimentRunner.analyzeBestSolutions(results);
            //     break;
            
            default:
                throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        }
        
        // Print validation results
        printValidationResults(bestSolutions);
        
        // Export results using the new naming convention
        exportResults(instance, algorithmType, results, bestSolutions, algorithmResultsDir);
        
        // Print summary
        printExperimentSummary(results, bestSolutions);
    }
    
    /**
     * Print validation results for all algorithms.
     */
    private static void printValidationResults(Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions) {
        System.out.println("\n--- Solution Validation Results ---");
        for (Map.Entry<String, GreedyExperimentRunner.BestSolutionInfo> entry : bestSolutions.entrySet()) {
            String algorithmName = entry.getKey();
            GreedyExperimentRunner.BestSolutionInfo info = entry.getValue();
            
            System.out.println("\n" + algorithmName + ":");
            System.out.println("  Best objective: " + String.format("%d",
                info.getBestResult().getObjectiveValue()));
            System.out.println("  Total runs: " + info.getTotalRuns());
            System.out.println("  Validation: " + (info.isValidated() ? "PASSED" : "FAILED"));
            if (!info.isValidated()) {
                System.out.println("  Validation details:");
                for (String error : info.getValidation().getErrors()) {
                    System.out.println("    ERROR: " + error);
                }
            }
        }
    }
    
    /**
     * Export all experiment results to the appropriate files.
     */
    private static void exportResults(Instance instance, String algorithmType,
                                    List<AlgorithmResult> results,
                                    Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions,
                                    Path algorithmResultsDir) throws Exception {
        
        String instanceName = instance.getName();
        
        // Export detailed results to JSON
        Path jsonPath = algorithmResultsDir.resolve(instanceName + "_" + algorithmType + "_results.json");
        ResultExporter.exportToJson(results, jsonPath);
        System.out.println("\nExported detailed results to: " + jsonPath);
        
        // Export visualization data
        Path vizPath = algorithmResultsDir.resolve(instanceName + "_visualization.json");
        VisualizationExporter.exportVisualizationData(instance, bestSolutions, vizPath);
        System.out.println("Exported visualization data to: " + vizPath);
        
        // Export summary statistics
        Path summaryPath = algorithmResultsDir.resolve(instanceName + "_summary.json");
        VisualizationExporter.exportSummaryStatistics(results, bestSolutions, summaryPath);
        System.out.println("Exported summary statistics to: " + summaryPath);
        
        // Export to CSV (for quick inspection)
        Path csvPath = algorithmResultsDir.resolve(instanceName + "_" + algorithmType + "_results.csv");
        ResultExporter.exportToCsv(results, csvPath);
        System.out.println("Exported CSV results to: " + csvPath);
    }
    
    /**
     * Print summary statistics to console.
     */
    private static void printExperimentSummary(List<AlgorithmResult> results, 
                                             Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions) {
        System.out.println("\n--- Experiment Summary ---");
        
        for (Map.Entry<String, GreedyExperimentRunner.BestSolutionInfo> entry : bestSolutions.entrySet()) {
            String algorithmName = entry.getKey();
            GreedyExperimentRunner.BestSolutionInfo info = entry.getValue();
            
            // Calculate statistics from all runs of this algorithm
            List<AlgorithmResult> algorithmResults = results.stream()
                .filter(r -> r.getAlgorithmName().startsWith(algorithmName))
                .toList();
            
            double avgObjective = algorithmResults.stream()
                .mapToLong(AlgorithmResult::getObjectiveValue)
                .average().orElse(0.0);
            
            long maxObjective = algorithmResults.stream()
                .mapToLong(AlgorithmResult::getObjectiveValue)
                .max().orElse(0);
            
            double avgTime = algorithmResults.stream()
                .mapToDouble(AlgorithmResult::getComputationTimeMs)
                .average().orElse(0.0);
            
            System.out.printf("%s: Min=%d, Avg=%.2f, Max=%d, AvgTime=%.1fms (%d runs) [%s]\n",
                algorithmName, 
                info.getBestResult().getObjectiveValue(),
                avgObjective, 
                maxObjective,
                avgTime, 
                info.getTotalRuns(),
                info.isValidated() ? "VALIDATED" : "VALIDATION FAILED");
        }
    }
}