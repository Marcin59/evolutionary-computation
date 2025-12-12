package tsp.analysis;

import tsp.algorithms.evolutionary.HybridEvolutionaryAlgorithm;
import tsp.algorithms.evolutionary.HybridEvolutionaryAlgorithm.RecombinationOperator;
import tsp.algorithms.MSLS_ILS.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;
import java.util.*;

/**
 * Experiment runner for Hybrid Evolutionary Algorithm (HEA).
 * 
 * According to Assignment 9:
 * - Time limit: average MSLS runtime (with 200 iterations)
 * - 20 runs per configuration
 * - Population sizes to test: 20
 * - Use steepest local search with TWO_OPT neighborhood
 * - Test both recombination operators:
 *   - Operator 1: Common subpaths + random fill + random connection + LS
 *   - Operator 2: Parent selection + remove non-common + repair (with and without LS)
 */
public class HEAExperimentRunner {
    
    // Number of runs per configuration per instance
    private static final int RUNS_PER_CONFIG = 20;
    
    // MSLS iterations for time calibration
    private static final int MSLS_ITERATIONS = 200;
    
    // Default population size
    private static final int POPULATION_SIZE = 20;
    
    // Map to store local search call counts for HEA results
    private static final Map<AlgorithmResult, Integer> heaLocalSearchCalls = new HashMap<>();
    
    /**
     * Configuration for HEA experiments.
     */
    private static class HEAConfig {
        final RecombinationOperator operator;
        final boolean useLocalSearchAfterRecombination;
        
        HEAConfig(RecombinationOperator operator, boolean useLocalSearchAfterRecombination) {
            this.operator = operator;
            this.useLocalSearchAfterRecombination = useLocalSearchAfterRecombination;
        }
        
        String getName() {
            String lsFlag = useLocalSearchAfterRecombination ? "LS" : "noLS";
            return String.format("HEA_%s_%s", operator, lsFlag);
        }
    }
    
    /**
     * Run all HEA experiments for a given instance.
     */
    public static List<AlgorithmResult> runAllExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        System.out.println("Running HEA experiments for instance: " + instance.getName());
        System.out.println("Total nodes: " + instance.getTotalNodes());
        System.out.println("Required nodes: " + instance.getRequiredNodes());
        System.out.println("Runs per configuration: " + RUNS_PER_CONFIG);
        
        // Phase 1: Run MSLS to get average runtime for time limit
        System.out.println("\n=== Phase 1: Running MSLS to calibrate time limit ===");
        long avgMSLSRuntime = runMSLSForCalibration(instance);
        
        System.out.println(String.format("\n=== Average MSLS runtime: %.2f seconds ===", avgMSLSRuntime / 1000.0));
        System.out.println(String.format("=== HEA time limit set to: %.2f seconds ===\n", avgMSLSRuntime / 1000.0));
        
        // Phase 2: Define HEA configurations to test
        List<HEAConfig> configs = new ArrayList<>();
        // Operator 1 with local search
        configs.add(new HEAConfig(RecombinationOperator.OPERATOR_1, true));
        // Operator 2 with local search
        configs.add(new HEAConfig(RecombinationOperator.OPERATOR_2, true));
        // Operator 2 without local search after recombination
        configs.add(new HEAConfig(RecombinationOperator.OPERATOR_2, false));
        
        System.out.println("\n=== Phase 2: Running HEA experiments ===");
        System.out.println("Configurations to test: " + configs.size());
        
        // Run experiments for each configuration
        int configNum = 0;
        for (HEAConfig config : configs) {
            configNum++;
            System.out.println(String.format("\n[%d/%d] Running configuration: %s", 
                configNum, configs.size(), config.getName()));
            
            List<AlgorithmResult> heaResults = runHEAConfiguration(
                instance, 
                POPULATION_SIZE, 
                avgMSLSRuntime,
                LocalSearchAlgorithm.Neighborhood.TWO_OPT,
                config.operator,
                config.useLocalSearchAfterRecombination
            );
            allResults.addAll(heaResults);
            
            // Print summary
            printConfigurationSummary(heaResults, config.getName());
        }
        
        return allResults;
    }
    
    /**
     * Run MSLS experiments to get average runtime for HEA time limit calibration.
     */
    private static long runMSLSForCalibration(Instance instance) {
        List<Long> runtimes = new ArrayList<>();
        
        System.out.println("Running MSLS calibration (" + RUNS_PER_CONFIG + " runs)...");
        
        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Calibration run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            
            long seed = System.nanoTime() + run * 1000000L;
            
            AlgorithmMSLS msls = new AlgorithmMSLS(
                instance,
                MSLS_ITERATIONS,
                AlgorithmMSLS.LocalSearchType.STEEPEST,
                LocalSearchAlgorithm.Neighborhood.TWO_OPT,
                seed
            );
            
            long startTime = System.currentTimeMillis();
            msls.solve();
            long endTime = System.currentTimeMillis();
            
            long runtime = endTime - startTime;
            runtimes.add(runtime);
            
            System.out.printf("Time: %.2fs\n", runtime / 1000.0);
        }
        
        // Calculate average runtime
        long avgRuntime = (long) runtimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        return avgRuntime;
    }
    
    /**
     * Run HEA experiments for a specific configuration.
     */
    private static List<AlgorithmResult> runHEAConfiguration(
            Instance instance, 
            int populationSize,
            long timeLimit,
            LocalSearchAlgorithm.Neighborhood neighborhood,
            RecombinationOperator operator,
            boolean useLocalSearchAfterRecombination) {
        
        List<AlgorithmResult> results = new ArrayList<>();
        
        System.out.println(String.format("Running HEA: pop=%d, time=%.2fs, operator=%s, LS=%s", 
            populationSize, timeLimit / 1000.0, operator, useLocalSearchAfterRecombination));
        
        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            
            long seed = System.nanoTime() + run * 1000000L;
            
            HybridEvolutionaryAlgorithm hea = new HybridEvolutionaryAlgorithm(
                instance,
                populationSize,
                timeLimit,
                neighborhood,
                operator,
                useLocalSearchAfterRecombination,
                seed
            );
            
            long startTime = System.currentTimeMillis();
            Solution solution = hea.solve();
            long endTime = System.currentTimeMillis();
            
            long computationTime = endTime - startTime;
            int localSearchCalls = hea.getLocalSearchCallCount();
            int iterations = hea.getIterationCount();
            
            AlgorithmResult result = new AlgorithmResult(
                hea.getName(),
                instance.getName(),
                solution,
                computationTime
            );
            results.add(result);
            
            // Store local search call count
            heaLocalSearchCalls.put(result, localSearchCalls);
            
            System.out.printf("Objective: %d, LS Calls: %d, Iterations: %d, Time: %.2fs\n", 
                result.getObjectiveValue(), 
                localSearchCalls,
                iterations,
                computationTime / 1000.0);
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
        System.out.printf("  Avg Time: %.2f seconds\n", avgTime / 1000.0);
        
        // Show LS call statistics
        double avgLSCalls = results.stream()
            .filter(heaLocalSearchCalls::containsKey)
            .mapToInt(heaLocalSearchCalls::get)
            .average()
            .orElse(0);
        int minLSCalls = results.stream()
            .filter(heaLocalSearchCalls::containsKey)
            .mapToInt(heaLocalSearchCalls::get)
            .min()
            .orElse(0);
        int maxLSCalls = results.stream()
            .filter(heaLocalSearchCalls::containsKey)
            .mapToInt(heaLocalSearchCalls::get)
            .max()
            .orElse(0);
        
        System.out.printf("  Avg LS Calls: %.2f\n", avgLSCalls);
        System.out.printf("  Min LS Calls: %d\n", minLSCalls);
        System.out.printf("  Max LS Calls: %d\n", maxLSCalls);
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
            String algorithmName = result.getAlgorithmName();
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
     * Get the local search call count for a specific HEA result.
     */
    public static Integer getLocalSearchCalls(AlgorithmResult result) {
        return heaLocalSearchCalls.get(result);
    }
    
    /**
     * Get all local search call counts.
     */
    public static Map<AlgorithmResult, Integer> getAllLocalSearchCalls() {
        return new HashMap<>(heaLocalSearchCalls);
    }
}
