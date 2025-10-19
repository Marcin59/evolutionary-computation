package tsp.algorithms;

import tsp.core.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public abstract class IterativeAlgorithm extends Algorithm {
    protected final int startNode;
    protected DistanceMatrix distanceMatrix;
    public IterativeAlgorithm(String name, Instance instance, int StartNode) {
        super(name, instance);
        this.startNode = StartNode;
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

        distanceMatrix = instance.getDistanceMatrix();

        // Build route by adding nodes at best positions
        while (selectedNodes.size() < instance.getRequiredNodes()) {
            Integer bestNode = null;
            int bestPosition = -1;

            // Find the best node and position to insert
            Map.Entry<Integer, Integer> bestEntry = findBestNodeAndPosition(unselectedNodes, route);
            if (bestEntry != null) {
                bestNode = bestEntry.getKey();
                bestPosition = bestEntry.getValue();
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

    protected abstract Map.Entry<Integer, Integer> findBestNodeAndPosition(
            Set<Integer> unselectedNodes, List<Integer> route);
}
