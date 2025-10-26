package tsp.analysis;

import tsp.algorithms.regret.*;
import tsp.algorithms.greedy.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Experiment runner for local search algorithms.
 * 
 * Evaluates 8 combinations based on three binary options:
 * 1. Type of local search: Steepest or Greedy
 * 2. Type of intra-route moves: Nodes exchange or Edges exchange (2-opt)
 * 3. Type of starting solutions: Random or Greedy Cycle
 * 
 * For each combination, runs 200 experiments (one per starting node).
 */
public class LocalSearchExperimentRunner {
    
    /**
     * Represents a configuration for local search experiments.
     */
    private static class LSConfiguration {
        final String name;
        final boolean isSteepest;
        final LocalSearchAlgorithm.Neighborhood neighborhood;
        final boolean useNearestNeighborAny2Regret_w1_1;
        
        LSConfiguration(String name, boolean isSteepest, 
                       LocalSearchAlgorithm.Neighborhood neighborhood, 
                       boolean useNearestNeighborAny2Regret_w1_1) {
            this.name = name;
            this.isSteepest = isSteepest;
            this.neighborhood = neighborhood;
            this.useNearestNeighborAny2Regret_w1_1 = useNearestNeighborAny2Regret_w1_1;
        }
        
        String getFullName() {
            String lsType = isSteepest ? "Steepest" : "Greedy";
            String moveType = neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP ? "Nodes" : "Edges";
            String initType = useNearestNeighborAny2Regret_w1_1 ? "NearestNeighborAny2Regret_w1_1" : "Random";
            return String.format("%sLS_%s_%s", lsType, moveType, initType);
        }
    }
    
    public static List<AlgorithmResult> runLocalSearchExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running local search experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        
        // Define all 8 configurations
        List<LSConfiguration> configurations = Arrays.asList(
            // Steepest + Nodes + Random
            new LSConfiguration("SteepestLS_Nodes_Random", true, 
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP, false),
            
            // Steepest + Nodes + NearestNeighborAny2Regret_w1_1
            new LSConfiguration("SteepestLS_Nodes_NearestNeighborAny2Regret_w1_1", true, 
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP, true),
            
            // Steepest + Edges + Random
            new LSConfiguration("SteepestLS_Edges_Random", true, 
                LocalSearchAlgorithm.Neighborhood.TWO_OPT, false),
            
            // Steepest + Edges + NearestNeighborAny2Regret_w1_1
            new LSConfiguration("SteepestLS_Edges_NearestNeighborAny2Regret_w1_1", true, 
                LocalSearchAlgorithm.Neighborhood.TWO_OPT, true),
            
            // Greedy + Nodes + Random
            new LSConfiguration("GreedyLS_Nodes_Random", false, 
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP, false),
            
            // Greedy + Nodes + NearestNeighborAny2Regret_w1_1
            new LSConfiguration("GreedyLS_Nodes_NearestNeighborAny2Regret_w1_1", false, 
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP, true),
            
            // Greedy + Edges + Random
            new LSConfiguration("GreedyLS_Edges_Random", false, 
                LocalSearchAlgorithm.Neighborhood.TWO_OPT, false),
            
            // Greedy + Edges + NearestNeighborAny2Regret_w1_1
            new LSConfiguration("GreedyLS_Edges_NearestNeighborAny2Regret_w1_1", false, 
                LocalSearchAlgorithm.Neighborhood.TWO_OPT, true)
        );
        
        // Run each configuration starting from each node
        for (LSConfiguration config : configurations) {
            System.out.println("\nRunning " + config.getFullName() + "...");
            
            for (int startNode = 0; startNode < instance.getTotalNodes(); startNode++) {
                // Create initial solution algorithm
                Algorithm initialAlgorithm;
                if (config.useNearestNeighborAny2Regret_w1_1) {
                    initialAlgorithm = new NearestNeighborAnyPositionTwoRegretAlgorithm(instance, startNode, 1, 1);
                } else {
                    // Use seed for reproducibility
                    long seed = startNode * 1000L + config.hashCode();
                    initialAlgorithm = new RandomSolutionAlgorithm(instance, startNode, seed);
                }
                
                // Create local search algorithm
                LocalSearchAlgorithm lsAlgorithm;
                long seed = startNode * 1000L + config.hashCode();
                
                if (config.isSteepest) {
                    lsAlgorithm = new SteepestLocalSearch(initialAlgorithm, seed, config.neighborhood);
                } else {
                    lsAlgorithm = new GreedyLocalSearch(initialAlgorithm, seed, config.neighborhood);
                }
                
                // Run the algorithm and collect result
                AlgorithmResult result = ExperimentRunner.runSingle(lsAlgorithm);
                
                // Override the name to include configuration details
                AlgorithmResult namedResult = new AlgorithmResult(
                    config.getFullName() + "_start" + startNode,
                    result.getInstanceName(),
                    result.getSolution(),
                    result.getComputationTimeMs()
                );
                
                allResults.add(namedResult);
                
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
