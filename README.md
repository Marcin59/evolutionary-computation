# TSP Solver with Node Selection

A Java-based solver for a variant of the Traveling Salesman Problem where you must:
- Select exactly 50% of nodes (rounded up if odd)
- Form a Hamiltonian cycle through selected nodes  
- Minimize: total path length + sum of selected node costs

## Problem Description

Given nodes with (x, y) coordinates and costs, the goal is to:
1. Select exactly ⌈n/2⌉ nodes
2. Find the shortest Hamiltonian cycle through selected nodes
3. Minimize: Euclidean path length + sum of node costs
4. Distances are calculated as rounded Euclidean distances

## Project Structure

```
├── analysis/                # Python analysis and visualization
│   ├── TSP_Greedy_Analysis.ipynb    # Comprehensive greedy analysis
│   ├── TSP_Analysis.ipynb           # General TSP analysis
│   ├── requirements.txt             # Python dependencies
│   └── README.md                    # Analysis documentation
├── experiments/             # Java experiments and algorithms
│   ├── src/main/java/tsp/          # Java source code
│   ├── data/                        # Problem instance files
│   ├── pom.xml                      # Maven configuration
│   ├── Dockerfile                   # Docker containerization
│   └── README.md                    # Experiments documentation
├── scripts/                 # Execution scripts
│   ├── run.bat                      # Windows Docker execution
│   ├── run.sh                       # Linux/Mac Docker execution
│   ├── run-experiments.bat          # Legacy Windows script
│   ├── run-experiments.sh           # Legacy Linux/Mac script
│   └── README.md                    # Scripts documentation
├── data/                    # Problem instance files (legacy)
├── results/                 # Generated results (organized by algorithm)
└── [project files]         # Docker, Maven, etc.
```

## Architecture

### Java Components

```
experiments/src/main/java/tsp/
├── core/                    # Core data structures
│   ├── Node.java           # Node with coordinates and cost
│   ├── Instance.java       # Problem instance loader
│   ├── DistanceMatrix.java # Precomputed distance matrix
│   ├── Solution.java       # Abstract solution class
│   ├── TSPSolution.java    # Concrete solution implementation
│   └── Algorithm.java      # Abstract algorithm base class
├── algorithms/              # Algorithm implementations
│   └── greedy/             # Greedy algorithm variants
│   ├── GreedyAlgorithm.java      # Cost-based greedy selection
│   └── GreedyDistanceAlgorithm.java # Balanced cost+distance selection
├── analysis/               # Performance analysis utilities
│   ├── AlgorithmResult.java     # Result data structure
│   ├── ExperimentRunner.java    # Experiment orchestration
│   └── ResultExporter.java      # Export to JSON/CSV for Python
└── Main.java              # Main experiment runner
```

### Python Analysis Components

```
analysis/
├── TSP_Greedy_Analysis.ipynb    # Comprehensive greedy algorithms analysis
├── TSP_Analysis.ipynb           # General TSP analysis and visualization
├── requirements.txt             # Python dependencies
└── README.md                    # Analysis documentation
```

**Features**:
- ✅ Statistical analysis and significance testing
- ✅ Performance comparisons and rankings  
- ✅ 2D visualization of solutions with node costs
- ✅ Algorithm consistency and convergence analysis
- ✅ Configurable algorithm folder loading
- ✅ Export functionality for plots and data

## Key Design Features

### ✅ **Extensible Algorithm Framework**
- Abstract `Algorithm` class makes adding new evolutionary algorithms easy
- Clean separation between node selection and route construction
- Algorithms only access `DistanceMatrix`, not coordinates (as required)

### ✅ **Comprehensive Performance Tracking**
- Automatic computation time measurement
- Objective function breakdown (path length vs node costs)
- Export to multiple formats (JSON, CSV) for Python analysis

### ✅ **Statistical Analysis Ready**
- Multiple runs per algorithm for statistical significance
- Export formats optimized for pandas/numpy analysis
- Built-in summary statistics and performance ranking

### ✅ **Clean Data Flow**
```
CSV Data → Instance → DistanceMatrix → Algorithm → Solution → Results → Python Analysis
```

## Usage

### Docker Only Setup (Recommended)

#### Ultra Quick Start (One command)
```bash
# Windows
scripts\run.bat

# Linux/Mac
chmod +x scripts/run.sh
./scripts/run.sh
```

#### Quick Start (With detailed output)
```bash
# Windows
scripts\run-experiments.bat

# Linux/Mac
chmod +x scripts/run-experiments.sh
./scripts/run-experiments.sh
```

#### Setup Python Environment (One-time)
```bash
# Install Python dependencies
pip install -r analysis/requirements.txt

# Or manually:
pip install pandas numpy matplotlib seaborn scipy jupyter jupyterlab
```

#### Run Analysis
```bash
# Windows
scripts\start-analysis.bat

# Linux/Mac
chmod +x scripts/start-analysis.sh
./scripts/start-analysis.sh

# Or manually:
cd analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

#### Manual Docker Commands

```bash
# Build image (from experiments folder)
cd experiments
docker build -t tsp-solver .

# Run experiments (Windows)
docker run --rm -v "%cd%\results:/app/results" -v "%cd%\data:/app/data" tsp-solver

# Run experiments (Linux/Mac)
docker run --rm -v "$(pwd)/results:/app/results" -v "$(pwd)/data:/app/data" tsp-solver

# Return to root and analyze locally:
cd ..
cd analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

### Full Local Installation (Alternative)

#### Prerequisites
- Java 17 (LTS) - Download from [Adoptium](https://adoptium.net/temurin/releases/?version=17)
- Maven 3.6+
- Python 3.8+ with pip

#### Run Experiments
```bash
# Compile and run experiments (from experiments folder)
cd experiments
mvn clean compile exec:java -Dexec.mainClass="tsp.Main"
```

This will:
- Load `data/TSPA.csv` and `data/TSPB.csv`
- Run each algorithm 30 times on each instance
- Export results to `results/` directory in JSON and CSV formats

#### Analyze Results in Python

Install dependencies and run the notebook:
```bash
pip install -r analysis/requirements.txt
cd analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

The notebook will:
- Load experiment results from JSON files
- Perform statistical analysis and significance testing
- Generate visualizations and performance comparisons
- Export summary reports and rankings

## Current Algorithms

### 1. **RandomAlgorithm**
- Randomly selects 50% of nodes
- Creates random route through selected nodes
- Baseline for comparison

### 2. **GreedyAlgorithm** 
- Selects nodes with lowest costs
- Uses nearest neighbor for route construction
- Pure cost optimization

### 3. **GreedyDistanceAlgorithm**
- Balances node cost (70%) and centrality (30%)
- Starts route from most central node
- More sophisticated selection strategy

## Adding New Algorithms

To add evolutionary algorithms, extend the `Algorithm` class:

```java
public class GeneticAlgorithm extends Algorithm {
    public GeneticAlgorithm(Instance instance) {
        super("Genetic", instance);
    }
    
    @Override
    public Solution solve() {
        // Your evolutionary algorithm implementation
        // Return TSPSolution with selected nodes and route
    }
}
```

Then add to the experiment runner in `Main.java`:

```java
List<Supplier<Algorithm>> algorithms = List.of(
    () -> new RandomAlgorithm(instance),
    () -> new GreedyAlgorithm(instance),
    () -> new GreedyDistanceAlgorithm(instance),
    () -> new GeneticAlgorithm(instance)  // Add here
);
```

## Results Structure

### JSON Export Format
```json
{
  "timestamp": "2025-01-01T12:00:00",
  "results": [
    {
      "algorithm": "Greedy",
      "instance": "TSPA", 
      "objective_value": 12500.5,
      "path_length": 8000.3,
      "node_costs": 4500.2,
      "computation_time_ms": 15,
      "selected_nodes_count": 101,
      "total_nodes": 201
    }
  ],
  "summary": {
    "total_runs": 90,
    "algorithms": { /* per-algorithm statistics */ }
  }
}
```

### Python Analysis Output
- Statistical significance tests between algorithms
- Performance rankings and recommendations
- Convergence analysis and consistency metrics
- Instance comparison and algorithm characteristics
- Comprehensive visualizations and summary reports

## Future Extensions

The architecture supports easy addition of:
- **Evolutionary Algorithms**: Genetic algorithms, evolution strategies
- **Local Search**: 2-opt, 3-opt improvements
- **Hybrid Approaches**: Combining different selection and routing strategies
- **Multi-objective Optimization**: Pareto-optimal solutions
- **Parameter Tuning**: Automated parameter optimization

## Dependencies

### Docker Only Setup (Recommended)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) for Windows/Mac, or Docker Engine for Linux
- Python 3.8+ with pip for analysis
- Dependencies listed in `requirements.txt`

### Local Installation (Alternative)
#### Java (Maven)
- Java 17 (LTS) - [Download from Adoptium](https://adoptium.net/temurin/releases/?version=17)
- Maven 3.6+
- Jackson for JSON processing
- Apache Commons Math for statistics
- JUnit for testing

#### Python (Jupyter)
- pandas, numpy for data analysis
- matplotlib, seaborn for visualization  
- scipy for statistical testing
- jupyter, jupyterlab for interactive analysis

*Install Python dependencies with: `pip install -r requirements.txt`*

## Data Format

CSV files with semicolon separation:
```
x_coordinate;y_coordinate;node_cost
1355;1796;496
2524;387;414
...
```

This architecture provides a solid foundation for evolutionary computation research on the TSP variant, with excellent analysis capabilities and easy extensibility for new algorithms.