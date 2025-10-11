package tsp.analysis;

import tsp.core.Algorithm;
import tsp.core.Solution;

/**
 * Represents the result of running an algorithm on an instance.
 */
public class AlgorithmResult {
    private final String algorithmName;
    private final String instanceName;
    private final Solution solution;
    private final long computationTimeMs;
    private final double objectiveValue;
    private final double pathLength;
    private final double nodeCosts;
    private final int selectedNodes;
    private final int totalNodes;
    
    public AlgorithmResult(String algorithmName, String instanceName, Solution solution, long computationTimeMs) {
        this.algorithmName = algorithmName;
        this.instanceName = instanceName;
        this.solution = solution;
        this.computationTimeMs = computationTimeMs;
        this.objectiveValue = solution.getObjectiveValue();
        this.pathLength = solution.getPathLength();
        this.nodeCosts = solution.getNodeCosts();
        this.selectedNodes = solution.getSelectedNodes().size();
        this.totalNodes = solution.getInstance().getTotalNodes();
    }
    
    // Getters
    public String getAlgorithmName() { return algorithmName; }
    public String getInstanceName() { return instanceName; }
    public Solution getSolution() { return solution; }
    public long getComputationTimeMs() { return computationTimeMs; }
    public double getObjectiveValue() { return objectiveValue; }
    public double getPathLength() { return pathLength; }
    public double getNodeCosts() { return nodeCosts; }
    public int getSelectedNodes() { return selectedNodes; }
    public int getTotalNodes() { return totalNodes; }
    
    @Override
    public String toString() {
        return String.format(
            "AlgorithmResult{algorithm='%s', instance='%s', objective=%.2f, time=%dms}",
            algorithmName, instanceName, objectiveValue, computationTimeMs
        );
    }
}