package tsp.algorithms.greedy;

import tsp.algorithms.IterativeAlgorithm;
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
public class NearestNeighborEndAlgorithm extends IterativeAlgorithm {

    public NearestNeighborEndAlgorithm(Instance instance, int startNode) {
        super("NearestNeighborEnd", instance, startNode);
    }
    
    @Override
    protected Map.Entry<Integer, Integer> findBestNodeAndPosition(
            Set<Integer> unselectedNodes, List<Integer> route
    ) {
        Integer closestNode = null;
        long bestObjectiveChange = Long.MAX_VALUE;

        // Current last node in route
        int lastNode = route.get(route.size() - 1);

        // Find the closest node to the last added node
        for (Integer candidate : unselectedNodes) {
            // Calculate the objective change (node cost + distance to last node)
            long nodeCost = instance.getNode(candidate).getCost();
            long distanceToLast = distanceMatrix.getDistance(lastNode, candidate);
            long objectiveChange = nodeCost + distanceToLast;

            if (objectiveChange < bestObjectiveChange) {
                bestObjectiveChange = objectiveChange;
                closestNode = candidate;
            }
        }

        // Always add at the end of the route
        if (closestNode != null) {
            return new AbstractMap.SimpleEntry<>(closestNode, route.size());
        } else {
            return null;
        }
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}