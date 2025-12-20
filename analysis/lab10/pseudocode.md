# ALNS (Adaptive Large Neighborhood Search) Pseudocode

## Algorithm Overview

```
Algorithm: ALNS for TSP with Node Selection
Input: Instance, destructionRate, timeLimit, useLocalSearch
Output: Best solution found

1. INITIALIZATION
   x_current = GenerateRandomSolution()
   x_current = LocalSearch(x_current)
   x_best = x_current

   // Initialize operator weights (equal)
   destroyWeights = [1.0, 1.0, 1.0]  // RANDOM, SUBPATH, COST_WEIGHTED_ROULETTE
   repairWeights = [1.0, 1.0]        // TWO_REGRET, GREEDY_CYCLE

   // Initialize SA temperature (~50% acceptance for 5% worse)
   temperature = 0.05 * x_current.cost / ln(2)

   // Operator statistics (reset each segment)
   destroyScores = [0, 0, 0]
   repairScores = [0, 0]
   destroyUsage = [0, 0, 0]
   repairUsage = [0, 0]

2. MAIN LOOP (while time < timeLimit)

   2.1 SELECT OPERATORS (Roulette Wheel)
       destroyOp = RouletteSelect(destroyWeights)
       repairOp = RouletteSelect(repairWeights)

   2.2 DESTROY
       partialTour = Destroy(x_current, destroyOp, destructionRate)

   2.3 REPAIR
       x_new = Repair(partialTour, repairOp)

   2.4 LOCAL SEARCH (optional)
       if useLocalSearch:
           x_new = SteepestLocalSearch(x_new)

   2.5 SCORE CALCULATION
       delta = x_new.cost - x_current.cost
       score = 0

       if x_new.cost < x_best.cost:
           x_best = x_new
           score = sigma1 (= 3.0)  // New global best
       else if delta < 0:
           score = sigma2 (= 2.0)  // Improved current

   2.6 ACCEPTANCE (Simulated Annealing)
       if delta < 0:
           accept = true
       else:
           p = exp(-delta / temperature)
           if random() < p:
               accept = true
               score = sigma3 (= 1.0)  // Accepted worse
           else:
               accept = false

       if accept:
           x_current = x_new

   2.7 UPDATE STATISTICS
       destroyUsage[destroyOp]++
       destroyScores[destroyOp] += score
       repairUsage[repairOp]++
       repairScores[repairOp] += score
       iteration++

   2.8 UPDATE WEIGHTS (every segmentLength iterations)
       if iteration % segmentLength == 0:
           UpdateWeights()

   2.9 COOL TEMPERATURE
       temperature = max(temperature * coolingRate, 0.001)

3. RETURN x_best
```

## Destroy Operators

### 1. RANDOM_REMOVAL
```
function DestroyRandom(tour, count):
    shuffled = shuffle(tour)
    removed = shuffled[0:count]
    return tour - removed
```

### 2. SUBPATH_REMOVAL
```
function DestroySubpath(tour, count):
    start = random(0, tour.length)
    removed = []
    for i = 0 to count:
        idx = (start + i) % tour.length
        removed.add(tour[idx])
    return tour - removed
```

### 3. COST_WEIGHTED_ROULETTE (new - replaces LONGEST_EDGE)
```
function DestroyCostWeightedRoulette(tour, count):
    removed = {}

    while |removed| < count AND |tour| - |removed| > 2:
        candidates = []
        costs = []
        totalCost = 0

        for each node in tour:
            if node not in removed:
                prev = tour[(index(node) - 1) % tour.length]
                next = tour[(index(node) + 1) % tour.length]

                // Cost = node cost + incoming edge + outgoing edge
                cost = node.cost + distance(prev, node) + distance(node, next)

                candidates.add(node)
                costs.add(cost)
                totalCost += cost

        // Roulette wheel selection (higher cost = higher probability)
        spin = random() * totalCost
        cumulative = 0
        for i = 0 to candidates.length:
            cumulative += costs[i]
            if cumulative >= spin:
                removed.add(candidates[i])
                break

    return tour - removed
```

## Repair Operators

### 1. TWO_REGRET
```
function RepairWith2Regret(partialTour):
    // Uses NearestNeighborAnyPositionTwoRegretAlgorithm
    // with weightInsertion=1, weightRegret=1

    while |route| < requiredNodes:
        bestNode = null
        bestScore = -infinity

        for each unselected node:
            // Find best and second-best insertion positions
            costs = []
            for each position in route:
                insertionCost = nodeCost + addedEdges - removedEdge
                costs.add(insertionCost)

            sort(costs)
            regret = costs[1] - costs[0]  // 2-regret
            score = weightRegret * regret - weightInsertion * costs[0]

            if score > bestScore:
                bestScore = score
                bestNode = node

        insert bestNode at best position

    return route
```

### 2. GREEDY_CYCLE (new)
```
function RepairWithGreedyCycle(partialTour):
    route = partialTour
    selected = set(partialTour)
    unselected = allNodes - selected

    while |selected| < requiredNodes:
        bestNode = null
        bestPosition = null
        bestCost = infinity

        for each candidate in unselected:
            nodeCost = candidate.cost

            for each position in route:
                nodeA = route[position]
                nodeB = route[(position + 1) % route.length]

                removedDist = distance(nodeA, nodeB)
                addedDist = distance(nodeA, candidate) + distance(candidate, nodeB)
                insertionCost = nodeCost + addedDist - removedDist

                if insertionCost < bestCost:
                    bestCost = insertionCost
                    bestNode = candidate
                    bestPosition = position + 1

        route.insert(bestPosition, bestNode)
        selected.add(bestNode)
        unselected.remove(bestNode)

    return route
```

## Weight Update Mechanism

```
function UpdateWeights():
    rho = reactionFactor  // How quickly weights adapt (default: 0.1)
    minWeight = 0.1       // Prevent operator starvation

    // Update destroy operator weights
    for i = 0 to destroyWeights.length:
        if destroyUsage[i] > 0:
            avgScore = destroyScores[i] / destroyUsage[i]
            destroyWeights[i] = destroyWeights[i] * (1 - rho) + rho * avgScore
            destroyWeights[i] = max(destroyWeights[i], minWeight)

        // Reset for next segment
        destroyUsage[i] = 0
        destroyScores[i] = 0

    // Update repair operator weights
    for i = 0 to repairWeights.length:
        if repairUsage[i] > 0:
            avgScore = repairScores[i] / repairUsage[i]
            repairWeights[i] = repairWeights[i] * (1 - rho) + rho * avgScore
            repairWeights[i] = max(repairWeights[i], minWeight)

        // Reset for next segment
        repairUsage[i] = 0
        repairScores[i] = 0
```

## Roulette Wheel Selection

```
function RouletteSelect(weights):
    totalWeight = sum(weights)
    r = random() * totalWeight
    cumulative = 0

    for i = 0 to weights.length:
        cumulative += weights[i]
        if r <= cumulative:
            return i

    return weights.length - 1  // Fallback
```

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| destructionRate | 0.25 | Fraction of nodes to remove (25%) |
| reactionFactor (rho) | 0.1 | Weight adaptation speed |
| segmentLength | 100 | Iterations between weight updates |
| coolingRate | 0.9995 | SA temperature decay per iteration |
| sigma1 | 3.0 | Reward for new global best |
| sigma2 | 2.0 | Reward for improving current |
| sigma3 | 1.0 | Reward for accepting worse |
| minWeight | 0.1 | Minimum operator weight |
