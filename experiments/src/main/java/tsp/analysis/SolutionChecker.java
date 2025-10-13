package tsp.analysis;

import tsp.core.*;
import java.util.*;

/**
 * Solution checker utility to validate TSP solutions.
 */
public class SolutionChecker {
    
    /**
     * Check if a solution is valid and return detailed validation results.
     */
    public static ValidationResult validateSolution(Solution solution) {
        ValidationResult result = new ValidationResult();
        Instance instance = solution.getInstance();
        
        // Check 1: Correct number of selected nodes
        int requiredNodes = instance.getRequiredNodes();
        int selectedNodes = solution.getSelectedNodes().size();
        if (selectedNodes != requiredNodes) {
            result.addError(String.format("Wrong number of selected nodes: %d (expected %d)", 
                          selectedNodes, requiredNodes));
        }
        
        // Check 2: Route contains exactly the selected nodes
        Set<Integer> routeNodes = new HashSet<>(solution.getRoute());
        if (!routeNodes.equals(solution.getSelectedNodes())) {
            result.addError("Route nodes don't match selected nodes");
        }
        
        // Check 3: No duplicate nodes in route
        if (solution.getRoute().size() != routeNodes.size()) {
            result.addError("Route contains duplicate nodes");
        }
        
        // Check 4: All nodes are valid (within bounds)
        for (Integer nodeId : solution.getSelectedNodes()) {
            if (nodeId < 0 || nodeId >= instance.getTotalNodes()) {
                result.addError(String.format("Invalid node ID: %d", nodeId));
            }
        }
        
        // Check 5: Objective function calculation
        try {
            long calculatedObjective = recalculateObjective(solution);
            long reportedObjective = solution.getObjectiveValue();
            
            if (calculatedObjective != reportedObjective) {
                result.addWarning(String.format(
                    "Objective function mismatch: calculated=%d, reported=%d",
                    calculatedObjective, reportedObjective));
            }
        } catch (Exception e) {
            result.addError("Error calculating objective function: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Recalculate objective function independently.
     */
    private static long recalculateObjective(Solution solution) {
        Instance instance = solution.getInstance();
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        List<Integer> route = solution.getRoute();
        
        // Calculate path length
        long pathLength = 0;
        for (int i = 0; i < route.size(); i++) {
            int currentNode = route.get(i);
            int nextNode = route.get((i + 1) % route.size());
            pathLength += distMatrix.getDistance(currentNode, nextNode);
        }
        
        // Calculate node costs
        long nodeCosts = 0;
        for (Integer nodeId : solution.getSelectedNodes()) {
            nodeCosts += instance.getNode(nodeId).getCost();
        }
        
        return pathLength + nodeCosts;
    }
    
    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public String getReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation ").append(isValid() ? "PASSED" : "FAILED").append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("ERRORS:\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("WARNINGS:\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}