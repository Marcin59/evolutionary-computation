package tsp.algorithms.localsearch;

import tsp.core.*;
import java.util.*;

/**
 * Abstract base class for local search algorithms.
 * Supports both steepest and greedy (first improvement) variants.
 * 
 * The search space includes:
 * - Intra-route: swapping positions of two nodes in the route
 * - Inter-route: exchanging two nodes between different positions
 * - Inter-route: exchanging two edges (2-opt operation)
 */
public abstract class LocalSearchAlgorithm extends Algorithm {
    protected final Algorithm initialSolutionAlgorithm;
    protected final Random random;
    protected Solution currentSolution;
    protected int iterationCount;
    protected final Neighborhood neighborhoodChoice;
    
    public enum Neighborhood {
        NODE_SWAP,
        TWO_OPT
    }

    public LocalSearchAlgorithm(String name, Algorithm initialSolutionAlgorithm) {
        this(name, initialSolutionAlgorithm, Neighborhood.NODE_SWAP);
    }

    public LocalSearchAlgorithm(String name, Algorithm initialSolutionAlgorithm, long seed) {
        this(name, initialSolutionAlgorithm, seed, Neighborhood.NODE_SWAP);
    }

    public LocalSearchAlgorithm(String name, Algorithm initialSolutionAlgorithm, Neighborhood neighborhoodChoice) {
        super(name, initialSolutionAlgorithm.getInstance());
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.random = new Random();
        this.iterationCount = 0;
        this.neighborhoodChoice = neighborhoodChoice;
    }

    public LocalSearchAlgorithm(String name, Algorithm initialSolutionAlgorithm, long seed, Neighborhood neighborhoodChoice) {
        super(name, initialSolutionAlgorithm.getInstance());
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.random = new Random(seed);
        this.iterationCount = 0;
        this.neighborhoodChoice = neighborhoodChoice;
    }
    
    @Override
    public Solution solve() {
        // Get initial solution
        currentSolution = initialSolutionAlgorithm.solve();
        iterationCount = 0;
        
        // Perform local search
        boolean improved = true;
        while (improved) {
            improved = performIteration();
            iterationCount++;
        }
        
        return currentSolution;
    }
    
    /**
     * Perform one iteration of local search.
     * @return true if improvement was found, false otherwise
     */
    protected abstract boolean performIteration();
    
    /**
     * Generate all possible neighbors using all neighborhood operators.
     */
    protected List<Move> generateAllMoves() {
        List<Move> moves = new ArrayList<>();
        List<Integer> route = currentSolution.getRoute();
        int n = route.size();
        // Intra-route: swap positions of two nodes OR 2-opt edge exchanges (exclusive)
        if (neighborhoodChoice == Neighborhood.NODE_SWAP) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    moves.add(new NodeSwapMove(i, j));
                }
            }
        } else { // TWO_OPT
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    // Avoid reversing the entire route
                    if (i == 0 && j == n - 1) continue;
                    moves.add(new TwoOptMove(i, j));
                }
            }
        }

        // Inter-route: change one node in the route to one outside the route
        // (replace a selected node by a currently unselected node)
        Set<Integer> selected = currentSolution.getSelectedNodes();
        int total = instance.getTotalNodes();
        for (int pos = 0; pos < n; pos++) {
            for (int node = 0; node < total; node++) {
                if (!selected.contains(node)) {
                    moves.add(new ReplaceNodeMove(pos, node));
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Calculate the change in objective value for a given move without creating a new solution.
     */
    protected long calculateMoveDelta(Move move) {
        return move.calculateDelta(currentSolution, instance.getDistanceMatrix());
    }
    
    /**
     * Apply a move to create a new solution.
     */
    protected Solution applyMove(Move move) {
        return move.apply(currentSolution);
    }
    
    /**
     * Shuffle a list in place using the algorithm's random generator.
     */
    protected <T> void shuffle(List<T> list) {
        Collections.shuffle(list, random);
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    @Override
    public String getName() {
        return super.getName() + "_" + initialSolutionAlgorithm.getName();
    }
    
    /**
     * Interface for neighborhood moves.
     */
    public interface Move {
        /**
         * Calculate the change in objective value if this move is applied.
         */
        long calculateDelta(Solution solution, DistanceMatrix distMatrix);
        
        /**
         * Apply this move to create a new solution.
         */
        Solution apply(Solution solution);
    }
    
    /**
     * Swap two nodes at given positions in the route.
     */
    public static class NodeSwapMove implements Move {
        private final int pos1;
        private final int pos2;
        
        public NodeSwapMove(int pos1, int pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
        
        @Override
        public long calculateDelta(Solution solution, DistanceMatrix distMatrix) {
            List<Integer> route = solution.getRoute();
            int n = route.size();
            
            // If positions are adjacent, calculate differently
            if (Math.abs(pos1 - pos2) == 1 || (pos1 == 0 && pos2 == n - 1) || (pos2 == 0 && pos1 == n - 1)) {
                return calculateAdjacentSwapDelta(route, distMatrix);
            }
            
            // Get the nodes
            int node1 = route.get(pos1);
            int node2 = route.get(pos2);
            
            // Get neighbors (with wrap-around for cycle)
            int prev1 = route.get((pos1 - 1 + n) % n);
            int next1 = route.get((pos1 + 1) % n);
            int prev2 = route.get((pos2 - 1 + n) % n);
            int next2 = route.get((pos2 + 1) % n);
            
            // Old edges
            long oldDist = distMatrix.getDistance(prev1, node1) + 
                          distMatrix.getDistance(node1, next1) +
                          distMatrix.getDistance(prev2, node2) + 
                          distMatrix.getDistance(node2, next2);
            
            // New edges (after swap)
            long newDist = distMatrix.getDistance(prev1, node2) + 
                          distMatrix.getDistance(node2, next1) +
                          distMatrix.getDistance(prev2, node1) + 
                          distMatrix.getDistance(node1, next2);
            
            return newDist - oldDist;
        }
        
        private long calculateAdjacentSwapDelta(List<Integer> route, DistanceMatrix distMatrix) {
            int n = route.size();
            int first = Math.min(pos1, pos2);
            int second = Math.max(pos1, pos2);
            
            // Handle wrap-around case
            if (first == 0 && second == n - 1) {
                first = n - 1;
                second = 0;
            }
            
            int node1 = route.get(first);
            int node2 = route.get(second);
            int prev = route.get((first - 1 + n) % n);
            int next = route.get((second + 1) % n);
            
            // Old: prev -> node1 -> node2 -> next
            long oldDist = distMatrix.getDistance(prev, node1) + 
                          distMatrix.getDistance(node1, node2) +
                          distMatrix.getDistance(node2, next);
            
            // New: prev -> node2 -> node1 -> next
            long newDist = distMatrix.getDistance(prev, node2) + 
                          distMatrix.getDistance(node2, node1) +
                          distMatrix.getDistance(node1, next);
            
            return newDist - oldDist;
        }
        
        @Override
        public Solution apply(Solution solution) {
            List<Integer> newRoute = new ArrayList<>(solution.getRoute());
            // Swap the nodes at the two positions
            int temp = newRoute.get(pos1);
            newRoute.set(pos1, newRoute.get(pos2));
            newRoute.set(pos2, temp);
            
            return new TSPSolution(solution.getInstance(), solution.getSelectedNodes(), newRoute);
        }
        
        @Override
        public String toString() {
            return "NodeSwap(" + pos1 + ", " + pos2 + ")";
        }
    }
    
    /**
     * 2-opt move: reverse a segment of the route.
     * This exchanges two edges in the cycle.
     */
    public static class TwoOptMove implements Move {
        private final int i;
        private final int j;
        
        public TwoOptMove(int i, int j) {
            this.i = i;
            this.j = j;
        }
        
        @Override
        public long calculateDelta(Solution solution, DistanceMatrix distMatrix) {
            List<Integer> route = solution.getRoute();
            int n = route.size();
            
            // Current edges: (i, i+1) and (j, j+1)
            int node_i = route.get((i + n) % n); // handle wrap-around
            int node_i_plus_1 = route.get((i + 1) % n);
            int node_j = route.get((j + n) % n);
            int node_j_plus_1 = route.get((j + 1) % n);
            
            // Old distance
            long oldDist = distMatrix.getDistance(node_i, node_i_plus_1) + 
                          distMatrix.getDistance(node_j, node_j_plus_1);
            
            // New edges: (i, j) and (i+1, j+1)
            long newDist = distMatrix.getDistance(node_i, node_j) + 
                          distMatrix.getDistance(node_i_plus_1, node_j_plus_1);
            
            return newDist - oldDist;
        }
        
        @Override
        public Solution apply(Solution solution) {
            List<Integer> route = solution.getRoute();
            List<Integer> newRoute = new ArrayList<>(route);
            
            // Reverse the segment from i+1 to j (inclusive)
            int left = i + 1;
            int right = j;
            
            while (left < right) {
                int temp = newRoute.get(left);
                newRoute.set(left, newRoute.get(right));
                newRoute.set(right, temp);
                left++;
                right--;
            }
            
            return new TSPSolution(solution.getInstance(), solution.getSelectedNodes(), newRoute);
        }
        
        @Override
        public String toString() {
            return "TwoOpt(" + i + ", " + j + ")";
        }
    }

    /**
     * Replace a node at position `pos` in the route with an outside node (not currently selected).
     */
    public static class ReplaceNodeMove implements Move {
        private final int pos;
        private final int outsideNode;

        public ReplaceNodeMove(int pos, int outsideNode) {
            this.pos = pos;
            this.outsideNode = outsideNode;
        }

        @Override
        public long calculateDelta(Solution solution, DistanceMatrix distMatrix) {
            List<Integer> route = solution.getRoute();
            int n = route.size();
            int oldNode = route.get(pos);

            int prev = route.get((pos - 1 + n) % n);
            int next = route.get((pos + 1) % n);

            long oldEdges = distMatrix.getDistance(prev, oldNode) + distMatrix.getDistance(oldNode, next);
            long newEdges = distMatrix.getDistance(prev, outsideNode) + distMatrix.getDistance(outsideNode, next);

            // Node costs difference
            long oldCost = solution.getInstance().getNode(oldNode).getCost();
            long newCost = solution.getInstance().getNode(outsideNode).getCost();

            return (newEdges - oldEdges) + (newCost - oldCost);
        }

        @Override
        public Solution apply(Solution solution) {
            List<Integer> newRoute = new ArrayList<>(solution.getRoute());
            int oldNode = newRoute.get(pos);
            newRoute.set(pos, outsideNode);

            Set<Integer> newSelected = new HashSet<>(solution.getSelectedNodes());
            newSelected.remove(oldNode);
            newSelected.add(outsideNode);

            return new TSPSolution(solution.getInstance(), newSelected, newRoute);
        }

        @Override
        public String toString() {
            return "ReplaceNode(pos=" + pos + ", outside=" + outsideNode + ")";
        }
    }
}
