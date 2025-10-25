package tsp.algorithms.localsearch;

import tsp.core.*;
import java.util.*;

/**
 * Greedy (first improvement) local search for TSP.
 * 
 * In each iteration:
 * 1. Generate all possible moves and randomize their order
 * 2. Evaluate moves in the randomized order
 * 3. Apply the first move that improves the solution
 * 4. Repeat until no improving move is found (local optimum)
 * 
 * The randomization ensures different search trajectories in different runs,
 * which is important for avoiding bias towards particular move types or positions.
 * 
 * Neighborhood includes:
 * - Node position swaps (intra-route)
 * - 2-opt edge exchanges (inter-route)
 */
public class GreedyLocalSearch extends LocalSearchAlgorithm {

    public GreedyLocalSearch(Algorithm initialSolutionAlgorithm, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("GreedyLocalSearch", initialSolutionAlgorithm, neighborhood);
    }

    public GreedyLocalSearch(Algorithm initialSolutionAlgorithm, long seed, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("GreedyLocalSearch", initialSolutionAlgorithm, seed, neighborhood);
    }
    
    @Override
    protected boolean performIteration() {
        // Generate all possible moves
        List<Move> allMoves = generateAllMoves();
        
        // Randomize the order to ensure unbiased search
        shuffle(allMoves);
        
        // Find the first improving move
        for (Move move : allMoves) {
            long delta = calculateMoveDelta(move);
            
            // If this move improves the solution, apply it immediately
            if (delta < 0) {
                currentSolution = applyMove(move);
                return true;
            }
        }
        
        // No improvement found - local optimum reached
        return false;
    }
    
    @Override
    public String getName() {
        return "GreedyLS_" + initialSolutionAlgorithm.getName();
    }
}
