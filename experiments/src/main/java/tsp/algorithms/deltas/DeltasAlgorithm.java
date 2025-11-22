package tsp.algorithms.deltas;

import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.core.*;
import java.util.*;

/**
 * Local Search with Delta Evaluation for TSP.
 * 
 * This algorithm uses cached delta values to efficiently find the best improving move
 * without recalculating all deltas in each iteration. After applying a move, only
 * the affected deltas are updated.
 * 
 * Algorithm:
 * 1. Start with an initial solution
 * 2. Initialize delta matrix for all possible moves
 * 3. In each iteration:
 *    a. Find the best (most negative) delta from the cached matrix
 *    b. Apply the best improving move
 *    c. Update only the deltas affected by the move
 * 4. Repeat until no improving move exists (local optimum)
 * 
 * Time complexity:
 * - Initial delta calculation: O(n²) for node swaps or O(n²) for 2-opt
 * - Finding best move: O(n²)
 * - Updating deltas: O(n) for affected moves
 * 
 * This is more efficient than recalculating all deltas (O(n³)) in each iteration.
 */
public class DeltasAlgorithm extends LocalSearchAlgorithm {
    
    // Cache for intra-route moves (swap or 2-opt)
    private Move[][] intraRouteMatrix;
    private long[][] intraRouteDeltaMatrix;
    
    // Cache for inter-route moves (replace node)
    private Move[][] interRouteMatrix;
    private long[][] interRouteDeltaMatrix;

    public DeltasAlgorithm(Algorithm initialSolutionAlgorithm, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("DeltasAlgorithm", initialSolutionAlgorithm, neighborhood);
    }

    public DeltasAlgorithm(Algorithm initialSolutionAlgorithm, long seed, LocalSearchAlgorithm.Neighborhood neighborhood) {
        super("DeltasAlgorithm", initialSolutionAlgorithm, seed, neighborhood);
    }
    
    @Override
    public Solution solve() {
        // Get initial solution
        currentSolution = initialSolutionAlgorithm.solve();
        
        // Initialize move and delta matrices
        initializeMatrices();
        
        // Perform local search with delta updates
        boolean improved = true;
        while (improved) {
            improved = performIteration();
        }
        
        return currentSolution;
    }
    
    /**
     * Initialize the move and delta matrices for all possible moves.
     */
    private void initializeMatrices() {
        int n = currentSolution.getRoute().size();
        int totalNodes = instance.getTotalNodes();
        
        // Initialize intra-route moves
        intraRouteMatrix = new Move[n][n];
        intraRouteDeltaMatrix = new long[n][n];
        
        if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
            // Create and cache all node swap moves
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Move move = new NodeSwapMove(i, j);
                    intraRouteMatrix[i][j] = move;
                    intraRouteDeltaMatrix[i][j] = calculateMoveDelta(move);
                }
            }
        } else { // TWO_OPT
            // Create and cache all 2-opt moves
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    if (i == 0 && j == n - 1) continue; // Skip full reversal
                    Move move = new TwoOptMove(i, j);
                    intraRouteMatrix[i][j] = move;
                    intraRouteDeltaMatrix[i][j] = calculateMoveDelta(move);
                }
            }
        }
        
        // Initialize inter-route moves (replace node)
        interRouteMatrix = new Move[n][totalNodes];
        interRouteDeltaMatrix = new long[n][totalNodes];
        
        Set<Integer> selected = currentSolution.getSelectedNodes();
        for (int pos = 0; pos < n; pos++) {
            for (int node = 0; node < totalNodes; node++) {
                if (!selected.contains(node)) {
                    Move move = new ReplaceNodeMove(pos, node);
                    interRouteMatrix[pos][node] = move;
                    interRouteDeltaMatrix[pos][node] = calculateMoveDelta(move);
                }
            }
        }
    }
    
    /**
     * Find and apply the best improving move, then update affected deltas.
     */
    @Override
    protected boolean performIteration() {
        int n = currentSolution.getRoute().size();
        int totalNodes = instance.getTotalNodes();
        
        // Find best move from both intra and inter-route matrices
        Move bestMove = null;
        long bestDelta = 0;
        int bestI = -1, bestJ = -1;
        boolean isInterRoute = false;
        
        // Check intra-route moves
        if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (intraRouteDeltaMatrix[i][j] < bestDelta) {
                        bestDelta = intraRouteDeltaMatrix[i][j];
                        bestI = i;
                        bestJ = j;
                        isInterRoute = false;
                    }
                }
            }
        } else { // TWO_OPT
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    if (i == 0 && j == n - 1) continue;
                    if (intraRouteDeltaMatrix[i][j] < bestDelta) {
                        bestDelta = intraRouteDeltaMatrix[i][j];
                        bestI = i;
                        bestJ = j;
                        isInterRoute = false;
                    }
                }
            }
        }
        
        // Check inter-route moves (replace node)
        for (int pos = 0; pos < n; pos++) {
            for (int node = 0; node < totalNodes; node++) {
                if (interRouteMatrix[pos][node] != null && interRouteDeltaMatrix[pos][node] < bestDelta) {
                    bestDelta = interRouteDeltaMatrix[pos][node];
                    bestI = pos;
                    bestJ = node;
                    isInterRoute = true;
                }
            }
        }
        
        // No improving move found
        if (bestI == -1) {
            return false;
        }
        
        // Apply the best move
        if (isInterRoute) {
            currentSolution = applyMove(interRouteMatrix[bestI][bestJ]);
            updateDeltasAfterReplaceNode(bestI, bestJ);
        } else {
            currentSolution = applyMove(intraRouteMatrix[bestI][bestJ]);
            updateDeltasAfterIntraRouteMove(bestI, bestJ);
        }
        
        return true;
    }
    
    /**
     * Update deltas after an intra-route move (swap or 2-opt).
     */
    private void updateDeltasAfterIntraRouteMove(int pos1, int pos2) {
        int n = currentSolution.getRoute().size();
        Set<Integer> affectedPositions = new HashSet<>();
        
        if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
            // For node swap: positions that swapped and their neighbors are affected
            affectedPositions.add(pos1);
            affectedPositions.add(pos2);
            affectedPositions.add((pos1 - 1 + n) % n);
            affectedPositions.add((pos1 + 1) % n);
            affectedPositions.add((pos2 - 1 + n) % n);
            affectedPositions.add((pos2 + 1) % n);
            
            // Update all intra-route moves involving affected positions
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (affectedPositions.contains(i) || affectedPositions.contains(j)) {
                        intraRouteMatrix[i][j] = new NodeSwapMove(i, j);
                        intraRouteDeltaMatrix[i][j] = calculateMoveDelta(intraRouteMatrix[i][j]);
                    }
                }
            }
        } else { // TWO_OPT
            // For 2-opt: all positions in reversed segment and neighbors are affected
            for (int k = pos1; k <= pos2 + 1; k++) {
                affectedPositions.add(k % n);
            }
            
            // Update all 2-opt moves involving affected positions
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    if (i == 0 && j == n - 1) continue;
                    
                    // Check if edge (i, i+1) or (j, j+1) is affected
                    boolean iAffected = affectedPositions.contains(i) || affectedPositions.contains((i + 1) % n);
                    boolean jAffected = affectedPositions.contains(j) || affectedPositions.contains((j + 1) % n);
                    
                    if (iAffected || jAffected) {
                        intraRouteMatrix[i][j] = new TwoOptMove(i, j);
                        intraRouteDeltaMatrix[i][j] = calculateMoveDelta(intraRouteMatrix[i][j]);
                    }
                }
            }
        }
        
        // Update inter-route moves for affected positions
        updateInterRouteDeltasForPositions(affectedPositions);
    }
    
    /**
     * Update deltas after replacing a node (inter-route move).
     */
    private void updateDeltasAfterReplaceNode(int pos, int newNode) {
        int n = currentSolution.getRoute().size();
        int totalNodes = instance.getTotalNodes();
        Set<Integer> selected = currentSolution.getSelectedNodes();
        
        // Affected positions: the replaced position and its neighbors
        Set<Integer> affectedPositions = new HashSet<>();
        affectedPositions.add(pos);
        affectedPositions.add((pos - 1 + n) % n);
        affectedPositions.add((pos + 1) % n);
        
        // Update intra-route moves for affected positions
        if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (affectedPositions.contains(i) || affectedPositions.contains(j)) {
                        intraRouteMatrix[i][j] = new NodeSwapMove(i, j);
                        intraRouteDeltaMatrix[i][j] = calculateMoveDelta(intraRouteMatrix[i][j]);
                    }
                }
            }
        } else { // TWO_OPT
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    if (i == 0 && j == n - 1) continue;
                    
                    boolean iAffected = affectedPositions.contains(i) || affectedPositions.contains((i + 1) % n);
                    boolean jAffected = affectedPositions.contains(j) || affectedPositions.contains((j + 1) % n);
                    
                    if (iAffected || jAffected) {
                        intraRouteMatrix[i][j] = new TwoOptMove(i, j);
                        intraRouteDeltaMatrix[i][j] = calculateMoveDelta(intraRouteMatrix[i][j]);
                    }
                }
            }
        }
        
        // Update all inter-route moves (selected/unselected nodes changed)
        for (int p = 0; p < n; p++) {
            for (int node = 0; node < totalNodes; node++) {
                if (!selected.contains(node)) {
                    interRouteMatrix[p][node] = new ReplaceNodeMove(p, node);
                    interRouteDeltaMatrix[p][node] = calculateMoveDelta(interRouteMatrix[p][node]);
                } else {
                    interRouteMatrix[p][node] = null;
                    interRouteDeltaMatrix[p][node] = Long.MAX_VALUE;
                }
            }
        }
    }
    
    /**
     * Update inter-route deltas for specific positions.
     */
    private void updateInterRouteDeltasForPositions(Set<Integer> positions) {
        int totalNodes = instance.getTotalNodes();
        Set<Integer> selected = currentSolution.getSelectedNodes();
        
        for (int pos : positions) {
            for (int node = 0; node < totalNodes; node++) {
                if (!selected.contains(node)) {
                    interRouteMatrix[pos][node] = new ReplaceNodeMove(pos, node);
                    interRouteDeltaMatrix[pos][node] = calculateMoveDelta(interRouteMatrix[pos][node]);
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Deltas_" + neighborhoodChoice + "_" + initialSolutionAlgorithm.getName();
    }
}
