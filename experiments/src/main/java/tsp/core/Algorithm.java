package tsp.core;

/**
 * Abstract base class for TSP solving algorithms.
 */
public abstract class Algorithm {
    protected final String name;
    protected final Instance instance;
    
    public Algorithm(String name, Instance instance) {
        this.name = name;
        this.instance = instance;
    }
    
    /**
     * Solve the TSP instance and return the best solution found.
     */
    public abstract Solution solve();
    
    /**
     * Get algorithm name for identification and reporting.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the instance this algorithm is solving.
     */
    public Instance getInstance() {
        return instance;
    }
}