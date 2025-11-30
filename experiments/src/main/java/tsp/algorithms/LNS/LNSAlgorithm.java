package tsp.algorithms.LNS;

import tsp.core.Algorithm;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.algorithms.localsearch.SteepestLocalSearch;
import tsp.core.Instance;
import tsp.core.Solution;
import tsp.core.TSPSolution;

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
    private final RepairHeuristic repairHeuristic;
    private final DestroyHeuristic destroyHeuristic;
    private final Random random;

    private Solution bestSolution;
    private int iterationCount;

    public enum RepairHeuristic {
        GREEDY_INSERTION, // Best-insertion
        REGRET_INSERTION  // Regret-k heuristic
    }

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
     * @param repairHeuristic The heuristic for the repair phase.
     * @param destroyHeuristic The heuristic for the destroy phase.
     * @param seed The random seed.
     */
    public LNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                        boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                        RepairHeuristic repairHeuristic, DestroyHeuristic destroyHeuristic, long seed) {
        super("LNS", instance);
        this.destructionRate = destructionRate;
        this.timeLimitMs = timeLimitMs;
        this.useLocalSearchAfterRepair = useLocalSearchAfterRepair;
        this.neighborhood = neighborhood;
        this.repairHeuristic = repairHeuristic;
        this.destroyHeuristic = destroyHeuristic;
        this.random = new Random(seed);
        this.iterationCount = 0;
    }

    /**
     * Constructor with default random seed.
     */
    public LNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                        boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                        RepairHeuristic repairHeuristic, DestroyHeuristic destroyHeuristic) {
        this(instance, destructionRate, timeLimitMs, useLocalSearchAfterRepair, neighborhood,
             repairHeuristic, destroyHeuristic, System.nanoTime());
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

            // 5. Repair
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

    private Solution repair(List<Integer> partialTour, List<Integer> removedNodes) {
        switch (repairHeuristic) {
            case GREEDY_INSERTION:
                return repairGreedy(partialTour, removedNodes);
            case REGRET_INSERTION:
                return repairRegret(partialTour, removedNodes);
            default:
                throw new IllegalStateException("Unknown repair heuristic: " + repairHeuristic);
        }
    }

    private Solution repairGreedy(List<Integer> partialTour, List<Integer> removedNodes) {
        List<Integer> tour = new ArrayList<>(partialTour);
        List<Integer> toInsert = new ArrayList<>(removedNodes);
        
        while (!toInsert.isEmpty()) {
            double bestCostIncrease = Double.POSITIVE_INFINITY;
            int bestNodeIndex = -1;
            int bestPosition = -1;
            
            for (int i = 0; i < toInsert.size(); i++) {
                int nodeToInsert = toInsert.get(i);
                for (int j = 0; j <= tour.size(); j++) {
                    double costIncrease = calculateInsertionCost(tour, nodeToInsert, j);
                    if (costIncrease < bestCostIncrease) {
                        bestCostIncrease = costIncrease;
                        bestNodeIndex = i;
                        bestPosition = j;
                    }
                }
            }
            
            if (bestNodeIndex != -1) {
                tour.add(bestPosition, toInsert.get(bestNodeIndex));
                toInsert.remove(bestNodeIndex);
            } else {
                break;
            }
        }
        
        return new TSPSolution(instance, new HashSet<>(tour), tour);
    }

    private Solution repairRegret(List<Integer> partialTour, List<Integer> removedNodes) {
        List<Integer> tour = new ArrayList<>(partialTour);
        List<Integer> toInsert = new ArrayList<>(removedNodes);
        int regretK = 2;

        while (!toInsert.isEmpty()) {
            double maxRegret = Double.NEGATIVE_INFINITY;
            int bestNodeIndex = -1;
            int bestPosition = -1;

            for (int i = 0; i < toInsert.size(); i++) {
                int node = toInsert.get(i);
                List<InsertionCost> costs = new ArrayList<>();
                for (int j = 0; j <= tour.size(); j++) {
                    costs.add(new InsertionCost(j, calculateInsertionCost(tour, node, j)));
                }
                Collections.sort(costs);

                double regret = 0;
                if (costs.size() >= regretK) {
                    regret = costs.get(1).getCost() - costs.get(0).getCost();
                } else if (!costs.isEmpty()) {
                    regret = costs.get(0).getCost();
                }
                
                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestNodeIndex = i;
                    bestPosition = costs.get(0).getPosition();
                }
            }

            if (bestNodeIndex != -1) {
                tour.add(bestPosition, toInsert.get(bestNodeIndex));
                toInsert.remove(bestNodeIndex);
            } else {
                break;
            }
        }
        
        return new TSPSolution(instance, new HashSet<>(tour), tour);
    }

    private double calculateInsertionCost(List<Integer> tour, int nodeToInsert, int position) {
        if (tour.isEmpty()) {
            return 0;
        }
        int prev, next;
        if (tour.size() == 1) {
            prev = tour.get(0);
            next = tour.get(0);
            return instance.getDistanceMatrix().getDistance(prev, nodeToInsert) * 2;
        }
        if (position == tour.size()) {
             prev = tour.get(tour.size()-1);
             next = tour.get(0);
        } else {
            prev = tour.get((position - 1 + tour.size()) % tour.size());
            next = tour.get(position);
        }

        return instance.getDistanceMatrix().getDistance(prev, nodeToInsert) + instance.getDistanceMatrix().getDistance(nodeToInsert, next) - instance.getDistanceMatrix().getDistance(prev, next);
    }
    
    private static class InsertionCost implements Comparable<InsertionCost> {
        private final int position;
        private final double cost;
        
        public InsertionCost(int position, double cost) {
            this.position = position;
            this.cost = cost;
        }
        
        public int getPosition() { return position; }
        public double getCost() { return cost; }
        
        @Override
        public int compareTo(InsertionCost other) {
            return Double.compare(this.cost, other.cost);
        }
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
        return String.format("LNS_d-%.2f_%s_%s_ls-%s_hood-%s",
                destructionRate, repairHeuristic, destroyHeuristic,
                useLocalSearchAfterRepair ? "On" : "Off", neighborhood);
    }

    public int getIterationCount() {
        return iterationCount;
    }
}

