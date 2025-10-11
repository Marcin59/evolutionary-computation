package tsp.core;

/**
 * Represents a node in the TSP problem with coordinates and cost.
 */
public class Node {
    private final int id;
    private final int x;
    private final int y;
    private final int cost;
    
    public Node(int id, int x, int y, int cost) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.cost = cost;
    }
    
    public int getId() {
        return id;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getCost() {
        return cost;
    }
    
    /**
     * Calculates Euclidean distance to another node, rounded to nearest integer.
     */
    public int distanceTo(Node other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }
    
    @Override
    public String toString() {
        return String.format("Node{id=%d, x=%d, y=%d, cost=%d}", id, x, y, cost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return id == node.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}