package tsp.algorithms.greedy;

import tsp.core.*;
import java.util.*;

/**
 * Random solution generator that selects required number of nodes randomly
 * and creates a random route through them.
 * 
 * Pseudocode:
 * 1. Randomly select required number of nodes from all available nodes
 * 2. Create random permutation of selected nodes as route
 * 3. Return solution with selected nodes and route
 */
public class RandomSolutionAlgorithm extends Algorithm {
    private final Random random;
    private final int startNode;
    
    public RandomSolutionAlgorithm(Instance instance, int startNode) {
        super("RandomSolution", instance);
        this.random = new Random();
        this.startNode = startNode;
    }
    
    public RandomSolutionAlgorithm(Instance instance, int startNode, long seed) {
        super("RandomSolution", instance);
        this.random = new Random(seed);
        this.startNode = startNode;
    }
    
    @Override
    public Solution solve() {
        // Get all available nodes
        List<Integer> allNodes = new ArrayList<>();
        for (int i = 0; i < instance.getTotalNodes(); i++) {
            allNodes.add(i);
        }
        
        // Shuffle to get random selection
        Collections.shuffle(allNodes, random);
        
        // Select required number of nodes
        Set<Integer> selectedNodes = new HashSet<>(
            allNodes.subList(0, instance.getRequiredNodes())
        );
        
        // If startNode is not selected, replace a random selected node with startNode
        if (!selectedNodes.contains(startNode)) {
            Iterator<Integer> iter = selectedNodes.iterator();
            iter.next(); // Remove first node
            iter.remove();
            selectedNodes.add(startNode);
        }
        
        // Create random route through selected nodes
        List<Integer> route = new ArrayList<>(selectedNodes);
        Collections.shuffle(route, random);
        
        return new TSPSolution(instance, selectedNodes, route);
    }
    
    @Override
    public String getName() {
        return super.getName() + "_start" + startNode;
    }
}