package tsp.analysis;

import tsp.algorithms.regret.*;
import tsp.core.*;
import java.util.*;
import java.util.function.Function;

/**
 * Experiment runner for 2-Regret algorithms.
 * Tests different weight combinations for weighted regret heuristics.
 * 
 * Runs experiments with:
 * - GreedyCycleTwoRegretAlgorithm
 * - NearestNeighborAnyPositionTwoRegretAlgorithm
 * 
 * For each algorithm, tests multiple weight combinations:
 * - Different ratios of insertion cost weight vs regret weight
 */
public class RegretExperimentRunner {
    
    // Weight combinations to test (weightInsertion, weightRegret)
    private static final int[][] WEIGHT_COMBINATIONS = {
        {1, 1},   // Equal weights
        {0, 1}   // only regret
    };
    
    public static List<AlgorithmResult> runRegretExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running 2-Regret experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        System.out.println("Testing " + WEIGHT_COMBINATIONS.length + " weight combinations");
        
        // Define algorithm factories for each weight combination
        Map<String, Function<Integer, Algorithm>> algorithmFactories = new LinkedHashMap<>();
        
        for (int[] weights : WEIGHT_COMBINATIONS) {
            int wInsertion = weights[0];
            int wRegret = weights[1];
            
            // GreedyCycle 2-Regret
            String gcName = "GreedyCycle2Regret_w" + wInsertion + "_" + wRegret;
            algorithmFactories.put(gcName, startNode -> 
                new GreedyCycleTwoRegretAlgorithm(instance, startNode, wInsertion, wRegret));
            
            // NearestNeighborAny 2-Regret
            String nnName = "NearestNeighborAny2Regret_w" + wInsertion + "_" + wRegret;
            algorithmFactories.put(nnName, startNode -> 
                new NearestNeighborAnyPositionTwoRegretAlgorithm(instance, startNode, wInsertion, wRegret));
        }
        
        System.out.println("Total algorithm variants: " + algorithmFactories.size());
        
        // Run each algorithm variant starting from each node
        int algorithmCount = 0;
        for (Map.Entry<String, Function<Integer, Algorithm>> entry : algorithmFactories.entrySet()) {
            String algorithmName = entry.getKey();
            Function<Integer, Algorithm> factory = entry.getValue();
            
            algorithmCount++;
            System.out.println("\n[" + algorithmCount + "/" + algorithmFactories.size() + "] Running " + algorithmName + "...");
            
            for (int startNode = 0; startNode < instance.getTotalNodes(); startNode++) {
                Algorithm algorithm = factory.apply(startNode);
                AlgorithmResult result = ExperimentRunner.runSingle(algorithm);
                allResults.add(result);
                
                if ((startNode + 1) % 50 == 0) {
                    System.out.printf("  Completed %d/%d starting nodes\n", 
                                    startNode + 1, instance.getTotalNodes());
                }
            }
        }
        
        System.out.println("\nTotal results generated: " + allResults.size());
        return allResults;
    }
    
    /**
     * Analyze results and find best solutions for each algorithm variant.
     */
    public static Map<String, GreedyExperimentRunner.BestSolutionInfo> analyzeBestSolutions(List<AlgorithmResult> results) {
        Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions = new HashMap<>();
        
        // Group results by algorithm name (without start node suffix)
        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : results) {
            String baseAlgorithmName = extractBaseAlgorithmName(result.getAlgorithmName());
            groupedResults.computeIfAbsent(baseAlgorithmName, k -> new ArrayList<>()).add(result);
        }
        
        // Find best solution for each algorithm
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
                
                GreedyExperimentRunner.BestSolutionInfo info = new GreedyExperimentRunner.BestSolutionInfo(
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
    
    /**
     * Get weight combinations being tested.
     */
    public static int[][] getWeightCombinations() {
        return WEIGHT_COMBINATIONS;
    }
}
