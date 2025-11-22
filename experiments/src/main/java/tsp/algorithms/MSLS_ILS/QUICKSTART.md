# Quick Start Guide - MSLS and ILS Experiments

## Overview
This guide shows how to run MSLS (Multiple Start Local Search) and ILS (Iterated Local Search) experiments.

## Running Experiments

### Option 1: Using Docker (Recommended)
```bash
# Navigate to project root
cd c:\Users\marci\Desktop\University\evolutionary-computation

# Run MSLS and ILS experiments
.\scripts\run-experiments.bat msls_ils
```

This will:
- Build the Docker container
- Run all 8 configurations (4 MSLS + 4 ILS)
- 10 runs per configuration per instance
- Save results to `results/msls_ils/`

### Option 2: Using Maven Directly
```bash
# Build the project
mvn clean compile

# Run experiments
mvn exec:java -Dexec.mainClass="tsp.analysis.ExperimentMain" -Dexec.args="msls_ils"
```

## Experiment Configurations

### MSLS Configurations (4 total)
1. **MSLS_STEEPEST_Nodes** - Steepest descent with node swaps
2. **MSLS_STEEPEST_Edges** - Steepest descent with 2-opt moves
3. **MSLS_GREEDY_Nodes** - First improvement with node swaps
4. **MSLS_GREEDY_Edges** - First improvement with 2-opt moves

### ILS Configurations (4 total)
1. **ILS_STEEPEST_Nodes** - Steepest descent with node swaps
2. **ILS_STEEPEST_Edges** - Steepest descent with 2-opt moves
3. **ILS_GREEDY_Nodes** - First improvement with node swaps
4. **ILS_GREEDY_Edges** - First improvement with 2-opt moves

## Parameters

- **Time Limit:** 438.46 seconds (7.3 minutes) per run
- **Runs per Config:** 10 per instance (TSPA and TSPB)
- **Total Runs:** 160 (8 configs × 2 instances × 10 runs)
- **Total Time:** ~19.5 hours for all experiments

### MSLS Specific
- **Max Iterations:** 10,000 (will be limited by time)
- **Starting Solutions:** Random

### ILS Specific
- **Perturbation Strength:** 10% of route length (minimum 5 moves)
- **Starting Solution:** Random
- **Perturbation Types:** Random swaps, 2-opt moves, node replacements

## Expected Output

### Console Output
```
Running MSLS and ILS experiments for instance: TSPA
Total nodes: 200
Required nodes: 100
Time limit per run: 438.46 seconds
Runs per configuration: 10

[1/8] Running configuration: MSLS_STEEPEST_Nodes
  Run 1/10... Objective: 69540, Time: 438.12s
  Run 2/10... Objective: 70123, Time: 438.28s
  ...

  === Summary for MSLS_STEEPEST_Nodes ===
  Min Score: 69540
  Max Score: 71234
  Avg Score: 70112.34
  Avg Time: 438.21 seconds
  Total Runs: 10
```

### Results Files
Results are saved to `results/msls_ils/`:
- `TSPA_msls_ils_results.json` - Detailed results in JSON format
- `TSPA_msls_ils_results.csv` - Results in CSV format
- `TSPA_summary.json` - Summary statistics
- `TSPA_visualization.json` - Best solution visualizations
- (Same for TSPB)

## Analyzing Results

### Generate Report
```bash
cd analysis
python generate_report.py msls_ils --output Lab6 --title "MSLS and ILS Algorithms for TSP"
```

This generates:
- `Lab6/TSP_msls_ils_report.md` - Markdown report
- `Lab6/images/` - Visualization images

### View Results in Jupyter
```bash
cd analysis
jupyter lab
# Open TSP_Analysis.ipynb
```

## Troubleshooting

### Issue: Time Limit Not Respected
**Solution:** The algorithms check time limit frequently. If exceeded slightly, it's due to completing current operation.

### Issue: Out of Memory
**Solution:** Reduce number of parallel runs or increase Docker memory allocation.

### Issue: No Results Generated
**Solution:** Check Docker logs and ensure data files exist in `data/` directory.

## Performance Tips

### For Faster Results (Testing)
Reduce time limit in `MSLS_ILS_ExperimentRunner.java`:
```java
private static final long TIME_LIMIT_MS = 60000;  // 1 minute
```

### For Better Quality
Increase perturbation strength in ILS:
```java
int perturbationStrength = instance.getRequiredNodes() / 5;  // 20% instead of 10%
```

## Comparing with Previous Labs

The results can be compared with:
- Lab 3: Local Search (steepest/greedy)
- Lab 4: Candidate Moves
- Lab 5: Delta Caching

Expected improvements:
- MSLS should outperform single-start local search
- ILS should find competitive or better solutions than MSLS
- Both should handle the 438s time limit efficiently

## Next Steps

After experiments complete:
1. Generate the report using `generate_report.py`
2. Analyze the best solutions found
3. Compare MSLS vs ILS performance
4. Compare steepest vs greedy variants
5. Compare nodes vs edges neighborhoods
6. Document findings in the lab report
