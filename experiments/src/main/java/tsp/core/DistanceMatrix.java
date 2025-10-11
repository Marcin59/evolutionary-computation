package tsp.core;

/**
 * Precomputed distance matrix for efficient distance lookups.
 * Only this matrix should be accessed by optimization algorithms, not node coordinates.
 */
public class DistanceMatrix {
    private final int[][] distances;
    private final int size;
    
    public DistanceMatrix(Node[] nodes) {
        this.size = nodes.length;
        this.distances = new int[size][size];
        
        // Precompute all distances
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    distances[i][j] = 0;
                } else {
                    distances[i][j] = nodes[i].distanceTo(nodes[j]);
                }
            }
        }
    }
    
    /**
     * Get distance between two nodes by their IDs.
     */
    public int getDistance(int nodeId1, int nodeId2) {
        return distances[nodeId1][nodeId2];
    }
    
    /**
     * Get the number of nodes in the matrix.
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Get a copy of the distance matrix.
     */
    public int[][] getDistances() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(distances[i], 0, copy[i], 0, size);
        }
        return copy;
    }
}