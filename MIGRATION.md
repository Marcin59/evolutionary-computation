# Project Structure Migration Guide

## What Changed

The project has been reorganized for better structure and maintainability:

### File Movements

#### Java Code → `experiments/` folder
- `src/` → `experiments/src/`
- `pom.xml` → `experiments/pom.xml`
- `Dockerfile` → `experiments/Dockerfile`
- `data/` → `experiments/data/`
- All Java source code and build configuration moved to experiments

#### Scripts → `scripts/` folder
- `run.bat` → `scripts/run.bat`
- `run.sh` → `scripts/run.sh`
- All execution scripts are now in the `scripts/` directory

#### Python Analysis → `analysis/` folder  
- `TSP_Greedy_Analysis.ipynb` → `analysis/TSP_Greedy_Analysis.ipynb`
- `TSP_Analysis.ipynb` → `analysis/TSP_Analysis.ipynb`
- `requirements.txt` → `analysis/requirements.txt`
- All Python-related files are now in the `analysis/` directory

## Updated Commands

### Before (Old)
```bash
# Running experiments
run.bat                    # Windows
./run.sh                   # Linux/Mac

# Python setup
pip install -r requirements.txt

# Jupyter analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

### After (New)
```bash
# Running experiments
scripts\run.bat            # Windows
./scripts/run.sh           # Linux/Mac

# Direct Java development (from experiments folder)
cd experiments
mvn clean compile exec:java -Dexec.mainClass="tsp.Main"

# Python setup
pip install -r analysis/requirements.txt

# Jupyter analysis
cd analysis
jupyter lab TSP_Greedy_Analysis.ipynb
```

## Benefits

- ✅ **Better Organization**: Clear separation of concerns (experiments vs analysis vs scripts)
- ✅ **Cleaner Root**: Minimal files in the main directory
- ✅ **Self-contained Experiments**: All Java code and dependencies in one folder
- ✅ **Documentation**: Each folder has its own README
- ✅ **Scalability**: Easy to add more experiment types or analysis tools

## Migration Steps

If you have existing setups:

1. **Update script calls**: Scripts now automatically navigate to experiments folder
2. **Java development**: Work from the `experiments/` directory for Maven/IDE
3. **Update Python environment**: Point to `analysis/requirements.txt`
4. **Update notebook paths**: Navigate to `analysis/` folder first
5. **Update bookmarks/shortcuts**: Point to new file locations
6. **Configuration**: Still use `experiments/src/main/java/tsp/config/ExperimentConfig.java`

## No Changes Required

- ✅ **Docker execution**: Still works the same way
- ✅ **Java source code**: No changes to src/ directory
- ✅ **Data files**: Still in the same `data/` location
- ✅ **Results output**: Still saves to `results/` directory
- ✅ **Configuration**: Still use `ExperimentConfig.java` to change algorithm types