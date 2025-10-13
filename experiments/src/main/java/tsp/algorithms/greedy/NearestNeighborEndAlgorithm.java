package tsp.algorithms.greedy;

import tsp.core.*;
import java.util.*;

/**
 * The nearest neighbor construction heuristic for TSP.
 * 
 * Pseudocode:
 * 1. Select (e.g. randomly) the starting vertex
 * 2. Repeat:
 *    - Add to the solution the vertex (and the leading edge) closest to the last one added
 *    Until all vertices have been added
 * 3. Add the edge from the last to the first vertex
 * 
 * Note: "Closest" means the best improvement in objective function (distance + node cost).
 */
public class NearestNeighborEndAlgorithm extends Algorithm {
    private final int startNode;
    
    public NearestNeighborEndAlgorithm(Instance instance, int startNode) {
        super("NearestNeighborEnd", instance);
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
        
        // Step 1: Select starting vertex (already provided via constructor)
        route.add(startNode);
        selectedNodes.add(startNode);
        unselectedNodes.remove(startNode);
        
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        
        // Step 2: Repeat - add closest vertex to the last one added
        while (selectedNodes.size() < instance.getRequiredNodes()) {
            Integer closestNode = null;
            long bestObjectiveChange = Long.MAX_VALUE;
            
            // Current last node in route
            int lastNode = route.get(route.size() - 1);
            
            // Find the closest node to the last added node
            for (Integer candidate : unselectedNodes) {
                // Calculate the objective change (node cost + distance to last node)
                long nodeCost = instance.getNode(candidate).getCost();
                long distanceToLast = distMatrix.getDistance(lastNode, candidate);
                long objectiveChange = nodeCost + distanceToLast;
                
                if (objectiveChange < bestObjectiveChange) {
                    bestObjectiveChange = objectiveChange;
                    closestNode = candidate;
                }
            }
            
            // Add closest node to the route
            if (closestNode != null) {
                route.add(closestNode);
                selectedNodes.add(closestNode);
                unselectedNodes.remove(closestNode);
            }
        }
        
        // Step 3: Edge from last to first vertex is automatically handled by TSPSolution
        return new TSPSolution(instance, selectedNodes, route);
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}