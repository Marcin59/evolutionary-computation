package tsp;

import tsp.analysis.*;
import tsp.config.*;
import tsp.core.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class to run TSP experiments and generate results for analysis.
 */
public class Main {
    
    public static void main(String[] args) {
        try {
            // Validate and print configuration
            ExperimentConfig.validateConfig();
            System.out.println("=== " + ExperimentConfig.getExperimentDescription() + " ===");
            ExperimentConfig.printConfig();
            
            // Create results directory
            Path resultsDir = Paths.get(ExperimentConfig.RESULTS_DIR);
            if (!resultsDir.toFile().exists()) {
                resultsDir.toFile().mkdirs();
                System.out.println("Created results directory: " + resultsDir.toAbsolutePath());
            }
            
            // Load instances
            List<Instance> instances = loadInstances();
            
            // Run algorithm experiments for each instance
            for (Instance instance : instances) {
                System.out.println("\n--- Processing instance: " + instance.getName() + " ---");
                System.out.println("Total nodes: " + instance.getTotalNodes());
                System.out.println("Required nodes: " + instance.getRequiredNodes());
                
                AlgorithmExperimentRunner.runExperiments(instance, ExperimentConfig.ALGORITHM_TYPE, resultsDir);
            }
            
            System.out.println("\n=== Experiments completed! ===");
            System.out.println("Algorithm type: " + ExperimentConfig.ALGORITHM_TYPE);
            System.out.println("Results saved to: " + resultsDir.resolve(ExperimentConfig.ALGORITHM_TYPE).toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error running experiments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load all instances from the data directory.
     */
    private static List<Instance> loadInstances() throws Exception {
        List<Instance> instances = new ArrayList<>();
        
        // Load instances based on configuration
        for (String fileName : ExperimentConfig.INSTANCE_FILES) {
            Path filePath = Paths.get(ExperimentConfig.DATA_DIR, fileName);
            System.out.println("Loading instance: " + filePath);
            
            Instance instance = Instance.fromFile(filePath);
            instances.add(instance);
            
            System.out.println("Loaded: " + instance);
        }
        
        return instances;
    }
}