package tsp.algorithms.greedy;

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
public class GreedyCycleAlgorithm extends Algorithm {
    private final int startNode;
    
    public GreedyCycleAlgorithm(Instance instance, int startNode) {
        super("GreedyCycle", instance);
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
        
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        
        // Step 1: Select starting vertex (already provided via constructor)
        route.add(startNode);
        selectedNodes.add(startNode);
        unselectedNodes.remove(startNode);
        
        // Step 2: Choose the nearest vertex and create incomplete cycle
        if (selectedNodes.size() < instance.getRequiredNodes()) {
            Integer nearestNode = null;
            long nearestObjectiveChange = Long.MAX_VALUE;
            
            // Find the nearest vertex based on objective function improvement
            for (Integer candidate : unselectedNodes) {
                long objectiveChange = instance.getNode(candidate).getCost() +
                                       2 * distMatrix.getDistance(startNode, candidate);
                
                if (objectiveChange < nearestObjectiveChange) {
                    nearestObjectiveChange = objectiveChange;
                    nearestNode = candidate;
                }
            }
            
            // Add the nearest vertex to form initial incomplete cycle
            if (nearestNode != null) {
                route.add(nearestNode);
                selectedNodes.add(nearestNode);
                unselectedNodes.remove(nearestNode);
            }
        }
        
        // Step 3: Repeat - insert vertices causing smallest increase in cycle length
        while (selectedNodes.size() < instance.getRequiredNodes()) {
            Integer bestNode = null;
            int bestInsertionIndex = -1;
            long bestObjectiveChange = Long.MAX_VALUE;
            
            // Try inserting each unselected node at each possible edge
            for (Integer candidate : unselectedNodes) {
                // Try inserting after each node in current route
                for (int i = 0; i < route.size(); i++) {
                    long objectiveChange = calculateInsertionCost(
                        candidate, i, route, distMatrix);
                    
                    if (objectiveChange < bestObjectiveChange) {
                        bestObjectiveChange = objectiveChange;
                        bestNode = candidate;
                        bestInsertionIndex = i;
                    }
                }
            }
            
            // Insert best node at best position
            if (bestNode != null) {
                route.add(bestInsertionIndex + 1, bestNode);
                selectedNodes.add(bestNode);
                unselectedNodes.remove(bestNode);
            }
        }
        
        return new TSPSolution(instance, selectedNodes, route);
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
        int nodeA = currentRoute.get(afterIndex);
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