package tsp.algorithms.evolutionary;

import tsp.core.*;
import tsp.algorithms.localsearch.*;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import tsp.algorithms.regret.NearestNeighborAnyPositionTwoRegretAlgorithm;

import java.util.*;

/**
 * Hybrid Evolutionary Algorithm (HEA) for TSP.
 * 
 * Assignment 9 requirements:
 * 1. Steady-state selection - offspring replaces worst individual if better
 * 2. Population stored in priority queue (sorted by objective value)
 * 3. Two recombination operators available:
 *    - Operator 1: Preserve common nodes/edges as subpaths, fill rest randomly, connect subpaths randomly
 *    - Operator 2: Take one parent, remove non-common nodes, repair using 2-regret heuristic
 * 4. Local search applied to each offspring (can be disabled for Operator 2)
 * 5. Duplicates removed by comparing objective values (scores)
 * 6. Time-limited execution
 */
public class HybridEvolutionaryAlgorithm extends Algorithm {
    
    public enum RecombinationOperator {
        OPERATOR_1,  // Common subpaths + random fill + random connection
        OPERATOR_2   // Parent selection + remove non-common + repair with heuristic
    }
    
    private final int populationSize;
    private final long timeLimitMs;
    private final LocalSearchAlgorithm.Neighborhood neighborhood;
    private final Random random;
    private final RecombinationOperator recombinationOperator;
    private final boolean useLocalSearchAfterRecombination;
    
    // Population stored as a priority queue (min-heap by objective value)
    private TreeSet<ScoredSolution> population;
    private Set<Long> objectiveValuesInPopulation;
    
    private Solution bestSolution;
    private int iterationCount;
    private int localSearchCallCount;
    
    /**
     * Wrapper class to store solutions with their scores for the priority queue.
     */
    private static class ScoredSolution implements Comparable<ScoredSolution> {
        private static long nextId = 0;
        
        final Solution solution;
        final long objectiveValue;
        final long id;
        
        ScoredSolution(Solution solution) {
            this.solution = solution;
            this.objectiveValue = solution.getObjectiveValue();
            this.id = nextId++;
        }
        
        /**
         * Constructor with pre-calculated objective value to avoid recalculation.
         */
        ScoredSolution(Solution solution, long objectiveValue) {
            this.solution = solution;
            this.objectiveValue = objectiveValue;
            this.id = nextId++;
        }
        
        @Override
        public int compareTo(ScoredSolution other) {
            int cmp = Long.compare(this.objectiveValue, other.objectiveValue);
            if (cmp != 0) return cmp;
            return Long.compare(this.id, other.id);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScoredSolution that = (ScoredSolution) o;
            return id == that.id;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
    }
    
    /**
     * Full constructor for HybridEvolutionaryAlgorithm.
     */
    public HybridEvolutionaryAlgorithm(Instance instance, int populationSize, long timeLimitMs,
                                        LocalSearchAlgorithm.Neighborhood neighborhood,
                                        RecombinationOperator recombinationOperator,
                                        boolean useLocalSearchAfterRecombination,
                                        long seed) {
        super("HEA", instance);
        this.populationSize = populationSize;
        this.timeLimitMs = timeLimitMs;
        this.neighborhood = neighborhood;
        this.recombinationOperator = recombinationOperator;
        this.useLocalSearchAfterRecombination = useLocalSearchAfterRecombination;
        this.random = new Random(seed);
        this.iterationCount = 0;
        this.localSearchCallCount = 0;
    }
    
    /**
     * Constructor with default local search after recombination (true).
     */
    public HybridEvolutionaryAlgorithm(Instance instance, int populationSize, long timeLimitMs,
                                        LocalSearchAlgorithm.Neighborhood neighborhood,
                                        RecombinationOperator recombinationOperator,
                                        long seed) {
        this(instance, populationSize, timeLimitMs, neighborhood, recombinationOperator, true, seed);
    }
    
    /**
     * Constructor without explicit seed.
     */
    public HybridEvolutionaryAlgorithm(Instance instance, int populationSize, long timeLimitMs,
                                        LocalSearchAlgorithm.Neighborhood neighborhood,
                                        RecombinationOperator recombinationOperator,
                                        boolean useLocalSearchAfterRecombination) {
        this(instance, populationSize, timeLimitMs, neighborhood, recombinationOperator, 
             useLocalSearchAfterRecombination, System.nanoTime());
    }
    
    /**
     * Constructor with defaults (Operator 1, local search enabled).
     */
    public HybridEvolutionaryAlgorithm(Instance instance, int populationSize, long timeLimitMs,
                                        LocalSearchAlgorithm.Neighborhood neighborhood) {
        this(instance, populationSize, timeLimitMs, neighborhood, RecombinationOperator.OPERATOR_1, true, System.nanoTime());
    }
    
    @Override
    public Solution solve() {
        long startTime = System.currentTimeMillis();
        
        // Initialize population (always with local search)
        initializePopulation();
        
        // Track best solution
        bestSolution = population.first().solution;
        iterationCount = 0;
        
        // Main evolutionary loop
        while (System.currentTimeMillis() - startTime < timeLimitMs) {
            // Select two random parents
            ScoredSolution[] parents = selectParents();
            
            // Apply recombination based on selected operator
            Solution offspring;
            if (recombinationOperator == RecombinationOperator.OPERATOR_1) {
                offspring = recombineOperator1(parents[0].solution, parents[1].solution);
            } else {
                offspring = recombineOperator2(parents[0].solution, parents[1].solution);
            }
            
            // Apply local search to offspring (if enabled)
            if (useLocalSearchAfterRecombination) {
                offspring = applyLocalSearch(offspring);
                localSearchCallCount++;
            }
            
            // Check for duplicate by comparing objective value (score)
            long offspringScore = offspring.getObjectiveValue();
            if (objectiveValuesInPopulation.contains(offspringScore)) {
                iterationCount++;
                continue;
            }
            
            // Get worst solution in population
            ScoredSolution worst = population.last();
            
            // If offspring is better than worst, replace worst
            if (offspringScore < worst.objectiveValue) {
                population.remove(worst);
                objectiveValuesInPopulation.remove(worst.objectiveValue);
                
                ScoredSolution scoredOffspring = new ScoredSolution(offspring, offspringScore);
                population.add(scoredOffspring);
                objectiveValuesInPopulation.add(offspringScore);
                
                if (offspringScore < bestSolution.getObjectiveValue()) {
                    bestSolution = offspring;
                }
            }
            
            iterationCount++;
        }
        
        return bestSolution;
    }
    
    /**
     * Initialize the population with random solutions optimized by local search.
     */
    private void initializePopulation() {
        population = new TreeSet<>();
        objectiveValuesInPopulation = new HashSet<>();
        
        while (population.size() < populationSize) {
            RandomSolutionAlgorithm randomAlgorithm = new RandomSolutionAlgorithm(
                instance, random.nextInt(instance.getTotalNodes()), random.nextLong()
            );
            
            // Always apply local search to initial population
            Solution solution = applyLocalSearch(randomAlgorithm.solve());
            localSearchCallCount++;
            
            long score = solution.getObjectiveValue();
            if (!objectiveValuesInPopulation.contains(score)) {
                ScoredSolution scored = new ScoredSolution(solution, score);
                population.add(scored);
                objectiveValuesInPopulation.add(score);
            }
        }
    }
    
    /**
     * Select two random parents from the population.
     */
    private ScoredSolution[] selectParents() {
        List<ScoredSolution> populationList = new ArrayList<>(population);
        
        int idx1 = random.nextInt(populationList.size());
        ScoredSolution parent1 = populationList.get(idx1);
        
        int idx2;
        do {
            idx2 = random.nextInt(populationList.size());
        } while (idx2 == idx1 && populationList.size() > 1);
        ScoredSolution parent2 = populationList.get(idx2);
        
        return new ScoredSolution[] { parent1, parent2 };
    }
    
    // ==================== OPERATOR 1 ====================
    /**
     * Operator 1: Preserve common nodes and edges as subpaths, fill rest randomly,
     * connect subpaths randomly.
     * 
     * 1. Find common nodes between parents
     * 2. Find common edges between parents
     * 3. Build subpaths from common edges (including single-node subpaths for common nodes)
     * 4. Add random nodes to reach 50% selection
     * 5. Connect all subpaths randomly (with random choice of connected end)
     */
    private Solution recombineOperator1(Solution parent1, Solution parent2) {
        Set<Integer> nodes1 = parent1.getSelectedNodes();
        Set<Integer> nodes2 = parent2.getSelectedNodes();
        
        // Find common nodes
        Set<Integer> commonNodes = new HashSet<>(nodes1);
        commonNodes.retainAll(nodes2);
        
        // Build edge sets for both parents
        Set<Edge> edges1 = buildEdgeSet(parent1.getRoute());
        Set<Edge> edges2 = buildEdgeSet(parent2.getRoute());
        
        // Find common edges
        Set<Edge> commonEdges = new HashSet<>(edges1);
        commonEdges.retainAll(edges2);
        
        // Build subpaths from common edges
        List<List<Integer>> subpaths = buildSubpathsFromEdges(commonEdges, commonNodes);
        
        // Add single-node subpaths for common nodes not already in a subpath
        Set<Integer> nodesInSubpaths = new HashSet<>();
        for (List<Integer> subpath : subpaths) {
            nodesInSubpaths.addAll(subpath);
        }
        for (Integer node : commonNodes) {
            if (!nodesInSubpaths.contains(node)) {
                List<Integer> singleNodeSubpath = new ArrayList<>();
                singleNodeSubpath.add(node);
                subpaths.add(singleNodeSubpath);
                nodesInSubpaths.add(node);
            }
        }
        
        // Calculate how many nodes we need (50% of total)
        int requiredNodes = instance.getRequiredNodes();
        int currentNodes = nodesInSubpaths.size();
        int nodesToAdd = requiredNodes - currentNodes;
        
        // Add random nodes (not already selected) to reach required count
        if (nodesToAdd > 0) {
            List<Integer> availableNodes = new ArrayList<>();
            for (int node = 0; node < instance.getTotalNodes(); node++) {
                if (!nodesInSubpaths.contains(node)) {
                    availableNodes.add(node);
                }
            }
            Collections.shuffle(availableNodes, random);
            
            for (int i = 0; i < nodesToAdd && i < availableNodes.size(); i++) {
                List<Integer> singleNodeSubpath = new ArrayList<>();
                singleNodeSubpath.add(availableNodes.get(i));
                subpaths.add(singleNodeSubpath);
            }
        }
        
        // Connect subpaths randomly to form a complete route
        List<Integer> finalRoute = connectSubpathsRandomly(subpaths);
        
        // Create the solution
        Set<Integer> selectedNodes = new HashSet<>(finalRoute);
        return new TSPSolution(instance, selectedNodes, finalRoute);
    }
    
    /**
     * Build subpaths from common edges.
     */
    private List<List<Integer>> buildSubpathsFromEdges(Set<Edge> edges, Set<Integer> validNodes) {
        List<List<Integer>> subpaths = new ArrayList<>();
        
        if (edges.isEmpty()) {
            return subpaths;
        }
        
        // Build adjacency list
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.node1, k -> new ArrayList<>()).add(edge.node2);
            adjacency.computeIfAbsent(edge.node2, k -> new ArrayList<>()).add(edge.node1);
        }
        
        // Build path segments
        Set<Integer> visited = new HashSet<>();
        
        for (Integer startNode : adjacency.keySet()) {
            if (visited.contains(startNode)) continue;
            
            // Find an endpoint (node with degree 1) by walking to the end of the chain
            Integer endpoint = findEndpointByWalking(startNode, adjacency);
            
            // Build path from endpoint
            List<Integer> segment = new ArrayList<>();
            buildPathSegment(endpoint, null, adjacency, visited, segment);
            
            if (!segment.isEmpty()) {
                subpaths.add(segment);
            }
        }
        
        return subpaths;
    }
    
    /**
     * Find an endpoint (degree 1 node) by walking along the chain.
     * Since common edges form linear paths (each node has degree 1 or 2),
     * we just walk in one direction until we hit an endpoint.
     */
    private Integer findEndpointByWalking(Integer start, Map<Integer, List<Integer>> adjacency) {
        // If start already has degree 1, it's an endpoint
        List<Integer> neighbors = adjacency.get(start);
        if (neighbors == null || neighbors.size() <= 1) {
            return start;
        }
        
        // Walk in one direction until we find a degree-1 node
        Integer current = start;
        Integer prev = null;
        
        while (true) {
            neighbors = adjacency.get(current);
            if (neighbors.size() == 1) {
                return current; // Found endpoint
            }
            
            // Move to the next node (not the one we came from)
            Integer next = null;
            for (Integer neighbor : neighbors) {
                if (!neighbor.equals(prev)) {
                    next = neighbor;
                    break;
                }
            }
            
            if (next == null) {
                return current; // Shouldn't happen, but safety check
            }
            
            prev = current;
            current = next;
        }
    }
    
    /**
     * Build a path segment following edges.
     */
    private void buildPathSegment(Integer current, Integer prev, Map<Integer, List<Integer>> adjacency,
                                   Set<Integer> visited, List<Integer> segment) {
        visited.add(current);
        segment.add(current);
        
        List<Integer> neighbors = adjacency.get(current);
        if (neighbors != null) {
            for (Integer neighbor : neighbors) {
                if (!visited.contains(neighbor) && !neighbor.equals(prev)) {
                    buildPathSegment(neighbor, current, adjacency, visited, segment);
                    break;
                }
            }
        }
    }
    
    /**
     * Connect subpaths randomly to form a complete tour.
     * Each subpath can be connected from either end, chosen randomly.
     */
    private List<Integer> connectSubpathsRandomly(List<List<Integer>> subpaths) {
        if (subpaths.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Shuffle the order of subpaths
        List<List<Integer>> shuffledSubpaths = new ArrayList<>(subpaths);
        Collections.shuffle(shuffledSubpaths, random);
        
        List<Integer> result = new ArrayList<>();
        
        for (List<Integer> subpath : shuffledSubpaths) {
            // Randomly decide whether to reverse this subpath
            List<Integer> orientedSubpath;
            if (random.nextBoolean()) {
                orientedSubpath = new ArrayList<>(subpath);
            } else {
                orientedSubpath = new ArrayList<>(subpath);
                Collections.reverse(orientedSubpath);
            }
            
            result.addAll(orientedSubpath);
        }
        
        return result;
    }
    
    // ==================== OPERATOR 2 ====================
    /**
     * Operator 2: Take one parent as starting solution, remove nodes not present
     * in the other parent, repair using 2-regret heuristic.
     * 
     * This preserves the order and traversal direction from one parent.
     */
    private Solution recombineOperator2(Solution parent1, Solution parent2) {
        // Randomly choose which parent to use as base
        Solution baseParent, otherParent;
        if (random.nextBoolean()) {
            baseParent = parent1;
            otherParent = parent2;
        } else {
            baseParent = parent2;
            otherParent = parent1;
        }
        
        Set<Integer> otherNodes = otherParent.getSelectedNodes();
        List<Integer> baseRoute = baseParent.getRoute();
        
        // Keep only nodes that are present in both parents (preserving order from base)
        List<Integer> partialRoute = new ArrayList<>();
        for (Integer node : baseRoute) {
            if (otherNodes.contains(node)) {
                partialRoute.add(node);
            }
        }
        
        // Repair using 2-regret heuristic (same as LNS)
        return repairWithRegret(partialRoute);
    }
    
    /**
     * Repair a partial solution using 2-regret heuristic.
     */
    private Solution repairWithRegret(List<Integer> partialRoute) {
        NearestNeighborAnyPositionTwoRegretAlgorithm regretAlgorithm = 
            new NearestNeighborAnyPositionTwoRegretAlgorithm(
                instance, 
                new ArrayList<>(partialRoute), 
                instance.getRequiredNodes(),
                1,  // weightInsertion
                1   // weightRegret
            );
        return regretAlgorithm.solve();
    }
    
    // ==================== COMMON UTILITIES ====================
    
    /**
     * Represents an undirected edge.
     */
    private static class Edge {
        final int node1;
        final int node2;
        
        Edge(int a, int b) {
            this.node1 = Math.min(a, b);
            this.node2 = Math.max(a, b);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return node1 == edge.node1 && node2 == edge.node2;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(node1, node2);
        }
    }
    
    /**
     * Build set of edges from a route (considering cyclic nature).
     */
    private Set<Edge> buildEdgeSet(List<Integer> route) {
        Set<Edge> edges = new HashSet<>();
        int n = route.size();
        for (int i = 0; i < n; i++) {
            int from = route.get(i);
            int to = route.get((i + 1) % n);
            edges.add(new Edge(from, to));
        }
        return edges;
    }
    
    /**
     * Apply local search to improve a solution.
     */
    private Solution applyLocalSearch(Solution solution) {
        Algorithm wrapper = new Algorithm("Wrapper", instance) {
            @Override
            public Solution solve() {
                return solution;
            }
        };
        
        SteepestLocalSearch localSearch = new SteepestLocalSearch(wrapper, neighborhood);
        return localSearch.solve();
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    public int getLocalSearchCallCount() {
        return localSearchCallCount;
    }
    
    public int getPopulationSize() {
        return populationSize;
    }
    
    public RecombinationOperator getRecombinationOperator() {
        return recombinationOperator;
    }
    
    public boolean isUseLocalSearchAfterRecombination() {
        return useLocalSearchAfterRecombination;
    }
    
    @Override
    public String getName() {
        String lsFlag = useLocalSearchAfterRecombination ? "LS" : "noLS";
        return String.format("HEA_%s_%s_pop%d_%s", recombinationOperator, lsFlag, populationSize, neighborhood);
    }
}
