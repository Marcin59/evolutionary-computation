package tsp.algorithms.regret;

import tsp.algorithms.greedy.GreedyCycleAlgorithm;
import tsp.core.*;
import java.util.*;

/**
 * Greedy Cycle construction heuristic with 2-Regret (weighted regret).
 * Uses weighted combination of insertion cost and regret value.
 * 
 * Regret = difference between best and second-best insertion cost for a node.
 * Final score = weightInsertion * insertionCost - weightRegret * regret
 * 
 * Higher regret (larger difference) means the node should be prioritized,
 * as it has much better position now than alternatives.
 * 
 * Pseudocode:
 * 1. Select the starting vertex
 * 2. Choose the nearest vertex and create an incomplete cycle from these two vertices
 * 3. Repeat:
 *    a. For each unselected node:
 *       - Find best insertion position (minimum cycle increase)
 *       - Find second-best insertion position
 *       - Calculate regret = secondBestCost - bestCost
 *       - Calculate score = weightInsertion * bestCost - weightRegret * regret
 *    b. Select node with minimum score
 *    c. Insert selected node at its best position in the cycle
 *    Until all vertices have been added
 */
public class GreedyCycleTwoRegretAlgorithm extends GreedyCycleAlgorithm {
    private final int weightInsertion;
    private final int weightRegret;

    public GreedyCycleTwoRegretAlgorithm(Instance instance, int startNode,
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
        int bestInsertionIndex = -1;
        double bestScore = Double.MAX_VALUE;

        // For each unselected node, find best and second-best positions
        for (Integer candidate : unselectedNodes) {
            long bestCost = Long.MAX_VALUE;
            long secondBestCost = Long.MAX_VALUE;
            int bestPos = -1;
            
            // Try inserting after each node in current route (at each edge)
            for (int position = 0; position <= route.size(); position++) {
                long cost = calculateInsertionCost(
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
            
            double score = (weightInsertion * bestCost) - (weightRegret * regret);
            
            if (score < bestScore) {
                bestScore = score;
                bestNode = candidate;
                // Position + 1 because we insert after the edge
                bestInsertionIndex = bestPos + 1;
            }
        }
        
        if (bestNode != null) {
            return new AbstractMap.SimpleEntry<>(bestNode, bestInsertionIndex);
        } else {
            return null;
        }
    }
    
    @Override
    public String getName() {
        return "GreedyCycle2Regret_w" + weightInsertion + "_" + weightRegret + "_start" + startNode;
    }
}
