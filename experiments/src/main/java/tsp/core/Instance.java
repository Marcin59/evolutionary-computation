package tsp.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a TSP instance with nodes and precomputed distance matrix.
 */
public class Instance {
    private final String name;
    private final Node[] nodes;
    private final DistanceMatrix distanceMatrix;
    private final int requiredNodes;
    
    public Instance(String name, Node[] nodes) {
        this.name = name;
        this.nodes = nodes.clone();
        this.distanceMatrix = new DistanceMatrix(nodes);
        // Select exactly 50% of nodes (rounded up if odd)
        this.requiredNodes = (int) Math.ceil(nodes.length / 2.0);
    }
    
    /**
     * Load instance from CSV file with format: x;y;cost
     */
    public static Instance fromFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        String instanceName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        List<String> lines = Files.readAllLines(filePath);
        List<Node> nodeList = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(";");
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                    "Invalid line format at line " + (i + 1) + ": " + line);
            }
            
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int cost = Integer.parseInt(parts[2]);
                nodeList.add(new Node(i, x, y, cost));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid number format at line " + (i + 1) + ": " + line, e);
            }
        }
        
        Node[] nodes = nodeList.toArray(new Node[0]);
        return new Instance(instanceName, nodes);
    }
    
    public String getName() {
        return name;
    }
    
    public Node[] getNodes() {
        return nodes.clone();
    }
    
    public DistanceMatrix getDistanceMatrix() {
        return distanceMatrix;
    }
    
    public int getRequiredNodes() {
        return requiredNodes;
    }
    
    public int getTotalNodes() {
        return nodes.length;
    }
    
    public Node getNode(int id) {
        if (id < 0 || id >= nodes.length) {
            throw new IndexOutOfBoundsException("Node ID " + id + " is out of bounds");
        }
        return nodes[id];
    }
    
    @Override
    public String toString() {
        return String.format("Instance{name='%s', totalNodes=%d, requiredNodes=%d}", 
                           name, getTotalNodes(), requiredNodes);
    }
}