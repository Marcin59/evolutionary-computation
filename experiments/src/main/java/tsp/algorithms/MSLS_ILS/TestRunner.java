package tsp.algorithms.MSLS_ILS;

import tsp.algorithms.MSLS_ILS.*;
import tsp.algorithms.localsearch.*;
import tsp.core.*;

/**
 * Simple test runner for MSLS and ILS algorithms.
 * This can be used to quickly test the implementation.
 */
public class TestRunner {
    
    public static void main(String[] args) {
        // Create a small test instance (you'd normally load from file)
        System.out.println("MSLS and ILS Algorithm Test");
        System.out.println("============================\n");
        
        // This is just a placeholder - in actual use, load from CSV
        System.out.println("To test the algorithms:");
        System.out.println("1. Load an instance using InstanceLoader");
        System.out.println("2. Create MSLS or ILS algorithm with desired parameters");
        System.out.println("3. Call solve() to get the solution");
        System.out.println("\nExample usage:");
        System.out.println("  AlgorithmMSLS msls = new AlgorithmMSLS(");
        System.out.println("    instance, 100, AlgorithmMSLS.LocalSearchType.STEEPEST,");
        System.out.println("    LocalSearchAlgorithm.Neighborhood.NODE_SWAP, 10000, seed);");
        System.out.println("  Solution solution = msls.solve();");
        System.out.println("\n  AlgorithmILS ils = new AlgorithmILS(");
        System.out.println("    instance, AlgorithmILS.LocalSearchType.GREEDY,");
        System.out.println("    LocalSearchAlgorithm.Neighborhood.TWO_OPT, 10, 10000, seed);");
        System.out.println("  Solution solution = ils.solve();");
    }
}
