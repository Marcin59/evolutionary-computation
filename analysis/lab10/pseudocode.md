ALNS(instance, timeLimit, destructionRate):
    // Initialize
    x_current = randomSolution() + localSearch()
    x_best = x_current
    weights[RANDOM, SUBPATH, LONGEST_EDGE] = [1.0, 1.0, 1.0]
    scores[] = [0, 0, 0], usageCounts[] = [0, 0, 0]
    temperature = 0.05 * x_current.objective / ln(2)

    while (time < timeLimit):
        // Select operator (roulette wheel)
        destroyOp = selectWeighted(weights)

        // Destroy & Repair
        partial, removed = destroy(x_current, destructionRate, destroyOp)
        y = repair(partial)  // 2-regret heuristic
        y = localSearch(y)

        // Score calculation
        score = 0
        if y.objective < x_best.objective:
            x_best = y
            score = 33  // sigma1: new global best
        elif y.objective < x_current.objective:
            score = 9   // sigma2: improvement

        // Simulated Annealing acceptance
        delta = y.objective - x_current.objective
        if delta < 0 OR random() < exp(-delta/temperature):
            x_current = y
            if delta >= 0: score = 13  // sigma3: accepted worse

        // Track operator performance
        usageCounts[destroyOp]++
        scores[destroyOp] += score

        // Update weights every 100 iterations
        if iteration % 100 == 0:
            for each op:
                if usageCounts[op] > 0:
                    avgScore = scores[op] / usageCounts[op]
                    weights[op] = weights[op] * 0.9 + 0.1 * avgScore
                    weights[op] = max(weights[op], 0.1)
            reset scores and usageCounts

        temperature *= 0.9995  // cooling

    return x_best
