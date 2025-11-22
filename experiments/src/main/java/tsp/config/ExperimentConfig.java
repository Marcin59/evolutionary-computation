package tsp.config;

/**
 * Configuration class for TSP experiments.
 * This class centralizes experiment configuration to make it easy to switch between different algorithm types.
 */
public class ExperimentConfig {
    
    // Algorithm type configuration
    public static final String ALGORITHM_TYPE = "deltas"; // Change this to switch algorithm types
    // Supported types: "greedy", "regret", "local_search", "candidate_moves", "deltas"
    
    // Directory configuration
    public static final String DATA_DIR = "data";
    public static final String RESULTS_DIR = "results";
    
    // Instance configuration
    public static final String[] INSTANCE_FILES = {"TSPA.csv", "TSPB.csv"};
    
    // Experiment parameters
    public static final int RUNS_PER_STARTING_NODE = 200;
    
    // Greedy algorithm parameters
    public static final boolean ENABLE_VALIDATION = true;
    public static final boolean EXPORT_CSV = true;
    public static final boolean EXPORT_VISUALIZATION = true;
    public static final boolean EXPORT_SUMMARY = true;
    
    /**
     * Get the full results directory path for the current algorithm type.
     */
    public static String getAlgorithmResultsDir() {
        return RESULTS_DIR + "/" + ALGORITHM_TYPE;
    }
    
    /**
     * Get experiment description for console output.
     */
    public static String getExperimentDescription() {
        return "TSP " + ALGORITHM_TYPE.toUpperCase() + " Algorithms Experiment Runner";
    }
    
    /**
     * Validate configuration settings.
     */
    public static void validateConfig() {
        if (ALGORITHM_TYPE == null || ALGORITHM_TYPE.trim().isEmpty()) {
            throw new IllegalArgumentException("ALGORITHM_TYPE cannot be null or empty");
        }
        
        if (RUNS_PER_STARTING_NODE <= 0) {
            throw new IllegalArgumentException("RUNS_PER_STARTING_NODE must be positive");
        }
        
        if (INSTANCE_FILES == null || INSTANCE_FILES.length == 0) {
            throw new IllegalArgumentException("INSTANCE_FILES cannot be null or empty");
        }
    }
    
    /**
     * Print current configuration to console.
     */
    public static void printConfig() {
        System.out.println("=== Experiment Configuration ===");
        System.out.println("Algorithm Type: " + ALGORITHM_TYPE);
        System.out.println("Results Directory: " + getAlgorithmResultsDir());
        System.out.println("Runs per Starting Node: " + RUNS_PER_STARTING_NODE);
        System.out.println("Validation Enabled: " + ENABLE_VALIDATION);
        System.out.println("Instance Files: " + String.join(", ", INSTANCE_FILES));
        System.out.println("=================================");
    }
}