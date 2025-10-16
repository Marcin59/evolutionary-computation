package tsp.algorithms.greedy;

import tsp.algorithms.IterativeAlgorithm;
import tsp.core.*;
import java.util.*;

/**
 * Nearest Neighbor algorithm that considers adding nodes at any position in the route.
 * "Nearest" means the best improvement in objective function (distance + node cost).
 * 
 * Pseudocode:
 * 1. Start with startNode in the route
 * 2. While route size < required nodes:
 *    a. For each unselected node:
 *       - For each possible position in route (including end):
 *         * Calculate objective change if inserted at this position
 *       - Keep best position for this node
 *    b. Select node and position with best (minimum) objective change
 *    c. Insert selected node at best position
 * 3. Return solution
 */
public class NearestNeighborAnyPositionAlgorithm extends IterativeAlgorithm {

    public NearestNeighborAnyPositionAlgorithm(Instance instance, int startNode) {
        super("NearestNeighborAny", instance, startNode);
    }
    
    @Override
    protected Map.Entry<Integer, Integer> findBestNodeAndPosition(
            Set<Integer> unselectedNodes, List<Integer> route
    ) {
        Integer bestNode = null;
        int bestPosition = -1;
        long bestObjectiveChange = Long.MAX_VALUE;

        // Try adding each unselected node at each possible position
        for (Integer candidate : unselectedNodes) {
            // Try all possible positions (0 to route.size())
            for (int position = 0; position <= route.size(); position++) {
                long objectiveChange = calculateObjectiveChangeAtPosition(
                        candidate, position, route, distanceMatrix);

                if (objectiveChange < bestObjectiveChange) {
                    bestObjectiveChange = objectiveChange;
                    bestNode = candidate;
                    bestPosition = position;
                }
            }
        }
        
        if (bestNode != null) {
            return new AbstractMap.SimpleEntry<>(bestNode, bestPosition);
        } else {
            return null;
        }
    }
    
    /**
     * Calculate objective function change when inserting a node at a specific position.
     */
    protected long calculateObjectiveChangeAtPosition(int newNode, int position,
                                                    List<Integer> currentRoute, DistanceMatrix distMatrix) {
        // Cost of adding the new node
        long nodeCost = instance.getNode(newNode).getCost();
        
        // Distance cost change
        long distanceChange;

        if (currentRoute.size() == 1) {
            // Special case: only one node in route
            distanceChange = distMatrix.getDistance(currentRoute.get(0), newNode);
        } else {
            // Get nodes before and after insertion position
            int prevNode, nextNode;
            
            if (position == 0) {
                // Insert at beginning
                nextNode = currentRoute.get(0); // Current first node
                distanceChange = distMatrix.getDistance(newNode, nextNode);
            } else if (position == currentRoute.size()) {
                // Insert at end
                prevNode = currentRoute.get(currentRoute.size() - 1); // Current last node
                distanceChange = distMatrix.getDistance(prevNode, newNode);
            } else {
                // Insert in middle
                prevNode = currentRoute.get(position - 1);
                nextNode = currentRoute.get(position);
                distanceChange = distMatrix.getDistance(prevNode, newNode) +
                                distMatrix.getDistance(newNode, nextNode) -
                                distMatrix.getDistance(prevNode, nextNode);
            }
        }
        
        return nodeCost + distanceChange;
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}