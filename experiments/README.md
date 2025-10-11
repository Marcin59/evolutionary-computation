# Experiments Directory

This directory contains all Java source code and configuration for running TSP experiments.

## Structure

```
experiments/
├── src/main/java/tsp/              # Java source code
│   ├── Main.java                   # Main experiment runner
│   ├── core/                       # Core TSP data structures
│   │   ├── Node.java              # Node with coordinates and cost
│   │   ├── Instance.java          # Problem instance loader
│   │   ├── DistanceMatrix.java    # Precomputed distance matrix
│   │   ├── Solution.java          # Abstract solution class
│   │   ├── TSPSolution.java       # Concrete solution implementation
│   │   └── Algorithm.java         # Abstract algorithm base class
│   ├── algorithms/                 # Algorithm implementations
│   │   └── greedy/                # Greedy algorithm variants
│   │       ├── RandomSolutionAlgorithm.java
│   │       ├── NearestNeighborEndAlgorithm.java
│   │       ├── NearestNeighborAnyPositionAlgorithm.java
│   │       └── GreedyCycleAlgorithm.java
│   ├── analysis/                   # Experiment orchestration
│   │   ├── AlgorithmExperimentRunner.java
│   │   ├── GreedyExperimentRunner.java
│   │   ├── ResultExporter.java
│   │   ├── SolutionChecker.java
│   │   └── VisualizationExporter.java
│   └── config/                     # Configuration
│       └── ExperimentConfig.java  # Centralized experiment settings
├── data/                           # Problem instance files
│   ├── TSPA.csv
│   └── TSPB.csv
├── pom.xml                         # Maven build configuration
└── Dockerfile                      # Docker containerization
```

## Configuration

### Algorithm Type Configuration
Edit `src/main/java/tsp/config/ExperimentConfig.java`:

```java
public static final String ALGORITHM_TYPE = "greedy"; // Change this line
```

Supported algorithm types:
- `"greedy"` - Run greedy heuristics (currently implemented)
- `"genetic"` - Run genetic algorithms (future implementation)
- `"simulated_annealing"` - Run simulated annealing (future implementation)

### Other Configuration Options
```java
// Instance files to process
public static final String[] INSTANCE_FILES = {"TSPA.csv", "TSPB.csv"};

// Number of runs per starting node (for greedy algorithms)
public static final int RUNS_PER_STARTING_NODE = 200;

// Export options
public static final boolean ENABLE_VALIDATION = true;
public static final boolean EXPORT_CSV = true;
public static final boolean EXPORT_VISUALIZATION = true;
public static final boolean EXPORT_SUMMARY = true;
```

## Building and Running

### Using Docker (Recommended)
```bash
# From the experiments directory
docker build -t tsp-solver .
docker run -v "${PWD}/../results:/app/results" tsp-solver

# Or from the root directory using scripts
../scripts/run.bat      # Windows
../scripts/run.sh       # Linux/Mac
```

### Using Maven Directly
```bash
# From the experiments directory
mvn clean compile exec:java -Dexec.mainClass="tsp.Main"
```

### Using IDE
1. Import the `experiments` folder as a Maven project
2. Set main class: `tsp.Main`
3. Run the project

## Algorithm Implementation

### Adding New Algorithms

1. **Create algorithm class** in `src/main/java/tsp/algorithms/{algorithm_type}/`
   ```java
   public class YourAlgorithm extends Algorithm {
       @Override
       public Solution solve(Instance instance) {
           // Your implementation
       }
   }
   ```

2. **Create experiment runner** following the pattern of `GreedyExperimentRunner.java`

3. **Update AlgorithmExperimentRunner.java** to support your new algorithm:
   ```java
   case "your_algorithm_type":
       results = YourExperimentRunner.runExperiments(instance);
       bestSolutions = YourExperimentRunner.analyzeBestSolutions(results);
       break;
   ```

4. **Update ExperimentConfig.java**:
   ```java
   public static final String ALGORITHM_TYPE = "your_algorithm_type";
   ```

## Output

Results are automatically organized by algorithm type:
```
../results/
├── greedy/
│   ├── TSPA_greedy_results.json
│   ├── TSPA_visualization.json
│   ├── TSPA_summary.json
│   └── ...
├── genetic/                    # Future algorithm type
└── simulated_annealing/       # Future algorithm type
```

## Analysis

After running experiments, use the Jupyter notebooks in `../analysis/` to analyze results:

```bash
cd ../analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

## Dependencies

- **Java 17** (LTS) - Required for compilation and execution
- **Maven 3.6+** - For dependency management and building
- **Docker** (optional) - For containerized execution
- **Jackson** - JSON serialization (managed by Maven)
- **Apache Commons Math** - Statistical computations (managed by Maven)

## Features

- ✅ **Modular Design** - Easy to extend with new algorithms
- ✅ **Comprehensive Validation** - Automatic solution correctness checking
- ✅ **Multiple Export Formats** - JSON, CSV, and visualization data
- ✅ **Docker Support** - Consistent execution environment
- ✅ **Statistical Analysis** - Built-in performance metrics
- ✅ **Configurable Parameters** - Centralized experiment settings