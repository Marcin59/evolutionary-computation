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
├── requirements.txt             # Python dependencies
└── README.md                    # Analysis documentation
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

## Data Format

CSV files with semicolon separation:
```
x_coordinate;y_coordinate;node_cost
1355;1796;496
2524;387;414
...
```

This architecture provides a solid foundation for evolutionary computation research on the TSP variant, with excellent analysis capabilities and easy extensibility for new algorithms.