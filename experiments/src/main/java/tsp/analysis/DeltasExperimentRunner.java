package tsp.analysis;

import tsp.algorithms.regret.*;
import tsp.algorithms.greedy.*;
import tsp.algorithms.deltas.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;
import java.util.*;

/**
 * Experiment runner for Deltas algorithm.
 * 
 * Evaluates different configurations of the Deltas algorithm:
 * 1. Type of intra-route moves: Nodes exchange or Edges exchange (2-opt)
 * 2. Type of starting solutions: Random or NearestNeighborAny2Regret_w1_1
 * 
 * For each combination, runs 200 experiments (one per starting node).
 */
public class DeltasExperimentRunner {
    
    /**
     * Represents a configuration for deltas experiments.
     */
    private static class DeltasConfiguration {
        final String name;
        final LocalSearchAlgorithm.Neighborhood neighborhood;
        final boolean useNearestNeighborAny2Regret_w1_1;
        
        DeltasConfiguration(String name, 
                           LocalSearchAlgorithm.Neighborhood neighborhood, 
                           boolean useNearestNeighborAny2Regret_w1_1) {
            this.name = name;
            this.neighborhood = neighborhood;
            this.useNearestNeighborAny2Regret_w1_1 = useNearestNeighborAny2Regret_w1_1;
        }
        
        String getFullName() {
            String moveType = neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP ? "Nodes" : "Edges";
            String initType = useNearestNeighborAny2Regret_w1_1 ? "NearestNeighborAny2Regret_w1_1" : "Random";
            return String.format("Deltas_%s_%s", moveType, initType);
        }
    }
    
    public static List<AlgorithmResult> runDeltasExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running Deltas experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        
        // Define all configurations
        List<DeltasConfiguration> configurations = new ArrayList<>();
        
        // Nodes + Random
        configurations.add(new DeltasConfiguration(
            "Deltas_Nodes_Random",
            LocalSearchAlgorithm.Neighborhood.NODE_SWAP,
            false
        ));
        
        // Nodes + NearestNeighborAny2Regret_w1_1
        configurations.add(new DeltasConfiguration(
            "Deltas_Nodes_NearestNeighborAny2Regret_w1_1",
            LocalSearchAlgorithm.Neighborhood.NODE_SWAP,
            true
        ));
        
        // Edges + Random
        configurations.add(new DeltasConfiguration(
            "Deltas_Edges_Random",
            LocalSearchAlgorithm.Neighborhood.TWO_OPT,
            false
        ));
        
        // Edges + NearestNeighborAny2Regret_w1_1
        configurations.add(new DeltasConfiguration(
            "Deltas_Edges_NearestNeighborAny2Regret_w1_1",
            LocalSearchAlgorithm.Neighborhood.TWO_OPT,
            true
        ));
        
        // Run experiments for each configuration
        for (DeltasConfiguration config : configurations) {
            System.out.println("\nRunning configuration: " + config.getFullName());
            List<AlgorithmResult> configResults = runDeltasConfiguration(instance, config);
            allResults.addAll(configResults);
            
            // Print summary statistics for this configuration
            printConfigurationSummary(configResults, config.getFullName());
        }
        
        return allResults;
    }
    
    private static List<AlgorithmResult> runDeltasConfiguration(Instance instance, DeltasConfiguration config) {
        List<AlgorithmResult> results = new ArrayList<>();
        int totalNodes = instance.getTotalNodes();
        
        // Run for each starting node
        for (int startNode = 0; startNode < totalNodes; startNode++) {
            // Create initial solution algorithm
            Algorithm initialAlgorithm;
            if (config.useNearestNeighborAny2Regret_w1_1) {
                initialAlgorithm = new NearestNeighborAnyPositionTwoRegretAlgorithm(instance, startNode, 1, 1);
            } else {
                initialAlgorithm = new RandomSolutionAlgorithm(instance, startNode);
            }
            
            // Create and run Deltas algorithm
            DeltasAlgorithm algorithm = new DeltasAlgorithm(initialAlgorithm, config.neighborhood);
            
            AlgorithmResult result = ExperimentRunner.runSingle(algorithm);
            results.add(result);
            
            // Progress indicator
            if ((startNode + 1) % 20 == 0) {
                System.out.printf("  Completed %d/%d experiments\n", startNode + 1, totalNodes);
            }
        }
        
        return results;
    }
    
    private static void printConfigurationSummary(List<AlgorithmResult> results, String configName) {
        if (results.isEmpty()) {
            return;
        }
        
        // Calculate statistics
        double minScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).min().orElse(0);
        double maxScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).max().orElse(0);
        double avgScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).average().orElse(0);
        double avgTime = results.stream().mapToDouble(AlgorithmResult::getComputationTimeMs).average().orElse(0);
        
        System.out.println("\n  Configuration: " + configName);
        System.out.printf("  Min Score: %.0f\n", minScore);
        System.out.printf("  Max Score: %.0f\n", maxScore);
        System.out.printf("  Avg Score: %.2f\n", avgScore);
        System.out.printf("  Avg Time: %.2f ms\n", avgTime);
        System.out.printf("  Total Runs: %d\n", results.size());
    }
    
    /**
     * Analyze results and find best solutions for each algorithm configuration.
     */
    public static Map<String, GreedyExperimentRunner.BestSolutionInfo> analyzeBestSolutions(
            List<AlgorithmResult> results) {
        Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions = new HashMap<>();
        
        // Group results by algorithm name (without start node suffix)
        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : results) {
            String baseAlgorithmName = extractBaseAlgorithmName(result.getAlgorithmName());
            groupedResults.computeIfAbsent(baseAlgorithmName, k -> new ArrayList<>()).add(result);
        }
        
        // Find best solution for each algorithm configuration
        for (Map.Entry<String, List<AlgorithmResult>> entry : groupedResults.entrySet()) {
            String algorithmName = entry.getKey();
            List<AlgorithmResult> algorithmResults = entry.getValue();
            
            AlgorithmResult bestResult = algorithmResults.stream()
                .min(Comparator.comparingDouble(AlgorithmResult::getObjectiveValue))
                .orElse(null);
            
            if (bestResult != null) {
                // Validate the best solution
                SolutionChecker.ValidationResult validation = 
                    SolutionChecker.validateSolution(bestResult.getSolution());
                
                GreedyExperimentRunner.BestSolutionInfo info = 
                    new GreedyExperimentRunner.BestSolutionInfo(
                        bestResult, validation, algorithmResults.size());
                bestSolutions.put(algorithmName, info);
            }
        }
        
        return bestSolutions;
    }
    
    /**
     * Extract base algorithm name (remove start node suffix).
     */
    private static String extractBaseAlgorithmName(String fullName) {
        int lastUnderscore = fullName.lastIndexOf("_start");
        if (lastUnderscore > 0) {
            return fullName.substring(0, lastUnderscore);
        }
        return fullName;
    }
}
