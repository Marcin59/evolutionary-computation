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
    private final int externalPerturbationStrength;
    private final long timeLimitMs;
    private final Random random;
    private Solution bestSolution;
    private Solution currentSolution;
    private int iterationCount;
    private int localSearchCallCount;
    
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
     * @param perturbationStrength Number of random moves for perturbation (intra-route)
     * @param externalPerturbationStrength Number of random node replacements (inter-route)
     * @param timeLimitMs Time limit in milliseconds
     * @param seed Random seed for reproducibility
     */
    public AlgorithmILS(Instance instance, LocalSearchType localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood, int perturbationStrength,
                        int externalPerturbationStrength, long timeLimitMs, long seed) {
        super("ILS", instance);
        this.localSearchType = localSearchType;
        this.neighborhood = neighborhood;
        this.perturbationStrength = perturbationStrength;
        this.externalPerturbationStrength = externalPerturbationStrength;
        this.timeLimitMs = timeLimitMs;
        this.random = new Random(seed);
        this.iterationCount = 0;
        this.localSearchCallCount = 0;
    }
    
    /**
     * Constructor without explicit seed (uses system time).
     */
    public AlgorithmILS(Instance instance, LocalSearchType localSearchType, 
                        LocalSearchAlgorithm.Neighborhood neighborhood, int perturbationStrength,
                        int externalPerturbationStrength, long timeLimitMs) {
        this(instance, localSearchType, neighborhood, perturbationStrength, 
             externalPerturbationStrength, timeLimitMs, System.nanoTime());
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
        localSearchCallCount = 1; // Count the initial local search
        
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
            localSearchCallCount++; // Increment counter
            
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
     * Perturb the solution by applying random moves using the implemented move classes.
     * This helps escape local optima.
     * 
     * Applies two types of perturbations:
     * 1. Intra-route moves (NODE_SWAP or TWO_OPT) - based on neighborhood type
     * 2. Inter-route moves (ReplaceNode) - replacing nodes with external ones
     */
    private Solution perturbSolution(Solution solution) {
        Solution currentSol = solution;
        
        // Apply random intra-route perturbations (based on neighborhood type)
        for (int i = 0; i < perturbationStrength; i++) {
            LocalSearchAlgorithm.Move move = generateRandomIntraRouteMove(currentSol);
            if (move != null) {
                currentSol = move.apply(currentSol);
            }
        }
        
        // Apply random inter-route perturbations (node replacements)
        for (int i = 0; i < externalPerturbationStrength; i++) {
            LocalSearchAlgorithm.Move move = generateRandomReplaceNodeMove(currentSol);
            if (move != null) {
                currentSol = move.apply(currentSol);
            }
        }
        
        return currentSol;
    }
    
    /**
     * Generate a random intra-route move based on the neighborhood type.
     */
    private LocalSearchAlgorithm.Move generateRandomIntraRouteMove(Solution solution) {
        List<Integer> route = solution.getRoute();
        int n = route.size();
        
        if (n < 2) return null;
        
        if (neighborhood == LocalSearchAlgorithm.Neighborhood.NODE_SWAP) {
            // Random node swap
            int pos1 = random.nextInt(n);
            int pos2 = random.nextInt(n);
            if (pos1 != pos2) {
                return new LocalSearchAlgorithm.NodeSwapMove(pos1, pos2);
            }
        } else if (neighborhood == LocalSearchAlgorithm.Neighborhood.TWO_OPT) {
            // Random 2-opt move (edge exchange)
            if (n < 3) return null;
            
            int i1 = random.nextInt(n - 1);
            int i2 = i1 + 1 + random.nextInt(n - i1 - 1);
            
            // Avoid reversing the entire route
            if (!(i1 == 0 && i2 == n - 1)) {
                return new LocalSearchAlgorithm.TwoOptMove(i1, i2);
            }
        }
        
        return null;
    }
    
    /**
     * Generate a random node replacement move (replace a selected node with an unselected one).
     */
    private LocalSearchAlgorithm.Move generateRandomReplaceNodeMove(Solution solution) {
        List<Integer> route = solution.getRoute();
        Set<Integer> selected = solution.getSelectedNodes();
        int totalNodes = instance.getTotalNodes();
        
        // Find all unselected nodes
        List<Integer> unselectedNodes = new ArrayList<>();
        for (int node = 0; node < totalNodes; node++) {
            if (!selected.contains(node)) {
                unselectedNodes.add(node);
            }
        }
        
        if (unselectedNodes.isEmpty() || route.isEmpty()) {
            return null;
        }
        
        // Pick a random position in the route
        int pos = random.nextInt(route.size());
        
        // Pick a random unselected node
        int outsideNode = unselectedNodes.get(random.nextInt(unselectedNodes.size()));
        
        return new LocalSearchAlgorithm.ReplaceNodeMove(pos, outsideNode);
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    public int getLocalSearchCallCount() {
        return localSearchCallCount;
    }
    
    @Override
    public String getName() {
        return "ILS_" + localSearchType + "_" + neighborhood + 
               "_pert" + perturbationStrength + "_ext" + externalPerturbationStrength;
    }
}