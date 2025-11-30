package tsp.analysis;

import tsp.algorithms.LNS.LNSAlgorithm;
import tsp.algorithms.MSLS_ILS.AlgorithmMSLS;
import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.core.Algorithm;
import tsp.analysis.AlgorithmResult;
import tsp.core.Instance;
import tsp.core.Solution;
import tsp.analysis.SolutionChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LNSExperimentRunner {

    // Number of runs per configuration per instance
    private static final int RUNS_PER_CONFIG = 20;

    /**
     * Configuration for LNS experiments.
     */
    private static class LNSConfig {
        final double destructionRate;
        final LNSAlgorithm.DestroyHeuristic destroyHeuristic;
        final boolean useLocalSearchAfterRepair;
        final LocalSearchAlgorithm.Neighborhood neighborhood = LocalSearchAlgorithm.Neighborhood.TWO_OPT; // Best one generally

        LNSConfig(double destructionRate, LNSAlgorithm.DestroyHeuristic destroyHeuristic,
                         boolean useLocalSearchAfterRepair) {
            this.destructionRate = destructionRate;
            this.destroyHeuristic = destroyHeuristic;
            this.useLocalSearchAfterRepair = useLocalSearchAfterRepair;
        }

        String getFullName() {
            return String.format("LNS_d-%.2f_%s_ls-%s",
                    destructionRate, destroyHeuristic, useLocalSearchAfterRepair ? "On" : "Off");
        }
    }

    public static List<AlgorithmResult> runAllExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();

        System.out.println("Running LNS experiments for instance: " + instance.getName());
        System.out.println("Runs per configuration: " + RUNS_PER_CONFIG);

        // First, determine the time limit from average MSLS run time.
        System.out.println("\n=== Phase 1: Calculating average MSLS runtime for time limit ===");
        List<Double> mslsRuntimes = new ArrayList<>();
        AlgorithmMSLS.LocalSearchType lsType = AlgorithmMSLS.LocalSearchType.STEEPEST;
        LocalSearchAlgorithm.Neighborhood neighborhood = LocalSearchAlgorithm.Neighborhood.TWO_OPT;

        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            long seed = System.nanoTime() + run;
            Algorithm msls = new AlgorithmMSLS(instance, 200, lsType, neighborhood, seed);
            
            long startTime = System.currentTimeMillis();
            msls.solve();
            long endTime = System.currentTimeMillis();
            mslsRuntimes.add((double) (endTime - startTime));
        }
        double avgMSLSRuntime = mslsRuntimes.stream().mapToDouble(d -> d).average().orElse(10000.0);
        long lnsTimeLimit = (long) avgMSLSRuntime;

        System.out.printf("\n=== Average MSLS runtime: %.2f ms ===\n", avgMSLSRuntime);
        System.out.printf("=== LNS time limit set to: %d ms ===\n", lnsTimeLimit);


        System.out.println("\n=== Phase 2: Running LNS experiments ===");

        // Define LNS configurations
        List<LNSConfig> configurations = new ArrayList<>();
        double[] destructionRates = {0.2, 0.3, 0.4};
        LNSAlgorithm.DestroyHeuristic[] destroyHeuristics = {
            LNSAlgorithm.DestroyHeuristic.RANDOM_REMOVAL,
            LNSAlgorithm.DestroyHeuristic.LONGEST_EDGE_REMOVAL
        };

        for (double rate : destructionRates) {
            for (LNSAlgorithm.DestroyHeuristic destroy : destroyHeuristics) {
                // Version with local search after repair
                configurations.add(new LNSConfig(rate, destroy, true));
                // Version without local search after repair
                configurations.add(new LNSConfig(rate, destroy, false));
            }
        }
        
        int configNum = 0;
        for (LNSConfig config : configurations) {
            configNum++;
            System.out.printf("\n[%d/%d] Running configuration: %s\n",
                    configNum, configurations.size(), config.getFullName());

            List<AlgorithmResult> configResults = runLNSConfiguration(instance, config, lnsTimeLimit);
            allResults.addAll(configResults);
            
            printConfigurationSummary(configResults, config.getFullName());
        }

        return allResults;
    }

    private static List<AlgorithmResult> runLNSConfiguration(Instance instance, LNSConfig config, long timeLimit) {
        List<AlgorithmResult> results = new ArrayList<>();

        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            long seed = System.nanoTime() + run;

            LNSAlgorithm algorithm = new LNSAlgorithm(
                    instance,
                    config.destructionRate,
                    timeLimit,
                    config.useLocalSearchAfterRepair,
                    config.neighborhood,
                    config.destroyHeuristic,
                    seed);
            
            long startTime = System.currentTimeMillis();
            Solution solution = algorithm.solve();
            long endTime = System.currentTimeMillis();
            long computationTime = endTime - startTime;

            int iterations = algorithm.getIterationCount();
            
            // Per assignment, report iterations instead of time in the result object
            AlgorithmResult result = new AlgorithmResult(
                algorithm.getName(),
                instance.getName(),
                solution,
                iterations
            );
            results.add(result);

            System.out.printf("Objective: %d, Iterations: %d, Time: %.2fs\n",
                    result.getObjectiveValue(),
                    iterations,
                    computationTime / 1000.0);
        }
        return results;
    }

    private static void printConfigurationSummary(List<AlgorithmResult> results, String configName) {
        if (results.isEmpty()) return;

        double minScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).min().orElse(0);
        double maxScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).max().orElse(0);
        double avgScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).average().orElse(0);
        // The value in computationTimeMs is iterations, so we calculate average iterations
        double avgIterations = results.stream().mapToDouble(AlgorithmResult::getComputationTimeMs).average().orElse(0);

        System.out.println("\n  === Summary for " + configName + " ===");
        System.out.printf("  Min Score: %.0f\n", minScore);
        System.out.printf("  Max Score: %.0f\n", maxScore);
        System.out.printf("  Avg Score: %.2f\n", avgScore);
        System.out.printf("  Avg Iterations: %.2f\n", avgIterations);
        System.out.printf("  Total Runs: %d\n", results.size());
    }

    public static Map<String, GreedyExperimentRunner.BestSolutionInfo> analyzeBestSolutions(List<AlgorithmResult> results) {
        Map<String, GreedyExperimentRunner.BestSolutionInfo> bestSolutions = new HashMap<>();

        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : results) {
            groupedResults.computeIfAbsent(result.getAlgorithmName(), k -> new ArrayList<>()).add(result);
        }

        for (Map.Entry<String, List<AlgorithmResult>> entry : groupedResults.entrySet()) {
            String algorithmName = entry.getKey();
            List<AlgorithmResult> algorithmResults = entry.getValue();

            AlgorithmResult bestResult = algorithmResults.stream()
                    .min(Comparator.comparingLong(AlgorithmResult::getObjectiveValue))
                    .orElse(null);

            if (bestResult != null) {
                SolutionChecker.ValidationResult validation = SolutionChecker.validateSolution(bestResult.getSolution());
                GreedyExperimentRunner.BestSolutionInfo info = new GreedyExperimentRunner.BestSolutionInfo(
                        bestResult, validation, algorithmResults.size());
                bestSolutions.put(algorithmName, info);
            }
        }
        return bestSolutions;
    }
}
