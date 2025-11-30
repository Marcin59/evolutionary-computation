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
    
    // For partial solution repair
    private List<Integer> initialRoute;
    private Set<Integer> initialSelectedNodes;
    private int targetRouteSize;

    public NearestNeighborAnyPositionTwoRegretAlgorithm(Instance instance, int startNode, 
                                                         int weightInsertion, int weightRegret) {
        super(instance, startNode);
        this.weightInsertion = weightInsertion;
        this.weightRegret = weightRegret;
        this.initialRoute = null;
        this.initialSelectedNodes = null;
        this.targetRouteSize = -1;
    }
    
    /**
     * Constructor for repairing a partial solution.
     * Considers ALL unselected nodes for insertion (not just specific nodes).
     * @param instance The TSP instance.
     * @param partialRoute The existing partial route to extend.
     * @param targetRouteSize The target size of the route (typically instance.getRequiredNodes()).
     * @param weightInsertion Weight for insertion cost.
     * @param weightRegret Weight for regret value.
     */
    public NearestNeighborAnyPositionTwoRegretAlgorithm(Instance instance, List<Integer> partialRoute,
                                                         int targetRouteSize,
                                                         int weightInsertion, int weightRegret) {
        super(instance, partialRoute.isEmpty() ? 0 : partialRoute.get(0));
        this.weightInsertion = weightInsertion;
        this.weightRegret = weightRegret;
        this.initialRoute = new ArrayList<>(partialRoute);
        this.initialSelectedNodes = new HashSet<>(partialRoute);
        this.targetRouteSize = targetRouteSize;
    }
    
    @Override
    public Solution solve() {
        // If we have a partial solution to repair, use custom logic
        if (initialRoute != null) {
            return solveFromPartialSolution();
        }
        // Otherwise, use the standard solve from parent
        return super.solve();
    }
    
    /**
     * Solve by extending a partial solution until target size is reached.
     * Considers all nodes not in the current route for insertion.
     */
    private Solution solveFromPartialSolution() {
        List<Integer> route = new ArrayList<>(initialRoute);
        Set<Integer> selectedNodes = new HashSet<>(initialSelectedNodes);
        
        // Build unselected nodes from ALL nodes not in the partial route
        Set<Integer> unselectedNodes = new HashSet<>();
        for (int i = 0; i < instance.getTotalNodes(); i++) {
            if (!selectedNodes.contains(i)) {
                unselectedNodes.add(i);
            }
        }
        
        distanceMatrix = instance.getDistanceMatrix();
        
        // Insert nodes until we reach the target route size
        while (selectedNodes.size() < targetRouteSize && !unselectedNodes.isEmpty()) {
            Map.Entry<Integer, Integer> bestEntry = findBestNodeAndPosition(unselectedNodes, route);
            if (bestEntry != null) {
                int bestNode = bestEntry.getKey();
                int bestPosition = bestEntry.getValue();
                route.add(bestPosition, bestNode);
                selectedNodes.add(bestNode);
                unselectedNodes.remove(bestNode);
            } else {
                break;
            }
        }
        
        return new TSPSolution(instance, selectedNodes, route);
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
                regret = secondBestCost -bestCost;
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
