package tsp.algorithms.MSLS_ILS;

import tsp.core.*;
import tsp.algorithms.localsearch.*;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import java.util.*;

/**
 * Iterated Local Search (ILS) Algorithm for TSP.
 * 
 * Algorithm:
 * 1. Generate initial random solution
 * 2. Apply local search to get local optimum
 * 3. Loop:
 *    a. Perturb the current solution
 *    b. Apply local search to the perturbed solution
 *    c. If new solution is better, accept it (otherwise keep current)
 * 4. Continue until time limit is reached
 * 
 * Perturbation strategies:
 * - Random perturbation: perform k random moves
 * - Large perturbation: perform multiple random swaps/replacements
 * 
 * Time limit: 438.46 seconds (as specified in Assignment 6)
 */
public class AlgorithmILS extends Algorithm {
    private final LocalSearchType localSearchType;
    private final LocalSearchAlgorithm.Neighborhood neighborhood;
    private final int perturbationStrength;
    private final long timeLimitMs;
    private final Random random;
    private Solution bestSolution;
    private Solution currentSolution;
    private int iterationCount;
    
    public enum LocalSearchType {
        STEEPEST,
        GREEDY
    }
    
    /**
     * Constructor with time limit.
     * 
     * @param instance TSP instance to solve
     * @param localSearchType Type of local search (STEEPEST or GREEDY)
     * @param neighborhood Type of neighborhood (NODE_SWAP or TWO_OPT)
     * @param perturbationStrength Number of random moves for perturbation
     * @param timeLimitMs Time limit in milliseconds
     * @param seed Random seed for reproducibility
     */
    public AlgorithmILS(Instance instance, LocalSearchType localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood, int perturbationStrength, 
                        long timeLimitMs, long seed) {
        super("ILS", instance);
        this.localSearchType = localSearchType;
        this.neighborhood = neighborhood;
        this.perturbationStrength = perturbationStrength;
        this.timeLimitMs = timeLimitMs;
        this.random = new Random(seed);
        this.iterationCount = 0;
    }
    
    /**
     * Constructor without explicit seed (uses system time).
     */
    public AlgorithmILS(Instance instance, LocalSearchType localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood, int perturbationStrength, 
                        long timeLimitMs) {
        this(instance, localSearchType, neighborhood, perturbationStrength, timeLimitMs, System.nanoTime());
    }
    
    @Override
    public Solution solve() {
        long startTime = System.currentTimeMillis();
        
        // Generate initial solution
        RandomSolutionAlgorithm randomAlgorithm = new RandomSolutionAlgorithm(
            instance, random.nextInt(instance.getTotalNodes()), random.nextLong()
        );
        
        // Apply local search to initial solution
        currentSolution = applyLocalSearch(randomAlgorithm);
        bestSolution = currentSolution;
        iterationCount = 0;
        
        // Main ILS loop
        while (System.currentTimeMillis() - startTime < timeLimitMs) {
            // Perturb current solution
            Solution perturbedSolution = perturbSolution(currentSolution);
            
            // Create algorithm that returns the perturbed solution
            Algorithm perturbedAlgorithm = new Algorithm("Perturbed", instance) {
                @Override
                public Solution solve() {
                    return perturbedSolution;
                }
            };
            
            // Apply local search to perturbed solution
            currentSolution = applyLocalSearch(perturbedAlgorithm);
            
            // Accept if better (or with some probability for diversification)
            if (currentSolution.getObjectiveValue() < bestSolution.getObjectiveValue()) {
                bestSolution = currentSolution;
            }
            
            // Keep current solution as base for next perturbation
            // (This allows exploring around the current local optimum)
            
            iterationCount++;
        }
        
        return bestSolution;
    }
    
    /**
     * Apply local search until local optimum is reached.
     */
    private Solution applyLocalSearch(Algorithm initialAlgorithm) {
        LocalSearchAlgorithm localSearch;
        if (localSearchType == LocalSearchType.STEEPEST) {
            localSearch = new SteepestLocalSearch(initialAlgorithm, neighborhood);
        } else {
            localSearch = new GreedyLocalSearch(initialAlgorithm, neighborhood);
        }
        return localSearch.solve();
    }
    
    /**
     * Perturb the solution by applying random moves.
     * This helps escape local optima.
     */
    private Solution perturbSolution(Solution solution) {
        List<Integer> route = new ArrayList<>(solution.getRoute());
        Set<Integer> selectedNodes = new HashSet<>(solution.getSelectedNodes());
        int n = route.size();
        
        // Apply multiple random perturbations
        for (int i = 0; i < perturbationStrength; i++) {
            if (neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP) {
                // Random node swap
                int pos1 = random.nextInt(n);
                int pos2 = random.nextInt(n);
                if (pos1 != pos2) {
                    int temp = route.get(pos1);
                    route.set(pos1, route.get(pos2));
                    route.set(pos2, temp);
                }
            } else if (neighborhood == LocalSearchAlgorithm.Neighborhood.TWO_OPT) {
                // Random 2-opt move (edge exchange)
                int i1 = random.nextInt(n - 1);
                int i2 = i1 + 1 + random.nextInt(n - i1 - 1);
                if (!(i1 == 0 && i2 == n - 1)) {
                    // Reverse segment
                    int left = i1 + 1;
                    int right = i2;
                    while (left < right) {
                        int temp = route.get(left);
                        route.set(left, route.get(right));
                        route.set(right, temp);
                        left++;
                        right--;
                    }
                }
            }
        }
        
        // Create and return perturbed solution with consistent selected nodes and route
        return new TSPSolution(instance, selectedNodes, route);
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    @Override
    public String getName() {
        return "ILS_" + localSearchType + "_" + neighborhood + "_pert" + perturbationStrength;
    }
}