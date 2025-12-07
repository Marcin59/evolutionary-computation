# Convexity Analysis for TSP Solutions

## Authors
- Adam Tomys 156057
- Marcin Kapiszewski 156048

## Introduction

This report analyzes the convexity and similarity patterns of TSP solutions generated using **Greedy Local Search** applied to **random starting solutions**. 

The method works as follows:
- Generate multiple random starting solutions
- Apply greedy local search to each random solution  
- Analyze the resulting local optima using edge-based and node-based similarity metrics

The analysis examines three types of solution comparisons:
1. **Average similarity**
2. **Similarity to the best out of the 1000**
3. **Similarity to the best from other algorithms (Large neighbourhood search)**

---

## Instance: TSPA

### Edge Similarity Analysis

#### Average - Edge Similarity
![TSPA Average Edge Similarity](images/convexity/TSPA_avg_edge_similarity.png)

#### Best out of 1000 - Edge Similarity
![TSPA Best 1000 Edge Similarity](images/convexity/TSPA_best1000_edge_similarity.png)

#### Best External - Edge Similarity
![TSPA External Edge Similarity](images/convexity/TSPA_external_edge_similarity.png)

### Node Similarity Analysis

#### Average - Node Similarity
![TSPA Average Node Similarity](images/convexity/TSPA_avg_node_similarity.png)

#### Best out of 1000 - Node Similarity
![TSPA Best 1000 Node Similarity](images/convexity/TSPA_best1000_node_similarity.png)

#### Best External - Node Similarity
![TSPA External Node Similarity](images/convexity/TSPA_external_node_similarity.png)

---

## Instance: TSPB

### Edge Similarity Analysis

#### Average - Edge Similarity
![TSPB Average Edge Similarity](images/convexity/TSPB_avg_edge_similarity.png)

#### Best out of 1000 - Edge Similarity
![TSPB Best 1000 Edge Similarity](images/convexity/TSPB_best1000_edge_similarity.png)

#### Best External - Edge Similarity
![TSPB External Edge Similarity](images/convexity/TSPB_external_edge_similarity.png)

### Node Similarity Analysis

#### Average - Node Similarity
![TSPB Average Node Similarity](images/convexity/TSPB_avg_node_similarity.png)

#### Best out of 1000 - Node Similarity
![TSPB Best 1000 Node Similarity](images/convexity/TSPB_best1000_node_similarity.png)

#### Best External - Node Similarity
![TSPB External Node Similarity](images/convexity/TSPB_external_node_similarity.png)

---

## Summary
The analysis of TSPA and TSPB confirms a "Big Valley" landscape structure, where high-quality local optima are not random but cluster significantly around each other.

### Key Findings:
1. Strong Negative Correlation: Across all scenarios, lower objective values (better solutions) correlate strongly with higher similarity.
2. External Validation: Both instances show strong convergence toward the optimum found in the best (so far) algorithm. Notably, TSPB exhibits high correlations for both edge ($r = -0.65$) and node ($r = -0.69$) similarity to the external best.
3. Edge vs. Node: Edge similarity has typically higher correlation to objection function value than node similarity. However, this is not the case for the TSPB global optimum comparison ($r = -0.69$ vs. $-0.65$).
4. Global vs. Local Gradient: In general solutions are more similar to each other on average than to the best solutions, both generated using the same algorithm as well as the best found so far.

### Conclusions
The solution space is resembling convex space, meaning good solutions share structural features.