package tsp.algorithms.greedy;

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
public class NearestNeighborAnyPositionAlgorithm extends Algorithm {
    private final int startNode;
    
    public NearestNeighborAnyPositionAlgorithm(Instance instance, int startNode) {
        super("NearestNeighborAny", instance);
        this.startNode = startNode;
    }
    
    @Override
    public Solution solve() {
        List<Integer> route = new ArrayList<>();
        Set<Integer> selectedNodes = new HashSet<>();
        Set<Integer> unselectedNodes = new HashSet<>();
        
        // Initialize with all nodes as unselected
        for (int i = 0; i < instance.getTotalNodes(); i++) {
            unselectedNodes.add(i);
        }
        
        // Start with the given start node
        route.add(startNode);
        selectedNodes.add(startNode);
        unselectedNodes.remove(startNode);
        
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        
        // Build route by adding nodes at best positions
        while (selectedNodes.size() < instance.getRequiredNodes()) {
            Integer bestNode = null;
            int bestPosition = -1;
            long bestObjectiveChange = Long.MAX_VALUE;
            
            // Try adding each unselected node at each possible position
            for (Integer candidate : unselectedNodes) {
                // Try all possible positions (0 to route.size())
                for (int position = 0; position <= route.size(); position++) {
                    long objectiveChange = calculateObjectiveChangeAtPosition(
                        candidate, position, route, distMatrix);
                    
                    if (objectiveChange < bestObjectiveChange) {
                        bestObjectiveChange = objectiveChange;
                        bestNode = candidate;
                        bestPosition = position;
                    }
                }
            }
            
            // Insert best node at best position
            if (bestNode != null) {
                route.add(bestPosition, bestNode);
                selectedNodes.add(bestNode);
                unselectedNodes.remove(bestNode);
            }
        }
        
        return new TSPSolution(instance, selectedNodes, route);
    }
    
    /**
     * Calculate objective function change when inserting a node at a specific position.
     */
    private long calculateObjectiveChangeAtPosition(int newNode, int position,
                                                    List<Integer> currentRoute, DistanceMatrix distMatrix) {
        // Cost of adding the new node
        long nodeCost = instance.getNode(newNode).getCost();
        
        // Distance cost change
        long distanceChange;
        
        if (currentRoute.size() == 1) {
            // Special case: only one node in route
            distanceChange = 2 * distMatrix.getDistance(currentRoute.get(0), newNode);
        } else {
            // Get nodes before and after insertion position
            int prevNode, nextNode;
            
            if (position == 0) {
                // Insert at beginning
                prevNode = currentRoute.get(currentRoute.size() - 1); // Last node (cycle)
                nextNode = currentRoute.get(0); // Current first node
            } else if (position == currentRoute.size()) {
                // Insert at end
                prevNode = currentRoute.get(currentRoute.size() - 1); // Current last node
                nextNode = currentRoute.get(0); // First node (cycle)
            } else {
                // Insert in middle
                prevNode = currentRoute.get(position - 1);
                nextNode = currentRoute.get(position);
            }
            
            // Calculate distance change
            long removedDistance = distMatrix.getDistance(prevNode, nextNode);
            long addedDistance = distMatrix.getDistance(prevNode, newNode) +
                                 distMatrix.getDistance(newNode, nextNode);
            
            distanceChange = addedDistance - removedDistance;
        }
        
        return nodeCost + distanceChange;
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}