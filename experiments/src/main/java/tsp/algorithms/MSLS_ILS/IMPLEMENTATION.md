# MSLS and ILS Algorithm Implementation

## Overview
Implementation of Multiple Start Local Search (MSLS) and Iterated Local Search (ILS) algorithms for the Traveling Salesman Problem (TSP) as per Assignment 6 requirements.

## Implemented Algorithms

### 1. AlgorithmMSLS (Multiple Start Local Search)
**Location:** `experiments/src/main/java/tsp/algorithms/MSLS_ILS/AlgorithmMSLS.java`

**Description:**
- Generates multiple random starting solutions
- Applies local search to each starting solution
- Returns the best solution found across all runs
- Stops when time limit is reached

**Key Features:**
- Time limit: 438.46 seconds (438,460 ms)
- Supports both STEEPEST and GREEDY local search variants
- Supports both NODE_SWAP and TWO_OPT neighborhoods
- Tracks number of completed iterations

**Constructor Parameters:**
```java
AlgorithmMSLS(
    Instance instance,           // TSP instance to solve
    int numIterations,           // Max number of random starts
    LocalSearchType localSearchType, // STEEPEST or GREEDY
    Neighborhood neighborhood,   // NODE_SWAP or TWO_OPT
    long timeLimitMs,           // Time limit in milliseconds
    long seed                   // Random seed for reproducibility
)
```

**Algorithm Steps:**
1. For each iteration (until time limit):
   - Generate a random starting solution
   - Apply local search until local optimum
   - Update best solution if improvement found
2. Return best solution found

### 2. AlgorithmILS (Iterated Local Search)
**Location:** `experiments/src/main/java/tsp/algorithms/MSLS_ILS/AlgorithmILS.java`

**Description:**
- Starts with a random initial solution
- Applies local search to reach local optimum
- Iteratively perturbs the solution and applies local search again
- Accepts improved solutions to guide the search

**Key Features:**
- Time limit: 438.46 seconds (438,460 ms)
- Supports both STEEPEST and GREEDY local search variants
- Supports both NODE_SWAP and TWO_OPT neighborhoods
- Configurable perturbation strength
- Multiple perturbation strategies: random swaps, 2-opt moves, and node replacements

**Constructor Parameters:**
```java
AlgorithmILS(
    Instance instance,           // TSP instance to solve
    LocalSearchType localSearchType, // STEEPEST or GREEDY
    Neighborhood neighborhood,   // NODE_SWAP or TWO_OPT
    int perturbationStrength,   // Number of random moves per perturbation
    long timeLimitMs,           // Time limit in milliseconds
    long seed                   // Random seed for reproducibility
)
```

**Algorithm Steps:**
1. Generate initial random solution
2. Apply local search to reach local optimum
3. Loop until time limit:
   - Perturb current solution (apply k random moves)
   - Apply local search to perturbed solution
   - Accept if better than best found
   - Continue from current solution (not best)
4. Return best solution found

**Perturbation Strategies:**
- Random node swaps (for NODE_SWAP neighborhood)
- Random 2-opt moves (for TWO_OPT neighborhood)
- Random node replacements (inter-route moves)
- Combines multiple perturbation types

## Experiment Runner

### MSLS_ILS_ExperimentRunner
**Location:** `experiments/src/main/java/tsp/analysis/MSLS_ILS_ExperimentRunner.java`

**Configurations Tested:**
1. MSLS_STEEPEST_Nodes
2. MSLS_STEEPEST_Edges
3. MSLS_GREEDY_Nodes
4. MSLS_GREEDY_Edges
5. ILS_STEEPEST_Nodes
6. ILS_STEEPEST_Edges
7. ILS_GREEDY_Nodes
8. ILS_GREEDY_Edges

**Experiment Parameters:**
- Time limit: 438.46 seconds per run
- Runs per configuration: 10 per instance
- Total runs: 8 configs × 2 instances × 10 runs = 160 runs
- ILS perturbation strength: 10% of route length (min 5 moves)

## Usage

### Running Experiments via Docker
```bash
# Run MSLS and ILS experiments
.\scripts\run-experiments.bat msls_ils
```

### Programmatic Usage

**MSLS Example:**
```java
Instance instance = InstanceLoader.loadInstance("data/TSPA.csv");

AlgorithmMSLS msls = new AlgorithmMSLS(
    instance,
    10000,  // Many iterations (limited by time)
    AlgorithmMSLS.LocalSearchType.STEEPEST,
    LocalSearchAlgorithm.Neighborhood.NODE_SWAP,
    438460,  // 438.46 seconds
    System.nanoTime()
);

Solution solution = msls.solve();
System.out.println("Best objective: " + solution.getObjectiveValue());
System.out.println("Completed iterations: " + msls.getCompletedIterations());
```

**ILS Example:**
```java
Instance instance = InstanceLoader.loadInstance("data/TSPB.csv");

AlgorithmILS ils = new AlgorithmILS(
    instance,
    AlgorithmILS.LocalSearchType.GREEDY,
    LocalSearchAlgorithm.Neighborhood.TWO_OPT,
    10,  // Perturbation strength
    438460,  // 438.46 seconds
    System.nanoTime()
);

Solution solution = ils.solve();
System.out.println("Best objective: " + solution.getObjectiveValue());
System.out.println("ILS iterations: " + ils.getIterationCount());
```

## Technical Details

### Local Search Variants

**Steepest Descent:**
- Evaluates ALL possible moves in the neighborhood
- Applies the move with the best (most negative) delta
- More thorough but slower per iteration

**Greedy (First Improvement):**
- Evaluates moves in random order
- Applies the FIRST improving move found
- Faster per iteration but potentially less optimal

### Neighborhood Operators

**NODE_SWAP:**
- Swaps positions of two nodes in the route
- O(n²) moves per iteration
- Smaller neighborhood, faster evaluation

**TWO_OPT:**
- Reverses a segment of the route
- O(n²) moves per iteration
- Larger neighborhood, potentially better solutions

### Time Complexity

**MSLS:**
- Per iteration: O(n² × m) for steepest, O(n²) expected for greedy
  - n = number of nodes in route
  - m = number of moves evaluated (depends on neighborhood)
- Total: Depends on number of iterations completed within time limit

**ILS:**
- Per local search: Same as MSLS single iteration
- Perturbation: O(k) where k is perturbation strength
- Total: Depends on number of perturbation cycles within time limit

## Integration with Existing Code

The algorithms integrate seamlessly with the existing codebase:

1. **Extends LocalSearchAlgorithm:** Inherits move generation and delta calculation
2. **Uses existing Move classes:** NodeSwapMove, TwoOptMove, ReplaceNodeMove
3. **Compatible with ExperimentRunner:** Standard AlgorithmResult output
4. **Follows naming conventions:** Consistent with other algorithm implementations

## Expected Results

Based on Assignment 6:
- MSLS should find high-quality solutions through extensive exploration
- ILS should find competitive solutions through strategic perturbations
- Steepest variants should find better solutions but complete fewer iterations
- Greedy variants should complete more iterations but find potentially worse solutions
- TWO_OPT neighborhood typically finds better solutions than NODE_SWAP

## Files Created/Modified

### New Files:
1. `AlgorithmMSLS.java` - MSLS implementation
2. `AlgorithmILS.java` - ILS implementation
3. `MSLS_ILS_ExperimentRunner.java` - Experiment runner
4. `TestRunner.java` - Simple test harness
5. `IMPLEMENTATION.md` - This documentation

### Modified Files:
1. `AlgorithmExperimentRunner.java` - Added MSLS/ILS case to switch statement

## Notes

- Both algorithms use the same time limit (438.46 seconds)
- Different random seeds ensure diverse starting solutions
- Perturbation strength in ILS is adaptive based on instance size
- Time limit is checked both between and during iterations to ensure compliance
- Solutions are validated using the existing SolutionChecker
