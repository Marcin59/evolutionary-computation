package tsp.algorithms.LNS;

import tsp.core.Algorithm;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.algorithms.localsearch.SteepestLocalSearch;
import tsp.algorithms.regret.NearestNeighborAnyPositionTwoRegretAlgorithm;
import tsp.core.Instance;
import tsp.core.Solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Large Neighborhood Search (LNS) algorithm for the Traveling Salesperson Problem.
 *
 * The algorithm works as follows:
 * 1. Generate an initial solution and improve it with local search.
 * 2. In a loop, until a time limit is met:
 *    a. Destroy the current solution by removing a fraction of nodes.
 *    b. Repair the solution by re-inserting the removed nodes using a heuristic.
 *    c. Optionally, apply local search to the repaired solution.
 *    d. Accept the new solution if it's better than the current one.
 */
public class LNSAlgorithm extends Algorithm {

    private final double destructionRate;
    private final long timeLimitMs;
    private final boolean useLocalSearchAfterRepair;
    private final LocalSearchAlgorithm.Neighborhood neighborhood;
    private final DestroyHeuristic destroyHeuristic;
    private final Random random;

    private Solution bestSolution;
    private int iterationCount;

    public enum DestroyHeuristic {
        RANDOM_REMOVAL,
        SUBPATH_REMOVAL,
        LONGEST_EDGE_REMOVAL
    }

    /**
     * Constructor for LNSAlgorithm.
     * @param instance The TSP instance.
     * @param destructionRate The fraction of nodes to remove (0.0 to 1.0).
     * @param timeLimitMs The time limit in milliseconds.
     * @param useLocalSearchAfterRepair Whether to apply local search after repairing.
     * @param neighborhood The neighborhood for local search.
     * @param destroyHeuristic The heuristic for the destroy phase.
     * @param seed The random seed.
     */
    public LNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                        boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                        DestroyHeuristic destroyHeuristic, long seed) {
        super("LNS", instance);
        this.destructionRate = destructionRate;
        this.timeLimitMs = timeLimitMs;
        this.useLocalSearchAfterRepair = useLocalSearchAfterRepair;
        this.neighborhood = neighborhood;
        this.destroyHeuristic = destroyHeuristic;
        this.random = new Random(seed);
        this.iterationCount = 0;
    }

    /**
     * Constructor with default random seed.
     */
    public LNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                        boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                        DestroyHeuristic destroyHeuristic) {
        this(instance, destructionRate, timeLimitMs, useLocalSearchAfterRepair, neighborhood,
             destroyHeuristic, System.nanoTime());
    }

    @Override
    public Solution solve() {
        long startTime = System.currentTimeMillis();

        // 1. Generate initial random solution
        RandomSolutionAlgorithm randomAlgorithm = new RandomSolutionAlgorithm(instance, random.nextInt(instance.getTotalNodes()), random.nextLong());
        Solution currentSolution = randomAlgorithm.solve();

        // 2. Apply local search to initial solution (as per assignment)
        currentSolution = applyLocalSearch(new SolutionAlgorithm(currentSolution));
        bestSolution = currentSolution;

        iterationCount = 0;

        // 3. Main LNS loop
        while (System.currentTimeMillis() - startTime < timeLimitMs) {
            Solution x = currentSolution;

            // 4. Destroy
            List<Integer> removedNodes = new ArrayList<>();
            List<Integer> partialTour = new ArrayList<>(x.getRoute());
            destroy(partialTour, removedNodes);

            // 5. Repair using 2-regret heuristic
            Solution y = repair(partialTour, removedNodes);

            // 6. Optional Local Search
            if (useLocalSearchAfterRepair) {
                y = applyLocalSearch(new SolutionAlgorithm(y));
            }

            // 7. Acceptance criterion (accept if better)
            if (y.getObjectiveValue() < x.getObjectiveValue()) {
                currentSolution = y;
                if (y.getObjectiveValue() < bestSolution.getObjectiveValue()) {
                    bestSolution = y;
                }
            }
            
            iterationCount++;
        }

        return bestSolution;
    }

    private Solution applyLocalSearch(Algorithm initialAlgorithm) {
        SteepestLocalSearch localSearch = new SteepestLocalSearch(initialAlgorithm, neighborhood);
        return localSearch.solve();
    }

    private void destroy(List<Integer> tour, List<Integer> removedNodes) {
        int tourSize = tour.size();
        if (tourSize <= 1) return;
        
        int nodesToRemove = (int) (tourSize * destructionRate);
        if (nodesToRemove == 0) nodesToRemove = 1;
        if (nodesToRemove >= tourSize) nodesToRemove = tourSize - 1;

        switch (destroyHeuristic) {
            case RANDOM_REMOVAL:
                destroyRandom(tour, removedNodes, nodesToRemove);
                break;
            case SUBPATH_REMOVAL:
                destroySubpath(tour, removedNodes, nodesToRemove);
                break;
            case LONGEST_EDGE_REMOVAL:
                destroyLongestEdge(tour, removedNodes, nodesToRemove);
                break;
        }
    }
    
    private void destroyRandom(List<Integer> tour, List<Integer> removedNodes, int count) {
        List<Integer> tempTour = new ArrayList<>(tour);
        Collections.shuffle(tempTour, random);
        for (int i = 0; i < count; i++) {
            removedNodes.add(tempTour.get(i));
        }
        tour.removeAll(removedNodes);
    }
    
    private void destroySubpath(List<Integer> tour, List<Integer> removedNodes, int count) {
        if (tour.isEmpty()) return;
        int start = random.nextInt(tour.size());
        for (int i = 0; i < count; i++) {
            int index = (start + i) % tour.size();
            removedNodes.add(tour.get(index));
        }
        // To avoid ConcurrentModificationException or re-indexing issues
        Set<Integer> toRemoveSet = new HashSet<>(removedNodes);
        tour.removeAll(toRemoveSet);
    }

    private void destroyLongestEdge(List<Integer> tour, List<Integer> removedNodes, int count) {
        if (tour.size() < 2) return;
    
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < tour.size(); i++) {
            int u = tour.get(i);
            int v = tour.get((i + 1) % tour.size());
            edges.add(new Edge(u, v, instance.getDistanceMatrix().getDistance(u, v)));
        }
        
        edges.sort(Comparator.comparingDouble(Edge::getLength).reversed());
    
        Set<Integer> nodesForRemoval = new HashSet<>();
        for (Edge edge : edges) {
            if (nodesForRemoval.size() >= count) break;
            
            int nodeToAdd = random.nextBoolean() ? edge.getU() : edge.getV();
            if (tour.size() - nodesForRemoval.size() > 2) { 
                nodesForRemoval.add(nodeToAdd);
            }
        }
        
        removedNodes.addAll(nodesForRemoval);
        tour.removeAll(removedNodes);
    }

    /**
     * Repair the partial tour by inserting nodes using 2-regret heuristic (w1_1).
     * Considers ALL unselected nodes for insertion, not just the removed ones.
     * Reuses NearestNeighborAnyPositionTwoRegretAlgorithm with weights 1 and 1.
     */
    private Solution repair(List<Integer> partialTour, List<Integer> removedNodes) {
        NearestNeighborAnyPositionTwoRegretAlgorithm regretAlgorithm = 
            new NearestNeighborAnyPositionTwoRegretAlgorithm(
                instance, 
                partialTour, 
                instance.getRequiredNodes(),  // target route size
                1,  // weightInsertion
                1   // weightRegret
            );
        return regretAlgorithm.solve();
    }
    
    private static class Edge {
        private final int u, v;
        private final double length;
        
        public Edge(int u, int v, double length) {
            this.u = u;
            this.v = v;
            this.length = length;
        }
        
        public int getU() { return u; }
        public int getV() { return v; }
        public double getLength() { return length; }
    }

    private static class SolutionAlgorithm extends Algorithm {
        private final Solution solution;
        public SolutionAlgorithm(Solution solution) {
            super("Wrapper", solution.getInstance());
            this.solution = solution;
        }
        @Override
        public Solution solve() {
            return solution;
        }
    }
    
    @Override
    public String getName() {
        return String.format("LNS_d-%.2f_%s_ls-%s_hood-%s",
                destructionRate, destroyHeuristic,
                useLocalSearchAfterRepair ? "On" : "Off", neighborhood);
    }

    public int getIterationCount() {
        return iterationCount;
    }
}

