package tsp.core;

import java.util.List;
import java.util.Set;

/**
 * Abstract base class for TSP solutions.
 * Represents a solution as a set of selected nodes and their visiting order.
 */
public abstract class Solution {
    protected final Instance instance;
    protected final Set<Integer> selectedNodes;
    protected final List<Integer> route;
    protected Long objectiveValue;
    
    public Solution(Instance instance, Set<Integer> selectedNodes, List<Integer> route) {
        this.instance = instance;
        this.selectedNodes = Set.copyOf(selectedNodes);
        this.route = List.copyOf(route);
        this.objectiveValue = null;
        
        validateSolution();
    }
    
    /**
     * Validates that the solution meets problem constraints.
     */
    private void validateSolution() {
        // Check that exactly the required number of nodes are selected
        if (selectedNodes.size() != instance.getRequiredNodes()) {
            throw new IllegalArgumentException(
                String.format("Solution must select exactly %d nodes, but selected %d", 
                             instance.getRequiredNodes(), selectedNodes.size()));
        }
        
        // Check that route contains exactly the selected nodes
        if (route.size() != selectedNodes.size()) {
            throw new IllegalArgumentException("Route size must match selected nodes count");
        }
        
        // Check that route contains only selected nodes
        for (Integer nodeId : route) {
            if (!selectedNodes.contains(nodeId)) {
                throw new IllegalArgumentException(
                    "Route contains node " + nodeId + " which is not selected");
            }
        }
        
        // Check that all selected nodes are in the route
        for (Integer nodeId : selectedNodes) {
            if (!route.contains(nodeId)) {
                throw new IllegalArgumentException(
                    "Selected node " + nodeId + " is not in the route");
            }
        }
    }
    
    /**
     * Calculate and cache the objective value.
     */
    public long getObjectiveValue() {
        if (objectiveValue == null) {
            objectiveValue = calculateObjectiveValue();
        }
        return objectiveValue;
    }
    
    /**
     * Calculate objective value: total path length + total node costs.
     */
    private long calculateObjectiveValue() {
        long totalPathLength = 0;
        long totalNodeCosts = 0;
        
        // Calculate path length (including return to start)
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        for (int i = 0; i < route.size(); i++) {
            int currentNode = route.get(i);
            int nextNode = route.get((i + 1) % route.size()); // Wrap around for cycle
            totalPathLength += distMatrix.getDistance(currentNode, nextNode);
        }
        
        // Calculate total node costs
        for (Integer nodeId : selectedNodes) {
            totalNodeCosts += instance.getNode(nodeId).getCost();
        }
        
        return totalPathLength + totalNodeCosts;
    }
    
    /**
     * Get path length component of objective function.
     */
    public long getPathLength() {
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        long pathLength = 0;
        
        for (int i = 0; i < route.size(); i++) {
            int currentNode = route.get(i);
            int nextNode = route.get((i + 1) % route.size());
            pathLength += distMatrix.getDistance(currentNode, nextNode);
        }
        
        return pathLength;
    }
    
    /**
     * Get node costs component of objective function.
     */
    public long getNodeCosts() {
        long totalCosts = 0;
        for (Integer nodeId : selectedNodes) {
            totalCosts += instance.getNode(nodeId).getCost();
        }
        return totalCosts;
    }
    
    /**
     * Check if this solution is valid.
     */
    public boolean isValid() {
        try {
            validateSolution();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    // Getters
    public Instance getInstance() {
        return instance;
    }
    
    public Set<Integer> getSelectedNodes() {
        return selectedNodes;
    }
    
    public List<Integer> getRoute() {
        return route;
    }
    
    /**
     * Create a copy of this solution (for modification by algorithms).
     */
    public abstract Solution copy();
    
    @Override
    public String toString() {
        return String.format("Solution{nodes=%d/%d, objective=%d, path=%d, costs=%d}",
                           selectedNodes.size(), instance.getTotalNodes(), 
                           getObjectiveValue(), getPathLength(), getNodeCosts());
    }
}