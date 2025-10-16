package tsp.algorithms.greedy;

import tsp.algorithms.IterativeAlgorithm;
import tsp.core.*;
import java.util.*;

/**
 * Greedy Cycle construction heuristic for TSP.
 * 
 * Pseudocode:
 * 1. Select (e.g. randomly) the starting vertex
 * 2. Choose the nearest vertex and create an incomplete cycle from these two vertices
 * 3. Repeat:
 *    - Insert into the current cycle in the best possible place the vertex 
 *      causing the smallest increase in cycle length
 *    Until all vertices have been added
 */
public class GreedyCycleAlgorithm extends IterativeAlgorithm {

    public GreedyCycleAlgorithm(Instance instance, int startNode) {
        super("GreedyCycle", instance, startNode);
    }
    
    @Override
    protected Map.Entry<Integer, Integer> findBestNodeAndPosition(
            Set<Integer> unselectedNodes, List<Integer> route
    ) {
        Integer bestNode = null;
        int bestInsertionIndex = -1;
        long bestObjectiveChange = Long.MAX_VALUE;

        // Try inserting each unselected node at each possible edge
        for (Integer candidate : unselectedNodes)
        {
            // Try inserting after each node in current route
            for (int position = 0; position <= route.size(); position++) {
                long objectiveChange = calculateInsertionCost(
                        candidate, position, route, distanceMatrix);

                if (objectiveChange < bestObjectiveChange) {
                    bestObjectiveChange = objectiveChange;
                    bestNode = candidate;
                    bestInsertionIndex = position + 1;
                }
            }
        }
        if (bestNode != null) {
            return new AbstractMap.SimpleEntry<>(bestNode, bestInsertionIndex);
        } else {
            return null;
        }
    }


    /**
     * Calculate the cost of inserting a node after the given index in the route.
     * This breaks the edge from route[index] to route[(index+1)%size] and 
     * creates two new edges: route[index] -> newNode -> route[(index+1)%size]
     */
    private long calculateInsertionCost(int newNode, int afterIndex,
                                        List<Integer> currentRoute, DistanceMatrix distMatrix) {
        // Cost of adding the new node
        long nodeCost = instance.getNode(newNode).getCost();
        
        // Distance cost change
        long distanceChange;
        
        // Normal case: inserting into an edge in the cycle
        int nodeA = currentRoute.get(afterIndex % currentRoute.size());
        int nodeB = currentRoute.get((afterIndex + 1) % currentRoute.size());
        
        // Remove edge A -> B
        long removedDistance = distMatrix.getDistance(nodeA, nodeB);
        
        // Add edges A -> newNode -> B
        long addedDistance = distMatrix.getDistance(nodeA, newNode) +
                             distMatrix.getDistance(newNode, nodeB);
        
        distanceChange = addedDistance - removedDistance;
        
        return nodeCost + distanceChange;
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}