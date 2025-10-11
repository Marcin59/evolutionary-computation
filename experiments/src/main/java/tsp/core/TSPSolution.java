package tsp.core;

import java.util.List;
import java.util.Set;

/**
 * Concrete implementation of Solution.
 */
public class TSPSolution extends Solution {
    
    public TSPSolution(Instance instance, Set<Integer> selectedNodes, List<Integer> route) {
        super(instance, selectedNodes, route);
    }
    
    @Override
    public Solution copy() {
        return new TSPSolution(instance, selectedNodes, route);
    }
}