# Scripts Directory

This directory contains all scripts for running TSP experiments and managing the project.

## Files

### Main Execution Scripts
- **`run.bat`** - Windows batch script to build and run TSP experiments using Docker
- **`run.sh`** - Linux/Mac shell script to build and run TSP experiments using Docker

### Legacy Scripts
- **`run-experiments.bat`** - Legacy Windows script (kept for compatibility)
- **`run-experiments.sh`** - Legacy Linux/Mac script (kept for compatibility)

## Usage

### Windows
```cmd
scripts\run.bat
```

### Linux/Mac
```bash
scripts/run.sh
```

## What These Scripts Do

1. **Navigate to experiments**: Changes to the `../experiments/` directory
2. **Build Docker Image**: Creates a containerized environment with Java and Maven
3. **Run Experiments**: Executes the TSP algorithm experiments
4. **Export Results**: Saves results to the `../results/` directory
5. **Organize Output**: Results are organized by algorithm type (e.g., `../results/greedy/`)

## Requirements

- Docker installed and running
- Sufficient disk space for results output
- Network access for Docker image building (if not cached)

## Output

Results are saved to:
```
../results/
├── greedy/
│   ├── TSPA_greedy_results.json
│   ├── TSPA_visualization.json
│   ├── TSPA_summary.json
│   └── ...
└── [other algorithm types]/
```

Use the Jupyter notebooks in the `../analysis/` directory to analyze the generated results.