package tsp.algorithms.regret;

import tsp.algorithms.greedy.NearestNeighborAnyPositionAlgorithm;
import tsp.core.*;
import java.util.*;

/**
 * Nearest Neighbor algorithm with 2-Regret (weighted regret).
 * Uses weighted combination of insertion cost and regret value.
 * 
 * Regret = difference between best and second-best insertion cost for a node.
 * Final score = weightInsertion * insertionCost - weightRegret * regret
 * 
 * Higher regret (larger difference) means the node should be prioritized,
 * as it has much better position now than alternatives.
 * 
 * Pseudocode:
 * 1. Start with startNode in the route
 * 2. While route size < required nodes:
 *    a. For each unselected node:
 *       - Find best insertion position (minimum cost)
 *       - Find second-best insertion position
 *       - Calculate regret = secondBestCost - bestCost
 *       - Calculate score = weightInsertion * bestCost - weightRegret * regret
 *    b. Select node with minimum score
 *    c. Insert selected node at its best position
 * 3. Return solution
 */
public class NearestNeighborAnyPositionTwoRegretAlgorithm extends NearestNeighborAnyPositionAlgorithm {
    private final int weightInsertion;
    private final int weightRegret;

    public NearestNeighborAnyPositionTwoRegretAlgorithm(Instance instance, int startNode, 
                                                         int weightInsertion, int weightRegret) {
        super(instance, startNode);
        this.weightInsertion = weightInsertion;
        this.weightRegret = weightRegret;
    }
    
    @Override
    protected Map.Entry<Integer, Integer> findBestNodeAndPosition(
            Set<Integer> unselectedNodes, List<Integer> route
    ) {
        Integer bestNode = null;
        int bestPosition = -1;
        double bestScore = Double.MAX_VALUE;

        // For each unselected node, find best and second-best positions
        for (Integer candidate : unselectedNodes) {
            long bestCost = Long.MAX_VALUE;
            long secondBestCost = Long.MAX_VALUE;
            int bestPos = -1;
            
            // Try all possible positions
            for (int position = 0; position <= route.size(); position++) {
                long cost = calculateObjectiveChangeAtPosition(
                        candidate, position, route, distanceMatrix);
                
                if (cost < bestCost) {
                    // New best position
                    secondBestCost = bestCost;
                    bestCost = cost;
                    bestPos = position;
                } else if (cost < secondBestCost) {
                    // New second-best position
                    secondBestCost = cost;
                }
            }
            
            // Calculate regret (difference between second-best and best)
            long regret = 0;
            if (secondBestCost != Long.MAX_VALUE) {
                regret = bestCost - secondBestCost;
            }
            
            // Calculate weighted score
            // Lower score is better
            // We subtract weighted regret because high regret should give priority
            double score = (weightInsertion * bestCost) - (weightRegret * regret);
            
            if (score < bestScore) {
                bestScore = score;
                bestNode = candidate;
                bestPosition = bestPos;
            }
        }
        
        if (bestNode != null) {
            return new AbstractMap.SimpleEntry<>(bestNode, bestPosition);
        } else {
            return null;
        }
    }
    
    @Override
    public String getName() {
        return "NearestNeighborAny2Regret_w" + weightInsertion + "_" + weightRegret + "_start" + startNode;
    }
}
