package tsp.analysis;

import tsp.core.Algorithm;
import tsp.core.Instance;
import tsp.core.Solution;
import tsp.algorithms.MSLS_ILS.AlgorithmILS;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class for running experiments and collecting performance data.
 */
public class ExperimentRunner {
    
    /**
     * Run an algorithm on an instance and measure performance.
     */
    public static AlgorithmResult runSingle(Algorithm algorithm) {
        long startTime = System.currentTimeMillis();
        Solution solution = algorithm.solve();
        long endTime = System.currentTimeMillis();
        
        long computationTime = endTime - startTime;

        // If this is an ILS algorithm, use the number of basic local search calls
        // as the reported "computation time" (keeps the CSV column name unchanged).
        if (algorithm instanceof AlgorithmILS) {
            computationTime = ((AlgorithmILS) algorithm).getLocalSearchCallCount();
        }

        return new AlgorithmResult(
            algorithm.getName(),
            algorithm.getInstance().getName(),
            solution,
            computationTime
        );
    }
    
    /**
     * Run an algorithm multiple times and collect all results.
     */
    public static List<AlgorithmResult> runMultiple(Supplier<Algorithm> algorithmSupplier, int runs) {
        List<AlgorithmResult> results = new ArrayList<>();
        
        for (int i = 0; i < runs; i++) {
            Algorithm algorithm = algorithmSupplier.get();
            AlgorithmResult result = runSingle(algorithm);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Run multiple algorithms on the same instance.
     */
    public static List<AlgorithmResult> runComparison(Instance instance, List<Supplier<Algorithm>> algorithmSuppliers) {
        List<AlgorithmResult> results = new ArrayList<>();
        
        for (Supplier<Algorithm> supplier : algorithmSuppliers) {
            Algorithm algorithm = supplier.get();
            AlgorithmResult result = runSingle(algorithm);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Run comprehensive experiment: multiple algorithms, multiple runs each.
     */
    public static List<AlgorithmResult> runFullExperiment(Instance instance, 
                                                         List<Supplier<Algorithm>> algorithmSuppliers, 
                                                         int runsPerAlgorithm) {
        List<AlgorithmResult> allResults = new ArrayList<>();
        
        for (Supplier<Algorithm> supplier : algorithmSuppliers) {
            System.out.println("Running algorithm: " + supplier.get().getName());
            
            for (int run = 0; run < runsPerAlgorithm; run++) {
                Algorithm algorithm = supplier.get();
                AlgorithmResult result = runSingle(algorithm);
                allResults.add(result);
                
                if ((run + 1) % 10 == 0) {
                    System.out.printf("  Completed %d/%d runs\n", run + 1, runsPerAlgorithm);
                }
            }
        }
        
        return allResults;
    }
}