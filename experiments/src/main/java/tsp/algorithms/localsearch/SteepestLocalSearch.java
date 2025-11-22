package tsp.algorithms.localsearch;

import tsp.core.*;
import java.util.*;

/**
 * Steepest (best improvement) local search for TSP.
 * 
 * In each iteration:
 * 1. Evaluate all possible moves in the neighborhood
 * 2. Select the move with the best (most negative) delta
 * 3. Apply the best move if it improves the solution
 * 4. Repeat until no improving move is found (local optimum)
 * 
 * Neighborhood includes:
 * - Node position swaps (intra-route)
 * - 2-opt edge exchanges (inter-route)
 */
public class SteepestLocalSearch extends LocalSearchAlgorithm {

    public SteepestLocalSearch(Algorithm initialSolutionAlgorithm, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("SteepestLocalSearch", initialSolutionAlgorithm, neighborhood);
    }

    public SteepestLocalSearch(Algorithm initialSolutionAlgorithm, long seed, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("SteepestLocalSearch", initialSolutionAlgorithm, seed, neighborhood);
    }
    
    @Override
    protected boolean performIteration() {
        // Generate all possible moves
        List<Move> allMoves = generateAllMoves();
        
        // Find the best improving move
        Move bestMove = null;
        long bestDelta = 0; // We only accept negative deltas (improvements)
        
        for (Move move : allMoves) {
            long delta = calculateMoveDelta(move);
            
            if (delta < bestDelta) {
                bestDelta = delta;
                bestMove = move;
            }
        }
        
        // If we found an improving move, apply it
        if (bestMove != null) {
            currentSolution = applyMove(bestMove);
            return true;
        }
        
        // No improvement found - local optimum reached
        return false;
    }
    
    @Override
    public String getName() {
        return "SteepestLS_" + initialSolutionAlgorithm.getName();
    }
}
