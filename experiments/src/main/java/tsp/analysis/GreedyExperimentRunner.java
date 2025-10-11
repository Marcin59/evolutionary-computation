package tsp.analysis;

import tsp.algorithms.greedyAlgorithms.*;
import tsp.core.*;
import java.util.*;
import java.util.function.Function;

/**
 * Experiment runner for greedy algorithms according to requirements:
 * - 200 solutions starting from each node for each greedy method
 * - 200 random solutions
 */
public class GreedyExperimentRunner {
    
    public static List<AlgorithmResult> runGreedyExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running greedy experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        
        // Define algorithm factories
        Map<String, Function<Integer, Algorithm>> algorithmFactories = new HashMap<>();
        algorithmFactories.put("RandomSolution", startNode -> 
            new RandomSolutionAlgorithm(instance, startNode, System.nanoTime()));
        algorithmFactories.put("NearestNeighborEnd", startNode -> 
            new NearestNeighborEndAlgorithm(instance, startNode));
        algorithmFactories.put("NearestNeighborAny", startNode -> 
            new NearestNeighborAnyPositionAlgorithm(instance, startNode));
        algorithmFactories.put("GreedyCycle", startNode -> 
            new GreedyCycleAlgorithm(instance, startNode));
        
        // Run each algorithm starting from each node
        for (Map.Entry<String, Function<Integer, Algorithm>> entry : algorithmFactories.entrySet()) {
            String algorithmName = entry.getKey();
            Function<Integer, Algorithm> factory = entry.getValue();
            
            System.out.println("\nRunning " + algorithmName + "...");
            
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
        
        // Additional 200 random solutions (separate from node-based random)
        System.out.println("\nRunning additional random solutions...");
        for (int i = 0; i < 200; i++) {
            int randomStartNode = new Random().nextInt(instance.getTotalNodes());
            Algorithm algorithm = new RandomSolutionAlgorithm(instance, randomStartNode, 
                                                            System.nanoTime() + i);
            AlgorithmResult result = ExperimentRunner.runSingle(algorithm);
            allResults.add(result);
            
            if ((i + 1) % 50 == 0) {
                System.out.printf("  Completed %d/200 additional random solutions\n", i + 1);
            }
        }
        
        System.out.println("\nTotal results generated: " + allResults.size());
        return allResults;
    }
    
    /**
     * Analyze results and find best solutions for each algorithm.
     */
    public static Map<String, BestSolutionInfo> analyzeBestSolutions(List<AlgorithmResult> results) {
        Map<String, BestSolutionInfo> bestSolutions = new HashMap<>();
        
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
                
                BestSolutionInfo info = new BestSolutionInfo(
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
     * Container for best solution information.
     */
    public static class BestSolutionInfo {
        private final AlgorithmResult bestResult;
        private final SolutionChecker.ValidationResult validation;
        private final int totalRuns;
        
        public BestSolutionInfo(AlgorithmResult bestResult, 
                              SolutionChecker.ValidationResult validation, 
                              int totalRuns) {
            this.bestResult = bestResult;
            this.validation = validation;
            this.totalRuns = totalRuns;
        }
        
        public AlgorithmResult getBestResult() { return bestResult; }
        public SolutionChecker.ValidationResult getValidation() { return validation; }
        public int getTotalRuns() { return totalRuns; }
        
        public List<Integer> getNodeIndices() {
            return bestResult.getSolution().getRoute();
        }
        
        public boolean isValidated() {
            return validation.isValid();
        }
    }
}