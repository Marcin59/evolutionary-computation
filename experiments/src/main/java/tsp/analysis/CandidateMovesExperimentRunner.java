package tsp.analysis;

import tsp.algorithms.regret.*;
import tsp.algorithms.greedy.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;
import java.util.*;

/**
 * Experiment runner for Candidate Moves algorithm.
 * 
 * Evaluates different configurations of the Candidate Moves algorithm:
 * 1. Type of intra-route moves: Nodes exchange or Edges exchange (2-opt)
 * 2. Type of starting solutions: Random or NearestNeighborAny2Regret_w1_1
 * 3. Different candidate list sizes: 5, 10
 * 
 * For each combination, runs 200 experiments (one per starting node).
 */
public class CandidateMovesExperimentRunner {
    
    /**
     * Represents a configuration for candidate moves experiments.
     */
    private static class CMConfiguration {
        final String name;
        final LocalSearchAlgorithm.Neighborhood neighborhood;
        final boolean useNearestNeighborAny2Regret_w1_1;
        final int candidateListSize;
        
        CMConfiguration(String name, 
                       LocalSearchAlgorithm.Neighborhood neighborhood, 
                       boolean useNearestNeighborAny2Regret_w1_1,
                       int candidateListSize) {
            this.name = name;
            this.neighborhood = neighborhood;
            this.useNearestNeighborAny2Regret_w1_1 = useNearestNeighborAny2Regret_w1_1;
            this.candidateListSize = candidateListSize;
        }
        
        String getFullName() {
            String moveType = neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP ? "Nodes" : "Edges";
            String initType = useNearestNeighborAny2Regret_w1_1 ? "NearestNeighborAny2Regret_w1_1" : "Random";
            return String.format("CandidateMoves_k%d_%s_%s", candidateListSize, moveType, initType);
        }
    }
    
    public static List<AlgorithmResult> runCandidateMovesExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running Candidate Moves experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        
        // Define all configurations
        // Test different candidate list sizes with both neighborhoods and both initial solutions
        List<CMConfiguration> configurations = new ArrayList<>();
        int[] candidateSizes = {10};
        
        for (int k : candidateSizes) {
            // Nodes + Random
            configurations.add(new CMConfiguration(
                "CandidateMoves_k" + k + "_Nodes_Random",
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP,
                false,
                k
            ));
            
            // Nodes + NearestNeighborAny2Regret_w1_1
            configurations.add(new CMConfiguration(
                "CandidateMoves_k" + k + "_Nodes_NearestNeighborAny2Regret_w1_1",
                LocalSearchAlgorithm.Neighborhood.NODE_SWAP,
                true,
                k
            ));
            
            // Edges + Random
            configurations.add(new CMConfiguration(
                "CandidateMoves_k" + k + "_Edges_Random",
                LocalSearchAlgorithm.Neighborhood.TWO_OPT,
                false,
                k
            ));
            
            // Edges + NearestNeighborAny2Regret_w1_1
            configurations.add(new CMConfiguration(
                "CandidateMoves_k" + k + "_Edges_NearestNeighborAny2Regret_w1_1",
                LocalSearchAlgorithm.Neighborhood.TWO_OPT,
                true,
                k
            ));
        }
        
        // Run each configuration starting from each node
        for (CMConfiguration config : configurations) {
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
                
                // Create Candidate Moves algorithm
                long seed = startNode * 1000L + config.hashCode();
                CandidateMovesAlgorithm cmAlgorithm = new CandidateMovesAlgorithm(
                    initialAlgorithm, 
                    seed, 
                    config.neighborhood,
                    config.candidateListSize
                );
                
                // Run the algorithm and collect result
                AlgorithmResult result = ExperimentRunner.runSingle(cmAlgorithm);
                
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
