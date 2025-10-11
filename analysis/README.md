# Analysis Directory

This directory contains all Python-related files for analyzing TSP experiment results.

## Files

### Jupyter Notebooks
- **`TSP_Greedy_Analysis.ipynb`** - Comprehensive analysis of greedy algorithm results
- **`TSP_Analysis.ipynb`** - General TSP analysis notebook

### Python Modules
- **`utils.py`** - Reusable analysis functions for all algorithm types
- **`utils_README.md`** - Detailed documentation for the utils module

### Dependencies
- **`requirements.txt`** - Python package dependencies for analysis

## Setup

### Install Dependencies
```bash
# Create virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r analysis/requirements.txt
```

### Launch Jupyter
```bash
cd analysis
jupyter notebook
# or
jupyter lab
```

## Notebooks Overview

### TSP_Greedy_Analysis.ipynb
**Purpose**: Analyze results from greedy algorithms experiments

**Features**:
- ✅ Configurable algorithm folder loading
- ✅ 2D visualization of best solutions
- ✅ Statistical analysis and comparisons
- ✅ Solution validation results
- ✅ Export functionality for plots and data
- ✅ Performance benchmarking

**Configuration**:
```python
# Change this to analyze different algorithm results
ALGORITHM_FOLDER = "greedy"  # or "genetic", "simulated_annealing", etc.
```

**Expected Data Structure**:
```
results/
├── greedy/
│   ├── TSPA_greedy_results.json
│   ├── TSPA_visualization.json
│   ├── TSPA_summary.json
│   └── ...
```

### TSP_Analysis.ipynb
**Purpose**: General TSP analysis and visualization

### utils.py Module
**Purpose**: Reusable analysis functions for all algorithm types

**Key Features**:
- ✅ Data loading functions for any algorithm folder
- ✅ Visualization functions (2D plots, distributions, time analysis)
- ✅ Statistical analysis functions (t-tests, ANOVA, ranking)
- ✅ Export functionality (VisualizationExporter class)
- ✅ Convenience functions for discovering available data

**Usage**:
```python
from utils import *

# Load any algorithm type results
data = load_all_algorithm_results("genetic", ["TSPA", "TSPB"])

# Use consistent visualization functions
plot_best_solutions(data)
plot_objective_distributions(data)

# Perform statistical analysis
perform_statistical_analysis(data)
```

See `utils_README.md` for detailed documentation.

## Usage Workflow

1. **Run Experiments**: Use scripts in `../scripts/` to generate results
2. **Configure Analysis**: Set `ALGORITHM_FOLDER` in notebooks
3. **Run Analysis**: Execute all cells in Jupyter notebooks
4. **Export Results**: Use built-in export functionality

## Dependencies

The `requirements.txt` includes:
- `jupyter` - Interactive notebooks
- `pandas` - Data analysis and manipulation
- `numpy` - Numerical computing
- `matplotlib` - Plotting and visualization
- `seaborn` - Statistical data visualization
- `scipy` - Scientific computing

## Output

Analysis notebooks generate:
- **Interactive visualizations** - 2D plots of solutions and routes
- **Statistical summaries** - Performance comparisons and metrics
- **Exported plots** - High-resolution images (PNG/PDF/SVG)
- **Data exports** - JSON and CSV files for further analysis

## Tips

- **Algorithm Switching**: Change `ALGORITHM_FOLDER` to analyze different algorithm types
- **Custom Instances**: Modify `INSTANCES` list to analyze specific problem instances
- **Export Control**: Use `EXPORT_ENABLED` to toggle automatic file exports
- **Plot Quality**: Adjust `PLOT_DPI` for publication-quality images