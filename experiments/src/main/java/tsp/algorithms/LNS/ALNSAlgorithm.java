package tsp.algorithms.LNS;

import tsp.core.Algorithm;
import tsp.algorithms.greedy.RandomSolutionAlgorithm;
import tsp.algorithms.greedy.GreedyCycleAlgorithm;
import tsp.algorithms.localsearch.LocalSearchAlgorithm;
import tsp.algorithms.localsearch.SteepestLocalSearch;
import tsp.algorithms.regret.NearestNeighborAnyPositionTwoRegretAlgorithm;
import tsp.core.DistanceMatrix;
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
 * Adaptive Large Neighborhood Search (ALNS) algorithm for the TSP with Node Selection.
 *
 * ALNS extends LNS with:
 * 1. Adaptive operator selection - probabilistically choose destroy operators based on performance
 * 2. Simulated annealing acceptance - accept worse solutions with decreasing probability
 * 3. Weight update mechanism - reward operators that find good solutions
 *
 * Based on: Ropke & Pisinger (2006) "An Adaptive Large Neighborhood Search Heuristic"
 */
public class ALNSAlgorithm extends Algorithm {

    // Core parameters
    private final double destructionRate;
    private final long timeLimitMs;
    private final boolean useLocalSearchAfterRepair;
    private final LocalSearchAlgorithm.Neighborhood neighborhood;
    private final Random random;

    // ALNS-specific parameters
    private final double reactionFactor;      // rho: how quickly weights adapt (0.1 = slow, 0.5 = fast)
    private final int segmentLength;          // iterations between weight updates
    private final double coolingRate;         // temperature cooling per iteration
    private final double sigma1;              // reward for new global best
    private final double sigma2;              // reward for improving current solution
    private final double sigma3;              // reward for accepting worse solution
    private final double minWeight;           // minimum operator weight to prevent starvation

    // Operator weights and statistics
    private double[] destroyWeights;
    private int[] destroyUsageCounts;
    private double[] destroyScores;

    // State
    private Solution bestSolution;
    private double temperature;
    private int iterationCount;

    // Destroy operators (RANDOM, SUBPATH from LNS + COST_WEIGHTED_ROULETTE replacing LONGEST_EDGE)
    public enum DestroyHeuristic {
        RANDOM_REMOVAL,
        SUBPATH_REMOVAL,
        COST_WEIGHTED_ROULETTE  // Replaced LONGEST_EDGE_REMOVAL
    }

    // Repair operators
    public enum RepairHeuristic {
        TWO_REGRET,
        GREEDY_CYCLE
    }

    // Repair operator weights and statistics
    private double[] repairWeights;
    private int[] repairUsageCounts;
    private double[] repairScores;

    /**
     * Constructor with default ALNS parameters.
     */
    public ALNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                         boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                         long seed) {
        this(instance, destructionRate, timeLimitMs, useLocalSearchAfterRepair, neighborhood, seed,
             0.1,      // reactionFactor (rho)
             100,      // segmentLength
             0.9995,   // coolingRate
             3.0,  // sigma1
             2.0,   // sigma2
             1.0,  // sigma3
             0.1       // minWeight
        );
    }

    /**
     * Constructor with full parameter control.
     */
    public ALNSAlgorithm(Instance instance, double destructionRate, long timeLimitMs,
                         boolean useLocalSearchAfterRepair, LocalSearchAlgorithm.Neighborhood neighborhood,
                         long seed, double reactionFactor, int segmentLength, double coolingRate,
                         double sigma1, double sigma2, double sigma3, double minWeight) {
        super("ALNS", instance);
        this.destructionRate = destructionRate;
        this.timeLimitMs = timeLimitMs;
        this.useLocalSearchAfterRepair = useLocalSearchAfterRepair;
        this.neighborhood = neighborhood;
        this.random = new Random(seed);
        this.reactionFactor = reactionFactor;
        this.segmentLength = segmentLength;
        this.coolingRate = coolingRate;
        this.sigma1 = sigma1;
        this.sigma2 = sigma2;
        this.sigma3 = sigma3;
        this.minWeight = minWeight;
        this.iterationCount = 0;

        // Initialize destroy operator weights (equal initially)
        int numDestroyOps = DestroyHeuristic.values().length;
        this.destroyWeights = new double[numDestroyOps];
        this.destroyUsageCounts = new int[numDestroyOps];
        this.destroyScores = new double[numDestroyOps];
        for (int i = 0; i < numDestroyOps; i++) {
            destroyWeights[i] = 1.0;
            destroyUsageCounts[i] = 0;
            destroyScores[i] = 0.0;
        }

        // Initialize repair operator weights (equal initially)
        int numRepairOps = RepairHeuristic.values().length;
        this.repairWeights = new double[numRepairOps];
        this.repairUsageCounts = new int[numRepairOps];
        this.repairScores = new double[numRepairOps];
        for (int i = 0; i < numRepairOps; i++) {
            repairWeights[i] = 1.0;
            repairUsageCounts[i] = 0;
            repairScores[i] = 0.0;
        }
    }

    @Override
    public Solution solve() {
        long startTime = System.currentTimeMillis();

        // 1. Generate initial random solution
        RandomSolutionAlgorithm randomAlgorithm = new RandomSolutionAlgorithm(
            instance, random.nextInt(instance.getTotalNodes()), random.nextLong());
        Solution currentSolution = randomAlgorithm.solve();

        // 2. Apply local search to initial solution
        currentSolution = applyLocalSearch(new SolutionAlgorithm(currentSolution));
        bestSolution = currentSolution;

        // 3. Initialize temperature for simulated annealing
        // Start temperature: ~50% acceptance probability for 5% worse solutions
        temperature = 0.05 * currentSolution.getObjectiveValue() / Math.log(2);

        iterationCount = 0;

        // 4. Main ALNS loop
        while (System.currentTimeMillis() - startTime < timeLimitMs) {
            // 4a. Select destroy operator (roulette wheel based on weights)
            DestroyHeuristic selectedDestroyOp = selectDestroyOperator();
            int destroyOpIndex = selectedDestroyOp.ordinal();

            // 4b. Select repair operator (roulette wheel based on weights)
            RepairHeuristic selectedRepairOp = selectRepairOperator();
            int repairOpIndex = selectedRepairOp.ordinal();

            // 4c. Destroy
            List<Integer> removedNodes = new ArrayList<>();
            List<Integer> partialTour = new ArrayList<>(currentSolution.getRoute());
            destroy(partialTour, removedNodes, selectedDestroyOp);

            // 4d. Repair using selected operator
            Solution newSolution = repair(partialTour, removedNodes, selectedRepairOp);

            // 4e. Optional Local Search
            if (useLocalSearchAfterRepair) {
                newSolution = applyLocalSearch(new SolutionAlgorithm(newSolution));
            }

            // 4f. Calculate operator score based on solution quality
            double operatorScore = 0.0;
            long delta = newSolution.getObjectiveValue() - currentSolution.getObjectiveValue();

            if (newSolution.getObjectiveValue() < bestSolution.getObjectiveValue()) {
                // New global best
                bestSolution = newSolution;
                operatorScore = sigma1;
            } else if (delta < 0) {
                // Improved current solution (but not global best)
                operatorScore = sigma2;
            }

            // 4g. Simulated Annealing acceptance
            boolean accepted = false;
            if (delta < 0) {
                // Always accept improvements
                accepted = true;
            } else {
                // Accept worse solutions with probability exp(-delta/temperature)
                double acceptProbability = Math.exp(-delta / temperature);
                if (random.nextDouble() < acceptProbability) {
                    accepted = true;
                    operatorScore = sigma3;  // Reward for accepting worse (enables exploration)
                }
            }

            if (accepted) {
                currentSolution = newSolution;
            }

            // 4h. Update operator statistics (both destroy and repair)
            destroyUsageCounts[destroyOpIndex]++;
            destroyScores[destroyOpIndex] += operatorScore;
            repairUsageCounts[repairOpIndex]++;
            repairScores[repairOpIndex] += operatorScore;

            // 4i. Update weights every segmentLength iterations
            iterationCount++;
            if (iterationCount % segmentLength == 0) {
                updateWeights();
            }

            // 4j. Cool down temperature
            temperature = Math.max(temperature * coolingRate, 0.001);
        }

        return bestSolution;
    }

    /**
     * Select a destroy operator using roulette wheel selection based on weights.
     */
    private DestroyHeuristic selectDestroyOperator() {
        double totalWeight = 0;
        for (double w : destroyWeights) {
            totalWeight += w;
        }

        double r = random.nextDouble() * totalWeight;
        double cumulative = 0;
        DestroyHeuristic[] operators = DestroyHeuristic.values();

        for (int i = 0; i < operators.length; i++) {
            cumulative += destroyWeights[i];
            if (r <= cumulative) {
                return operators[i];
            }
        }
        return operators[operators.length - 1]; // Fallback
    }

    /**
     * Select a repair operator using roulette wheel selection based on weights.
     */
    private RepairHeuristic selectRepairOperator() {
        double totalWeight = 0;
        for (double w : repairWeights) {
            totalWeight += w;
        }

        double r = random.nextDouble() * totalWeight;
        double cumulative = 0;
        RepairHeuristic[] operators = RepairHeuristic.values();

        for (int i = 0; i < operators.length; i++) {
            cumulative += repairWeights[i];
            if (r <= cumulative) {
                return operators[i];
            }
        }
        return operators[operators.length - 1]; // Fallback
    }

    /**
     * Update operator weights based on their performance in the last segment.
     * Formula: w_new = w_old * (1 - rho) + rho * avgScore
     */
    private void updateWeights() {
        // Update destroy weights
        for (int i = 0; i < destroyWeights.length; i++) {
            if (destroyUsageCounts[i] > 0) {
                double avgScore = destroyScores[i] / destroyUsageCounts[i];
                destroyWeights[i] = destroyWeights[i] * (1 - reactionFactor) + reactionFactor * avgScore;
                destroyWeights[i] = Math.max(destroyWeights[i], minWeight);
            }
            // Reset for next segment
            destroyUsageCounts[i] = 0;
            destroyScores[i] = 0.0;
        }

        // Update repair weights
        for (int i = 0; i < repairWeights.length; i++) {
            if (repairUsageCounts[i] > 0) {
                double avgScore = repairScores[i] / repairUsageCounts[i];
                repairWeights[i] = repairWeights[i] * (1 - reactionFactor) + reactionFactor * avgScore;
                repairWeights[i] = Math.max(repairWeights[i], minWeight);
            }
            // Reset for next segment
            repairUsageCounts[i] = 0;
            repairScores[i] = 0.0;
        }
    }

    private Solution applyLocalSearch(Algorithm initialAlgorithm) {
        SteepestLocalSearch localSearch = new SteepestLocalSearch(initialAlgorithm, neighborhood);
        return localSearch.solve();
    }

    private void destroy(List<Integer> tour, List<Integer> removedNodes, DestroyHeuristic heuristic) {
        int tourSize = tour.size();
        if (tourSize <= 1) return;

        int nodesToRemove = (int) (tourSize * destructionRate);
        if (nodesToRemove == 0) nodesToRemove = 1;
        if (nodesToRemove >= tourSize) nodesToRemove = tourSize - 1;

        switch (heuristic) {
            case RANDOM_REMOVAL:
                destroyRandom(tour, removedNodes, nodesToRemove);
                break;
            case SUBPATH_REMOVAL:
                destroySubpath(tour, removedNodes, nodesToRemove);
                break;
            case COST_WEIGHTED_ROULETTE:
                destroyCostWeightedRoulette(tour, removedNodes, nodesToRemove);
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
        Set<Integer> toRemoveSet = new HashSet<>(removedNodes);
        tour.removeAll(toRemoveSet);
    }

    /**
     * Cost-weighted roulette destroy: nodes with higher cost (node cost + adjacent edge costs)
     * have higher probability of being removed.
     */
    private void destroyCostWeightedRoulette(List<Integer> tour, List<Integer> removedNodes, int count) {
        if (tour.size() < 2) return;

        DistanceMatrix dm = instance.getDistanceMatrix();
        Set<Integer> toRemove = new HashSet<>();

        while (toRemove.size() < count && tour.size() - toRemove.size() > 2) {
            // Calculate cost for each node not yet marked for removal
            List<Integer> candidates = new ArrayList<>();
            List<Double> costs = new ArrayList<>();
            double totalCost = 0;

            for (int i = 0; i < tour.size(); i++) {
                int node = tour.get(i);
                if (toRemove.contains(node)) continue;

                // Cost = node cost + incoming edge + outgoing edge
                int prevIdx = (i - 1 + tour.size()) % tour.size();
                int nextIdx = (i + 1) % tour.size();
                int prevNode = tour.get(prevIdx);
                int nextNode = tour.get(nextIdx);

                double nodeCost = instance.getNode(node).getCost();
                double edgeCost = dm.getDistance(prevNode, node) + dm.getDistance(node, nextNode);
                double cost = nodeCost + edgeCost;

                candidates.add(node);
                costs.add(cost);
                totalCost += cost;
            }

            if (candidates.isEmpty() || totalCost == 0) break;

            // Roulette wheel selection
            double spin = random.nextDouble() * totalCost;
            double cumulative = 0;
            int selectedNode = candidates.get(candidates.size() - 1);

            for (int i = 0; i < candidates.size(); i++) {
                cumulative += costs.get(i);
                if (cumulative >= spin) {
                    selectedNode = candidates.get(i);
                    break;
                }
            }

            toRemove.add(selectedNode);
        }

        removedNodes.addAll(toRemove);
        tour.removeAll(toRemove);
    }

    /**
     * Repair the partial tour using selected repair operator.
     */
    private Solution repair(List<Integer> partialTour, List<Integer> removedNodes, RepairHeuristic heuristic) {
        switch (heuristic) {
            case TWO_REGRET:
                return repairWith2Regret(partialTour);
            case GREEDY_CYCLE:
            default:
                return repairWithGreedyCycle(partialTour);
        }
    }

    /**
     * Repair using 2-regret heuristic with weights (1, 1).
     */
    private Solution repairWith2Regret(List<Integer> partialTour) {
        NearestNeighborAnyPositionTwoRegretAlgorithm regretAlgorithm =
            new NearestNeighborAnyPositionTwoRegretAlgorithm(
                instance,
                partialTour,
                instance.getRequiredNodes(),
                1,  // weightInsertion
                1   // weightRegret
            );
        return regretAlgorithm.solve();
    }

    /**
     * Repair using greedy cycle insertion.
     * Inserts nodes at the position causing minimum cost increase.
     */
    private Solution repairWithGreedyCycle(List<Integer> partialTour) {
        List<Integer> route = new ArrayList<>(partialTour);
        Set<Integer> selectedNodes = new HashSet<>(partialTour);

        // Find unselected nodes
        Set<Integer> unselectedNodes = new HashSet<>();
        for (int i = 0; i < instance.getTotalNodes(); i++) {
            if (!selectedNodes.contains(i)) {
                unselectedNodes.add(i);
            }
        }

        DistanceMatrix dm = instance.getDistanceMatrix();

        // Greedy insertion until we have required number of nodes
        while (selectedNodes.size() < instance.getRequiredNodes()) {
            int bestNode = -1;
            int bestPosition = -1;
            long bestCost = Long.MAX_VALUE;

            for (int candidate : unselectedNodes) {
                long nodeCost = instance.getNode(candidate).getCost();

                for (int pos = 0; pos < route.size(); pos++) {
                    int nodeA = route.get(pos);
                    int nodeB = route.get((pos + 1) % route.size());

                    long removedDist = dm.getDistance(nodeA, nodeB);
                    long addedDist = dm.getDistance(nodeA, candidate) + dm.getDistance(candidate, nodeB);
                    long insertionCost = nodeCost + addedDist - removedDist;

                    if (insertionCost < bestCost) {
                        bestCost = insertionCost;
                        bestNode = candidate;
                        bestPosition = pos + 1;
                    }
                }
            }

            if (bestNode != -1) {
                route.add(bestPosition, bestNode);
                selectedNodes.add(bestNode);
                unselectedNodes.remove(bestNode);
            } else {
                break;
            }
        }

        return new TSPSolution(instance, selectedNodes, route);
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
        return String.format("ALNS_d-%.2f_rho-%.2f_seg-%d_cool-%.4f_ls-%s",
                destructionRate, reactionFactor, segmentLength, coolingRate,
                useLocalSearchAfterRepair ? "On" : "Off");
    }

    public int getIterationCount() {
        return iterationCount;
    }

    /**
     * Get the final destroy weights for analysis purposes.
     */
    public double[] getDestroyWeights() {
        return destroyWeights.clone();
    }

    /**
     * Get the final repair weights for analysis purposes.
     */
    public double[] getRepairWeights() {
        return repairWeights.clone();
    }
}
