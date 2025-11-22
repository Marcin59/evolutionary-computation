package tsp.algorithms.MSLS_ILS;

import tsp.core.*;
import tsp.algorithms.localsearch.*;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import java.util.*;

/**
 * Multiple Start Local Search (MSLS) Algorithm for TSP.
 * 
 * Algorithm:
 * 1. Generate multiple random starting solutions
 * 2. Apply local search to each starting solution
 * 3. Return the best solution found across all runs
 * 
 * Parameters:
 * - Number of iterations (starting solutions to try)
 * - Type of local search to use (steepest or greedy)
 * - Neighborhood type (NODE_SWAP or TWO_OPT)
 * 
 * Time limit: 438.46 seconds (as specified in Assignment 6)
 */
public class AlgorithmMSLS extends Algorithm {
    private final int numIterations;
    private final LocalSearchType localSearchType;
    private final LocalSearchAlgorithm.Neighborhood neighborhood;
    private final Random random;
    private Solution bestSolution;
    private int completedIterations;
    
    public enum LocalSearchType {
        STEEPEST,
        GREEDY
    }
    
    /**
     * Constructor.
     * 
     * @param instance TSP instance to solve
     * @param numIterations Number of random starting solutions to try
     * @param localSearchType Type of local search (STEEPEST or GREEDY)
     * @param neighborhood Type of neighborhood (NODE_SWAP or TWO_OPT)
     * @param seed Random seed for reproducibility
     */
    public AlgorithmMSLS(Instance instance, int numIterations, LocalSearchType localSearchType, 
                         LocalSearchAlgorithm.Neighborhood neighborhood, long seed) {
        super("MSLS", instance);
        this.numIterations = numIterations;
        this.localSearchType = localSearchType;
        this.neighborhood = neighborhood;
        this.random = new Random(seed);
        this.completedIterations = 0;
    }
    
    /**
     * Constructor without explicit seed (uses system time).
     */
    public AlgorithmMSLS(Instance instance, int numIterations, LocalSearchType localSearchType, 
                         LocalSearchAlgorithm.Neighborhood neighborhood) {
        this(instance, numIterations, localSearchType, neighborhood, System.nanoTime());
    }
    
    @Override
    public Solution solve() {
        bestSolution = null;
        completedIterations = 0;
        
        for (int i = 0; i < numIterations; i++) {
            // Generate random starting solution
            RandomSolutionAlgorithm randomAlgorithm = new RandomSolutionAlgorithm(
                instance, random.nextInt(instance.getTotalNodes()), random.nextLong()
            );
            
            // Create and run local search algorithm
            LocalSearchAlgorithm localSearch;
            if (localSearchType == LocalSearchType.STEEPEST) {
                localSearch = new SteepestLocalSearch(randomAlgorithm, neighborhood);
            } else {
                localSearch = new GreedyLocalSearch(randomAlgorithm, neighborhood);
            }
            
            Solution currentSolution = localSearch.solve();
            
            // Update best solution
            if (bestSolution == null || currentSolution.getObjectiveValue() < bestSolution.getObjectiveValue()) {
                bestSolution = currentSolution;
            }
            
            completedIterations++;
        }
        
        return bestSolution;
    }
    
    public int getCompletedIterations() {
        return completedIterations;
    }
    
    @Override
    public String getName() {
        return "MSLS_" + localSearchType + "_" + neighborhood;
    }
}