package tsp.algorithms.localsearch;

import tsp.core.*;
import java.util.*;

/**
 * Candidate Moves local search for TSP.
 * 
 * This algorithm uses a candidate list strategy to improve efficiency by limiting
 * the neighborhood to most promising moves based on nearest neighbors.
 * 
 * Pseudocode:
 * 1. Start with an initial solution
 * 2. For each node in the solution, maintain a list of k nearest neighbors (candidates)
 * 3. In each iteration:
 *    a. Generate moves only between nodes and their candidates (instead of all pairs)
 *    b. Evaluate candidate moves to find improving moves
 *    c. Apply the best improving move found
 * 4. Repeat until no improving move is found among candidates (local optimum)
 * 
 * The candidate list approach significantly reduces computational complexity
 * from O(n²) to O(n*k) where k << n, while maintaining solution quality
 * since most beneficial moves typically involve nearby nodes.
 * 
 * Neighborhood types:
 * - Node position swaps (intra-route) - only with candidate neighbors
 * - 2-opt edge exchanges (inter-route) - only with candidate edges
 */
public class CandidateMovesAlgorithm extends LocalSearchAlgorithm {
    
    private static final int DEFAULT_CANDIDATE_LIST_SIZE = 10;
    private final int candidateListSize;
    private Map<Integer, List<Integer>> candidateLists;

    public CandidateMovesAlgorithm(Algorithm initialSolutionAlgorithm, LocalSearchAlgorithm.Neighborhood neighborhood) {
        this(initialSolutionAlgorithm, neighborhood, DEFAULT_CANDIDATE_LIST_SIZE);
    }

    public CandidateMovesAlgorithm(Algorithm initialSolutionAlgorithm, long seed, LocalSearchAlgorithm.Neighborhood neighborhood) {
        this(initialSolutionAlgorithm, seed, neighborhood, DEFAULT_CANDIDATE_LIST_SIZE);
    }
    
    public CandidateMovesAlgorithm(Algorithm initialSolutionAlgorithm, LocalSearchAlgorithm.Neighborhood neighborhood, int candidateListSize) {
        super("CandidateMovesAlgorithm", initialSolutionAlgorithm, neighborhood);
        this.candidateListSize = candidateListSize;
        this.candidateLists = null;
    }

    public CandidateMovesAlgorithm(Algorithm initialSolutionAlgorithm, long seed, LocalSearchAlgorithm.Neighborhood neighborhood, int candidateListSize) {
        super("CandidateMovesAlgorithm", initialSolutionAlgorithm, seed, neighborhood);
        this.candidateListSize = candidateListSize;
        this.candidateLists = null;
    }
    
    @Override
    public Solution solve() {
        // Build candidate lists before starting search
        buildCandidateLists();
        return super.solve();
    }
    
    /**
     * Build candidate lists: for each node, find k nearest neighbors.
     */
    private void buildCandidateLists() {
        candidateLists = new HashMap<>();
        DistanceMatrix distMatrix = instance.getDistanceMatrix();
        int totalNodes = instance.getTotalNodes();
        
        for (int node = 0; node < totalNodes; node++) {
            // Create list of (neighbor, distance) pairs
            List<NodeDistance> neighbors = new ArrayList<>();
            for (int other = 0; other < totalNodes; other++) {
                if (node != other) {
                    neighbors.add(new NodeDistance(other, distMatrix.getDistance(node, other)));
                }
            }
            
            // Sort by distance and take k nearest
            neighbors.sort(Comparator.comparingInt(nd -> nd.distance));
            List<Integer> candidates = new ArrayList<>();
            int limit = Math.min(candidateListSize, neighbors.size());
            for (int i = 0; i < limit; i++) {
                candidates.add(neighbors.get(i).nodeId);
            }
            
            candidateLists.put(node, candidates);
        }
    }
    
    /**
     * Generate candidate moves using only candidate lists.
     * For each node in route, iterate through its candidates and create moves
     * that ensure an edge between the node and candidate exists in the resulting solution.
     */
    private List<Move> generateCandidateMoves() {
        List<Move> moves = new ArrayList<>();
        List<Integer> route = currentSolution.getRoute();
        Set<Integer> selectedNodes = currentSolution.getSelectedNodes();
        int n = route.size();
        
        // Build position map for quick lookup
        Map<Integer, Integer> nodeToPosition = new HashMap<>();
        for (int i = 0; i < n; i++) {
            nodeToPosition.put(route.get(i), i);
        }
        
        // For each node in the route
        for (int i = 0; i < n; i++) {
            int currentNode = route.get(i);
            int nextPos = (i + 1) % n;
            int nextNode = route.get(nextPos);
            int prevPos = (i - 1 + n) % n;
            int prevNode = route.get(prevPos);
            
            List<Integer> candidates = candidateLists.get(currentNode);
            
            // Iterate through each candidate of the current node
            for (int candidate : candidates) {
                
                if (selectedNodes.contains(candidate)) {
                    // Candidate is in the route - generate intra-route moves
                    int j = nodeToPosition.get(candidate);
                    
                    if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
                        // Swap the next node with the candidate (unless next node IS the candidate)
                        if (candidate != nextNode) {
                            moves.add(new NodeSwapMove(nextPos, j));
                            moves.add(new NodeSwapMove(prevPos, j)); // Also consider swapping with previous node
                        }
                    } else { // TWO_OPT
                        // For 2-OPT: create an edge between currentNode and candidate
                        // Skip if candidate is already adjacent (prev or next)
                        if (candidate != nextNode && candidate != prevNode) {
                            // TwoOptMove(i, j) creates edges (i, j) and (i+1, j+1)
                            int pos1 = Math.min(i, j);
                            int pos2 = Math.max(i, j);
                            if (pos2 - pos1 > 1) {
                                moves.add(new TwoOptMove(pos1, pos2)); // _ i _ _ j _ -> _ i j _ _ _
                                moves.add(new TwoOptMove(pos1 - 1, pos2 - 1)); // _ _ i _ _ j -> _ _ _ _ _ i j
                            }
                        }
                    }
                } else {
                    // Candidate is not in the route - generate replace move
                    // Replace next neighbor - candidate will be adjacent to currentNode  
                    moves.add(new ReplaceNodeMove(nextPos, candidate));
                    moves.add(new ReplaceNodeMove(prevPos, candidate));
                }
            }
        }
        
        return moves;
    }
    
    @Override
    protected boolean performIteration() {
        // Generate candidate moves only
        List<Move> candidateMoves = generateCandidateMoves();
        
        // Find the best improving move
        Move bestMove = null;
        long bestDelta = 0; // We only accept negative deltas (improvements)
        
        for (Move move : candidateMoves) {
            long delta = calculateMoveDelta(move);
            
            if (delta < bestDelta) {
                bestDelta = delta;
                bestMove = move;
            }
        }
        
        // If we found an improving move, apply it
        if (bestMove != null) {
            currentSolution = applyMove(bestMove);
            return true;
        }
        
        // No improvement found - local optimum reached
        return false;
    }
    
    @Override
    public String getName() {
        return "CandidateMoves_" + neighborhoodChoice + "_" + initialSolutionAlgorithm.getName();
    }
    
    /**
     * Helper class to store node and distance pairs for sorting.
     */
    private static class NodeDistance {
        final int nodeId;
        final int distance;
        
        NodeDistance(int nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}
