package tsp.analysis;

import tsp.algorithms.MSLS_ILS.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;
import java.util.*;

/**
 * Experiment runner for MSLS and ILS algorithms.
 * 
 * According to Assignment 6:
 * - Time limit: 438.46 seconds per run
 * - 20 runs per configuration (10 for each instance)
 * - Test configurations:
 *   MSLS: steepest/greedy × nodes/edges
 *   ILS: steepest/greedy × nodes/edges
 */
public class MSLS_ILS_ExperimentRunner {
    
    // Time limit in milliseconds (438.46 seconds)
    private static final long TIME_LIMIT_MS = 438460;
    
    // Number of runs per configuration per instance
    private static final int RUNS_PER_CONFIG = 20;
    
    // Map to store local search call counts for ILS results
    private static final Map<AlgorithmResult, Integer> ilsLocalSearchCalls = new HashMap<>();
    
    /**
     * Configuration for MSLS/ILS experiments.
     */
    private static class ExperimentConfig {
        final String algorithmType; // "MSLS" or "ILS"
        final String localSearchType; // "STEEPEST" or "GREEDY"
        final LocalSearchAlgorithm.Neighborhood neighborhood;
        final int perturbationStrength; // For ILS intra-route perturbation
        final int externalPerturbationStrength; // For ILS inter-route perturbation
        
        ExperimentConfig(String algorithmType, String localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood) {
            this(algorithmType, localSearchType, neighborhood, 0, 0);
        }
        
        ExperimentConfig(String algorithmType, String localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood,
                        int perturbationStrength, int externalPerturbationStrength) {
            this.algorithmType = algorithmType;
            this.localSearchType = localSearchType;
            this.neighborhood = neighborhood;
            this.perturbationStrength = perturbationStrength;
            this.externalPerturbationStrength = externalPerturbationStrength;
        }
        
        String getFullName() {
            String moveType = neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP ? "Nodes" : "Edges";
            if (algorithmType.equals("ILS")) {
                return String.format("%s_%s_%s_P%d_E%d", algorithmType, localSearchType, moveType, 
                    perturbationStrength, externalPerturbationStrength);
            }
            return String.format("%s_%s_%s", algorithmType, localSearchType, moveType);
        }
    }
    
    /**
     * Run all MSLS and ILS experiments for a given instance.
     */
    public static List<AlgorithmResult> runAllExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running MSLS and ILS experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        System.out.println("Runs per configuration: " + RUNS_PER_CONFIG);
        
        // Define MSLS configurations - only Steepest with edge exchange (TWO_OPT)
        List<ExperimentConfig> mslsConfigurations = new ArrayList<>();
        mslsConfigurations.add(new ExperimentConfig("MSLS", "STEEPEST", LocalSearchAlgorithm.Neighborhood.TWO_OPT));
        
        // Run MSLS experiments first
        System.out.println("\n=== Phase 1: Running MSLS experiments ===");
        List<Double> mslsRuntimes = new ArrayList<>();
        int configNum = 0;
        for (ExperimentConfig config : mslsConfigurations) {
            configNum++;
            System.out.println(String.format("\n[%d/%d] Running configuration: %s", 
                configNum, mslsConfigurations.size(), config.getFullName()));
            
            List<AlgorithmResult> configResults = runMSLSConfiguration(instance, config);
            allResults.addAll(configResults);
            
            // Collect runtimes
            for (AlgorithmResult result : configResults) {
                mslsRuntimes.add((double) result.getComputationTimeMs());
            }
            
            // Print summary statistics
            printConfigurationSummary(configResults, config.getFullName());
        }
        
        // Calculate average MSLS runtime for ILS time limit
        double avgMSLSRuntime = mslsRuntimes.stream().mapToDouble(d -> d).average().orElse((double) TIME_LIMIT_MS);
        long ilsTimeLimit = (long) avgMSLSRuntime;
        
        System.out.println(String.format("\n=== Average MSLS runtime: %.2f seconds ===", avgMSLSRuntime / 1000.0));
        System.out.println(String.format("=== ILS time limit set to: %.2f seconds ===\n", ilsTimeLimit / 1000.0));
        
        // Define ILS configurations with grid of perturbation strengths
        // Grid: 5, 10, 15 for both intra-route and inter-route perturbations (9 combinations)
        List<ExperimentConfig> ilsConfigurations = new ArrayList<>();
        int[] perturbationValues = {15};
        int[] extrnalValues = {1, 3};
        
        for (int intraPert : perturbationValues) {
            for (int externalPert : extrnalValues) {
                ilsConfigurations.add(new ExperimentConfig("ILS", "STEEPEST", 
                    LocalSearchAlgorithm.Neighborhood.TWO_OPT, intraPert, externalPert));
            }
        }
        
        System.out.println(String.format("=== Running %d ILS configurations (3x3 grid) ===\n", ilsConfigurations.size()));
        
        // Run ILS experiments
        System.out.println("\n=== Phase 2: Running ILS experiments ===");
        configNum = 0;
        for (ExperimentConfig config : ilsConfigurations) {
            configNum++;
            System.out.println(String.format("\n[%d/%d] Running configuration: %s", 
                configNum, ilsConfigurations.size(), config.getFullName()));
            
            List<AlgorithmResult> configResults = runILSConfiguration(instance, config, ilsTimeLimit);
            allResults.addAll(configResults);
            
            // Print summary statistics
            printConfigurationSummary(configResults, config.getFullName());
        }
        
        return allResults;
    }
    
    /**
     * Run MSLS experiments for a specific configuration using basic local search.
     */
    private static List<AlgorithmResult> runMSLSConfiguration(Instance instance, ExperimentConfig config) {
        List<AlgorithmResult> results = new ArrayList<>();
        
        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            
            // Use different seed for each run
            long seed = System.nanoTime() + run * 1000000L;
            
            AlgorithmMSLS.LocalSearchType lsType = config.localSearchType.equals("STEEPEST") 
                ? AlgorithmMSLS.LocalSearchType.STEEPEST 
                : AlgorithmMSLS.LocalSearchType.GREEDY;
            
            // MSLS with 200 iterations (no time limit, runs all iterations)
            Algorithm algorithm = new AlgorithmMSLS(
                instance, 
                200, // Fixed number of iterations
                lsType,
                config.neighborhood,
                seed
            );
            
            // Run the algorithm
            AlgorithmResult result = ExperimentRunner.runSingle(algorithm);
            results.add(result);
            
            System.out.printf("Objective: %d, Time: %.2fs\n", 
                result.getObjectiveValue(), 
                result.getComputationTimeMs() / 1000.0);
        }
        
        return results;
    }
    
    /**
     * Run ILS experiments for a specific configuration.
     */
    private static List<AlgorithmResult> runILSConfiguration(Instance instance, ExperimentConfig config, long timeLimit) {
        List<AlgorithmResult> results = new ArrayList<>();
        
        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            
            // Use different seed for each run
            long seed = System.nanoTime() + run * 1000000L;
            
            AlgorithmILS.LocalSearchType lsType = config.localSearchType.equals("STEEPEST") 
                ? AlgorithmILS.LocalSearchType.STEEPEST 
                : AlgorithmILS.LocalSearchType.GREEDY;
            
            // Use the configured perturbation strengths from the grid
            Algorithm algorithm = new AlgorithmILS(
                instance,
                lsType,
                config.neighborhood,
                config.perturbationStrength,
                config.externalPerturbationStrength,
                timeLimit,
                seed
            );
            
            // Run the algorithm
            AlgorithmResult result = ExperimentRunner.runSingle(algorithm);
            results.add(result);
            
            // Get local search call count if it's an ILS algorithm
            int lsCallCount = 0;
            if (algorithm instanceof AlgorithmILS) {
                lsCallCount = ((AlgorithmILS) algorithm).getLocalSearchCallCount();
                ilsLocalSearchCalls.put(result, lsCallCount); // Store for later analysis
            }
            
            System.out.printf("Objective: %d, LS Calls: %d, Intra-Pert: %d, External-Pert: %d\n", 
                result.getObjectiveValue(), 
                lsCallCount,
                config.perturbationStrength,
                config.externalPerturbationStrength);
        }
        
        return results;
    }
    
    /**
     * Print summary statistics for a configuration.
     */
    private static void printConfigurationSummary(List<AlgorithmResult> results, String configName) {
        if (results.isEmpty()) {
            return;
        }
        
        // Calculate statistics
        double minScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).min().orElse(0);
        double maxScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).max().orElse(0);
        double avgScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).average().orElse(0);
        double avgTime = results.stream().mapToDouble(AlgorithmResult::getComputationTimeMs).average().orElse(0);
        
        System.out.println("\n  === Summary for " + configName + " ===");
        System.out.printf("  Min Score: %.0f\n", minScore);
        System.out.printf("  Max Score: %.0f\n", maxScore);
        System.out.printf("  Avg Score: %.2f\n", avgScore);
        
        // Check if these are ILS results (have local search call counts)
        boolean hasLSCalls = results.stream().anyMatch(ilsLocalSearchCalls::containsKey);
        if (hasLSCalls) {
            // Show LS call statistics for ILS
            double avgLSCalls = results.stream()
                .filter(ilsLocalSearchCalls::containsKey)
                .mapToInt(ilsLocalSearchCalls::get)
                .average()
                .orElse(0);
            int minLSCalls = results.stream()
                .filter(ilsLocalSearchCalls::containsKey)
                .mapToInt(ilsLocalSearchCalls::get)
                .min()
                .orElse(0);
            int maxLSCalls = results.stream()
                .filter(ilsLocalSearchCalls::containsKey)
                .mapToInt(ilsLocalSearchCalls::get)
                .max()
                .orElse(0);
            System.out.printf("  Avg LS Calls: %.2f\n", avgLSCalls);
            System.out.printf("  Min LS Calls: %d\n", minLSCalls);
            System.out.printf("  Max LS Calls: %d\n", maxLSCalls);
        } else {
            // Show time for MSLS
            System.out.printf("  Avg Time: %.2f seconds\n", avgTime / 1000.0);
        }
        
        System.out.printf("  Total Runs: %d\n", results.size());
    }
    
    /**
     * Analyze results and find best solutions for each algorithm configuration.
     */
    public static Map<String, GreedyExperimentRunner.BestSolutionInfo> analyzeBestSolutions(
            List<AlgorithmResult> results) {
        Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions = new HashMap<>();
        
        // Group results by algorithm name
        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : results) {
            String algorithmName = extractBaseAlgorithmName(result.getAlgorithmName());
            groupedResults.computeIfAbsent(algorithmName, k -> new ArrayList<>()).add(result);
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
                
                GreedyExperimentRunner.BestSolutionInfo info = 
                    new GreedyExperimentRunner.BestSolutionInfo(
                        bestResult, validation, algorithmResults.size());
                bestSolutions.put(algorithmName, info);
            }
        }
        
        return bestSolutions;
    }
    
    /**
     * Extract base algorithm name (remove any suffix).
     */
    private static String extractBaseAlgorithmName(String fullName) {
        // Remove any random/starting node suffixes
        int lastUnderscore = fullName.lastIndexOf("_Random");
        if (lastUnderscore > 0) {
            return fullName.substring(0, lastUnderscore);
        }
        return fullName;
    }
    
    /**
     * Get the local search call count for a specific ILS result.
     * Returns null if the result is not an ILS result.
     */
    public static Integer getLocalSearchCalls(AlgorithmResult result) {
        return ilsLocalSearchCalls.get(result);
    }
    
    /**
     * Get all local search call counts.
     */
    public static Map<AlgorithmResult, Integer> getAllLocalSearchCalls() {
        return new HashMap<>(ilsLocalSearchCalls);
    }
}
