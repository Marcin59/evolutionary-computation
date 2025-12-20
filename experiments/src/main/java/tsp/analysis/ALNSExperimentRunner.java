package tsp.analysis;

import tsp.algorithms.LNS.ALNSAlgorithm;
import tsp.algorithms.MSLS_ILS.AlgorithmMSLS;
import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.core.Algorithm;
import tsp.core.Instance;
import tsp.core.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Experiment runner for Adaptive Large Neighborhood Search (ALNS).
 *
 * Tests multiple ALNS configurations and compares performance.
 * Time limit is calibrated using MSLS runtime (same as LNS/HEA experiments).
 */
public class ALNSExperimentRunner {

    // Number of runs per configuration per instance
    private static final int RUNS_PER_CONFIG = 20;

    /**
     * Configuration for ALNS experiments.
     */
    private static class ALNSConfig {
        final double destructionRate;
        final double reactionFactor;
        final int segmentLength;
        final double coolingRate;
        final boolean useLocalSearchAfterRepair;
        final LocalSearchAlgorithm.Neighborhood neighborhood;

        ALNSConfig(double destructionRate, double reactionFactor, int segmentLength,
                   double coolingRate, boolean useLocalSearchAfterRepair) {
            this.destructionRate = destructionRate;
            this.reactionFactor = reactionFactor;
            this.segmentLength = segmentLength;
            this.coolingRate = coolingRate;
            this.useLocalSearchAfterRepair = useLocalSearchAfterRepair;
            this.neighborhood = LocalSearchAlgorithm.Neighborhood.TWO_OPT;
        }

        String getFullName() {
            return String.format("ALNS_d-%.2f_rho-%.2f_seg-%d_cool-%.4f_ls-%s",
                    destructionRate, reactionFactor, segmentLength, coolingRate,
                    useLocalSearchAfterRepair ? "On" : "Off");
        }
    }

    public static List<AlgorithmResult> runAllExperiments(Instance instance) {
        List<AlgorithmResult> allResults = new ArrayList<>();

        System.out.println("Running ALNS experiments for instance: " + instance.getName());
        System.out.println("Runs per configuration: " + RUNS_PER_CONFIG);

        // Phase 1: Calibrate time limit using MSLS
        System.out.println("\n=== Phase 1: Calculating average MSLS runtime for time limit ===");
        long alnsTimeLimit = calibrateMSLSRuntime(instance);
        System.out.printf("=== ALNS time limit set to: %d ms ===\n", alnsTimeLimit);

        // Phase 2: Define configurations to test
        System.out.println("\n=== Phase 2: Running ALNS experiments ===");
        List<ALNSConfig> configurations = defineConfigurations();

        // Phase 3: Run experiments
        int configNum = 0;
        for (ALNSConfig config : configurations) {
            configNum++;
            System.out.printf("\n[%d/%d] Running configuration: %s\n",
                    configNum, configurations.size(), config.getFullName());

            List<AlgorithmResult> configResults = runALNSConfiguration(instance, config, alnsTimeLimit);
            allResults.addAll(configResults);

            printConfigurationSummary(configResults, config.getFullName());
        }

        // Phase 4: Print overall summary
        printOverallSummary(allResults);

        return allResults;
    }

    private static long calibrateMSLSRuntime(Instance instance) {
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
        System.out.printf("Average MSLS runtime: %.2f ms\n", avgMSLSRuntime);

        return (long) avgMSLSRuntime;
    }

    private static List<ALNSConfig> defineConfigurations() {
        List<ALNSConfig> configs = new ArrayList<>();

        // Configuration 1: Default balanced
        // destructionRate=0.25, rho=0.1, segment=100, cooling=0.9995
        configs.add(new ALNSConfig(0.25, 0.1, 100, 0.9995, true));

        // Configuration 2: More destruction, slower adaptation
        configs.add(new ALNSConfig(0.35, 0.1, 100, 0.9995, true));

        // Configuration 3: Faster adaptation, smaller segments
        configs.add(new ALNSConfig(0.25, 0.2, 50, 0.9995, true));

        // Configuration 4: No cooling (pure adaptive)
        configs.add(new ALNSConfig(0.30, 0.15, 75, 1.0, true));

        // Configuration 5: Aggressive destruction with slow cooling
        configs.add(new ALNSConfig(0.40, 0.1, 100, 0.999, true));

        return configs;
    }

    private static List<AlgorithmResult> runALNSConfiguration(Instance instance, ALNSConfig config, long timeLimit) {
        List<AlgorithmResult> results = new ArrayList<>();

        for (int run = 0; run < RUNS_PER_CONFIG; run++) {
            System.out.printf("  Run %d/%d... ", run + 1, RUNS_PER_CONFIG);
            long seed = System.nanoTime() + run;

            ALNSAlgorithm algorithm = new ALNSAlgorithm(
                    instance,
                    config.destructionRate,
                    timeLimit,
                    config.useLocalSearchAfterRepair,
                    config.neighborhood,
                    seed,
                    config.reactionFactor,
                    config.segmentLength,
                    config.coolingRate,
                    10.0,  // sigma1 - reward for new global best
                    5.0,   // sigma2 - reward for improving current
                    2.0,  // sigma3 - reward for accepting worse
                    0.05    // minWeight - prevent operator starvation
            );

            long startTime = System.currentTimeMillis();
            Solution solution = algorithm.solve();
            long endTime = System.currentTimeMillis();
            long computationTime = endTime - startTime;

            int iterations = algorithm.getIterationCount();
            double[] destroyWeights = algorithm.getDestroyWeights();
            double[] repairWeights = algorithm.getRepairWeights();

            AlgorithmResult result = new AlgorithmResult(
                    algorithm.getName(),
                    instance.getName(),
                    solution,
                    iterations
            );
            results.add(result);

            System.out.printf("Obj: %d, Iters: %d, Time: %.2fs, Destroy:[%.2f,%.2f,%.2f] Repair:[%.2f,%.2f]\n",
                    result.getObjectiveValue(),
                    iterations,
                    computationTime / 1000.0,
                    destroyWeights[0], destroyWeights[1], destroyWeights[2],
                    repairWeights[0], repairWeights[1]);
        }
        return results;
    }

    private static void printConfigurationSummary(List<AlgorithmResult> results, String configName) {
        if (results.isEmpty()) return;

        double minScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).min().orElse(0);
        double maxScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).max().orElse(0);
        double avgScore = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).average().orElse(0);
        double avgIterations = results.stream().mapToDouble(AlgorithmResult::getComputationTimeMs).average().orElse(0);

        System.out.println("\n  === Summary for " + configName + " ===");
        System.out.printf("  Min Score: %.0f\n", minScore);
        System.out.printf("  Max Score: %.0f\n", maxScore);
        System.out.printf("  Avg Score: %.2f\n", avgScore);
        System.out.printf("  Avg Iterations: %.2f\n", avgIterations);
        System.out.printf("  Total Runs: %d\n", results.size());
    }

    private static void printOverallSummary(List<AlgorithmResult> allResults) {
        System.out.println("\n========================================");
        System.out.println("=== OVERALL ALNS EXPERIMENT SUMMARY ===");
        System.out.println("========================================");

        // Group by algorithm name
        Map<String, List<AlgorithmResult>> groupedResults = new HashMap<>();
        for (AlgorithmResult result : allResults) {
            groupedResults.computeIfAbsent(result.getAlgorithmName(), k -> new ArrayList<>()).add(result);
        }

        // Find best configuration
        String bestConfig = null;
        double bestAvg = Double.MAX_VALUE;

        for (Map.Entry<String, List<AlgorithmResult>> entry : groupedResults.entrySet()) {
            String name = entry.getKey();
            List<AlgorithmResult> results = entry.getValue();

            double min = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).min().orElse(0);
            double max = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).max().orElse(0);
            double avg = results.stream().mapToLong(AlgorithmResult::getObjectiveValue).average().orElse(0);

            System.out.printf("\n%s:\n", name);
            System.out.printf("  Min: %.0f, Avg: %.2f, Max: %.0f\n", min, avg, max);

            if (avg < bestAvg) {
                bestAvg = avg;
                bestConfig = name;
            }
        }

        System.out.println("\n=== BEST CONFIGURATION: " + bestConfig + " ===");
        System.out.printf("=== Best Average Score: %.2f ===\n", bestAvg);
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
