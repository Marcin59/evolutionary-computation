"""
TSP Analysis Utilities

Reusable functions for analyzing TSP algorithm results across different algorithm types.
This module contains functions that can be shared between different analysis notebooks.
"""

import json
from typing import Iterable
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
from scipy import stats
import warnings
import os
from datetime import datetime

warnings.filterwarnings('ignore')

# Set default style for plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

RESULTS_PATH = Path("results")

# ==============================================================================
# DATA LOADING FUNCTIONS
# ==============================================================================

def load_algorithm_results(instance_name, algorithm_folder="greedy"):
    """
    Load algorithm experiment results for a given instance from specified folder.
    
    Args:
        instance_name (str): Name of the TSP instance (e.g., 'TSPA', 'TSPB')
        algorithm_folder (str): Folder name containing algorithm results
        
    Returns:
        tuple: (results_df, viz_data, summary_data) or (None, None, None) if not found
    """
    results_path = RESULTS_PATH / algorithm_folder / f"{instance_name}_{algorithm_folder}_results.json"
    viz_path = RESULTS_PATH / algorithm_folder / f"{instance_name}_visualization.json"
    summary_path = RESULTS_PATH / algorithm_folder / f"{instance_name}_summary.json"
    if not results_path.exists():
        print(f"Results file not found: {results_path}")
        return None, None, None
    
    # Load results
    with open(results_path, 'r') as f:
        results_data = json.load(f)
    
    # Load visualization data
    viz_data = None
    if viz_path.exists():
        with open(viz_path, 'r') as f:
            viz_data = json.load(f)
    
    # Load summary data
    summary_data = None
    if summary_path.exists():
        with open(summary_path, 'r') as f:
            summary_data = json.load(f)
    
    # Convert results to DataFrame
    df = pd.DataFrame(results_data['results'])
    
    print(f"Loaded {len(df)} results for {instance_name} from {algorithm_folder} folder")
    if not df.empty:
        algorithms = sorted(df['algorithm'].str.replace(r'_start\d+', '', regex=True).unique())
        print(f"Algorithms: {algorithms}")
    
    return df, viz_data, summary_data


def load_all_algorithm_results(algorithm_folder="greedy", instances=None):
    """
    Load results for all instances from specified algorithm folder.
    
    Args:
        algorithm_folder (str): Folder name containing algorithm results
        instances (list): List of instance names to load (default: ['TSPA', 'TSPB'])
        
    Returns:
        dict: Dictionary with instance names as keys and data dictionaries as values
    """
    if instances is None:
        instances = ['TSPA', 'TSPB']
    
    all_data = {}
    
    for instance in instances:
        df, viz_data, summary_data = load_algorithm_results(instance, algorithm_folder)
        if df is not None:
            all_data[instance] = {
                'df': df, 
                'viz_data': viz_data, 
                'summary_data': summary_data
            }
    
    return all_data


def load_results(instance_name, results_folder: Path = RESULTS_PATH):
    """
    Load experiment results for a given instance (legacy function for backward compatibility).
    
    Args:
        instance_name (str): Name of the TSP instance
        results_folder (Path): Path to results folder
        
    Returns:
        tuple: (results_df, summary_data) or (None, None) if not found
    """
    results_path = results_folder / f"{instance_name}_results.json"
    
    if not results_path.exists():
        print(f"Results file not found: {results_path}")
        return None, None
    
    with open(results_path, 'r') as f:
        data = json.load(f)
    
    # Convert to DataFrame
    df = pd.DataFrame(data['results'])
    summary = data['summary']
    
    print(f"Loaded {len(df)} results for {instance_name}")
    if not df.empty:
        print(f"Algorithms: {df['algorithm'].unique()}")
    
    return df, summary


def load_all_results(results_folder: Path = RESULTS_PATH, instances=None):
    """
    Load results for all instances (legacy function for backward compatibility).
    
    Args:
        results_folder (Path): Path to results folder
        instances (list): List of instance names to load
        
    Returns:
        dict: Dictionary with instance names as keys and data dictionaries as values
    """
    if instances is None:
        instances = ['TSPA', 'TSPB']
    
    all_data = {}
    
    for instance in instances:
        df, summary = load_results(instance, results_folder)
        if df is not None:
            all_data[instance] = {'df': df, 'summary': summary}
    
    return all_data


# ==============================================================================
# VISUALIZATION FUNCTIONS
# ==============================================================================

def plot_best_solutions(data):
    """
    Create 2D visualizations of best solutions with node costs represented by color and size.
    Each instance is plotted separately with algorithms displayed individually and node order printed after each.
    
    Args:
        data (dict): Dictionary containing algorithm data with visualization information
    """
    for instance_name, instance_data in data.items():
        if 'viz_data' not in instance_data or instance_data['viz_data'] is None:
            print(f"No visualization data available for {instance_name}")
            continue
            
        viz_data = instance_data['viz_data']
        best_solutions = viz_data['best_solutions']
        nodes = viz_data['nodes']
        
        # Prepare node data
        node_coords = {node['id']: (node['x'], node['y']) for node in nodes}
        node_costs = {node['id']: node['cost'] for node in nodes}
        max_cost = max(node['cost'] for node in nodes)
        min_cost = min(node['cost'] for node in nodes)
        
        print(f"\n{'='*80}")
        print(f"{instance_name} - BEST SOLUTIONS")
        print(f"{'='*80}\n")
        
        # Plot each algorithm separately
        for algorithm, solution_data in best_solutions.items():
            # Create individual plot for this algorithm
            fig, ax = plt.subplots(1, 1, figsize=(12, 8))
            
            # Plot all nodes (unselected) in light gray
            for node in nodes:
                x, y = node['x'], node['y']
                cost = node['cost']
                size = 50 + 200 * (cost - min_cost) / (max_cost - min_cost)
                ax.scatter(x, y, c='lightgray', s=size, alpha=0.5, zorder=1)
                ax.text(x, y-50, str(node['id']), ha='center', va='top', 
                       fontsize=6, alpha=0.7)
            
            # Plot selected nodes with cost-based coloring and sizing
            selected_nodes = solution_data['selected_nodes']
            route = solution_data['route']
            
            # Plot selected nodes
            for node_id in selected_nodes:
                x, y = node_coords[node_id]
                cost = node_costs[node_id]
                
                # Size based on cost (normalized)
                size = 50 + 200 * (cost - min_cost) / (max_cost - min_cost)
                
                # Color based on cost
                ax.scatter(x, y, c=cost, s=size, cmap='viridis', 
                          vmin=min_cost, vmax=max_cost, 
                          edgecolors='black', linewidth=1, zorder=3)
            
            # Plot route
            route_coords = [node_coords[node_id] for node_id in route]
            route_coords.append(route_coords[0])  # Close the cycle
            
            route_x = [coord[0] for coord in route_coords]
            route_y = [coord[1] for coord in route_coords]
            
            ax.plot(route_x, route_y, 'r-', linewidth=2, alpha=0.8, zorder=2)
            
            # Add arrows to show direction
            for i in range(len(route)):
                x1, y1 = route_coords[i]
                x2, y2 = route_coords[i + 1]
                dx, dy = x2 - x1, y2 - y1
                ax.annotate('', xy=(x1 + 0.7*dx, y1 + 0.7*dy), xytext=(x1 + 0.3*dx, y1 + 0.3*dy),
                           arrowprops=dict(arrowstyle='->', color='red', lw=1.5), zorder=4)
            
            # Formatting for plot
            validated_text = "✓ VALIDATED" if solution_data['is_validated'] else "✗ VALIDATION FAILED"
            fig.suptitle(f'{instance_name} - {algorithm}', fontsize=16, fontweight='bold')
            ax.set_title(f'Objective: {solution_data["objective_value"]:.2f} | '
                        f'Path: {solution_data["path_length"]:.2f} | Costs: {solution_data["node_costs"]:.2f} | '
                        f'{validated_text}')
            ax.set_xlabel('X Coordinate')
            ax.set_ylabel('Y Coordinate')
            ax.grid(True, alpha=0.3)
            ax.set_aspect('equal')
            
            # Add colorbar
            sm = plt.cm.ScalarMappable(cmap='viridis', norm=plt.Normalize(vmin=min_cost, vmax=max_cost))
            sm.set_array([])
            cbar = fig.colorbar(sm, ax=ax, orientation='horizontal', pad=0.1, shrink=0.8)
            cbar.set_label('Node Cost', fontsize=12)
            
            plt.tight_layout()
            plt.show()
            
            # Print node order information immediately after the graph
            validated = "✓ VALIDATED" if solution_data['is_validated'] else "✗ VALIDATION FAILED"
            
            print(f"\n{algorithm}")
            print("-" * 80)
            print(f"Status: {validated}")
            print(f"Objective Value: {solution_data['objective_value']:.2f}")
            print(f"Path Length: {solution_data['path_length']:.2f}")
            print(f"Node Costs: {solution_data['node_costs']:.2f}")
            print(f"Selected Nodes: {len(selected_nodes)}")
            print(f"\nNode Order (Route):")
            
            # Format route with line breaks for readability (10 nodes per line)
            print(', '.join(str(node) for node in route))
            print("\n")


def plot_objective_distributions(data, blocked_prefixes: Iterable[str] = None):
    """
    Plot objective value distributions for all algorithms and instances - one per row.
    
    Args:
        data (dict): Dictionary containing algorithm data
        blocked_prefixes (Iterable[str], optional): List of algorithm name prefixes to exclude from plots
    """
    # Calculate number of rows - one per instance
    n_instances = len(data)
    
    fig, axes = plt.subplots(n_instances, 1, figsize=(12, 8*n_instances))
    if n_instances == 1:
        axes = [axes]  # Ensure axes is always a list
    
    for idx, (instance_name, instance_data) in enumerate(data.items()):
        df = instance_data['df']
        
        # Create base algorithm column for grouping
        df_plot = df.copy()
        if blocked_prefixes:
            for prefix in blocked_prefixes:
                df_plot = df_plot[~df_plot['algorithm'].str.startswith(prefix)]
        df_plot['base_algorithm'] = df_plot['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        # Combined box and violin plot
        sns.violinplot(data=df_plot, x='base_algorithm', y='objective_value', ax=axes[idx], alpha=0.6)
        sns.boxplot(data=df_plot, x='base_algorithm', y='objective_value', ax=axes[idx], 
                   width=0.3, showcaps=True, boxprops=dict(alpha=0.8))
        
        axes[idx].set_title(f'{instance_name} - Objective Value Distribution', fontsize=14, fontweight='bold')
        axes[idx].set_xlabel('Algorithm', fontsize=12)
        axes[idx].set_ylabel('Objective Value', fontsize=12)
        axes[idx].tick_params(axis='x', rotation=45)
        axes[idx].grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.show()


def plot_objective_components(data):
    """
    Plot the components of objective function (path length vs node costs).
    
    Args:
        data (dict): Dictionary containing algorithm data
    """
    fig, axes = plt.subplots(len(data), 2, figsize=(15, 6*len(data)))
    if len(data) == 1:
        axes = axes.reshape(1, -1)
    
    for idx, (instance_name, instance_data) in enumerate(data.items()):
        df = instance_data['df']
        df_plot = df.copy()
        df_plot['base_algorithm'] = df_plot['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        # Path length comparison
        sns.boxplot(data=df_plot, x='base_algorithm', y='path_length', ax=axes[idx, 0])
        axes[idx, 0].set_title(f'{instance_name} - Path Length Component')
        axes[idx, 0].tick_params(axis='x', rotation=45)
        
        # Node costs comparison
        sns.boxplot(data=df_plot, x='base_algorithm', y='node_costs', ax=axes[idx, 1])
        axes[idx, 1].set_title(f'{instance_name} - Node Costs Component')
        axes[idx, 1].tick_params(axis='x', rotation=45)
    
    plt.tight_layout()
    plt.show()


def plot_computation_time(data):
    """
    Plot computation time analysis.
    
    Args:
        data (dict): Dictionary containing algorithm data
    """
    fig, axes = plt.subplots(len(data), 2, figsize=(15, 6*len(data)))
    if len(data) == 1:
        axes = axes.reshape(1, -1)
    
    for idx, (instance_name, instance_data) in enumerate(data.items()):
        df = instance_data['df']
        df_plot = df.copy()
        df_plot['base_algorithm'] = df_plot['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        # Computation time distribution
        sns.boxplot(data=df_plot, x='base_algorithm', y='computation_time_ms', ax=axes[idx, 0])
        axes[idx, 0].set_title(f'{instance_name} - Computation Time (ms)')
        axes[idx, 0].tick_params(axis='x', rotation=45)
        
        # Time vs Quality scatter
        for algorithm in df_plot['base_algorithm'].unique():
            algo_data = df_plot[df_plot['base_algorithm'] == algorithm]
            axes[idx, 1].scatter(algo_data['computation_time_ms'], 
                               algo_data['objective_value'], 
                               label=algorithm, alpha=0.7)
        
        axes[idx, 1].set_xlabel('Computation Time (ms)')
        axes[idx, 1].set_ylabel('Objective Value')
        axes[idx, 1].set_title(f'{instance_name} - Time vs Quality')
        axes[idx, 1].legend()
    
    plt.tight_layout()
    plt.show()


# ==============================================================================
# STATISTICAL ANALYSIS FUNCTIONS
# ==============================================================================

def perform_statistical_tests(data):
    """
    Perform statistical significance tests between algorithms.
    
    Args:
        data (dict): Dictionary containing algorithm data
    """
    for instance_name, instance_data in data.items():
        print(f"\n{'='*60}")
        print(f"STATISTICAL TESTS - {instance_name}")
        print(f"{'='*60}")
        
        df = instance_data['df']
        df_analysis = df.copy()
        df_analysis['base_algorithm'] = df_analysis['algorithm'].str.replace(r'_start\d+', '', regex=True)
        algorithms = df_analysis['base_algorithm'].unique()
        
        # Pairwise t-tests for objective values
        print("\nPairwise t-tests for objective values:")
        print("-" * 40)
        
        for i, algo1 in enumerate(algorithms):
            for j, algo2 in enumerate(algorithms):
                if i < j:  # Avoid duplicate comparisons
                    data1 = df_analysis[df_analysis['base_algorithm'] == algo1]['objective_value']
                    data2 = df_analysis[df_analysis['base_algorithm'] == algo2]['objective_value']
                    
                    # Perform t-test
                    t_stat, p_value = stats.ttest_ind(data1, data2)
                    
                    significance = "***" if p_value < 0.001 else "**" if p_value < 0.01 else "*" if p_value < 0.05 else "ns"
                    
                    print(f"{algo1} vs {algo2}: t={t_stat:.3f}, p={p_value:.6f} {significance}")
        
        # ANOVA test
        algorithm_groups = [df_analysis[df_analysis['base_algorithm'] == algo]['objective_value'] for algo in algorithms]
        f_stat, p_value = stats.f_oneway(*algorithm_groups)
        
        print(f"\nANOVA Test:")
        print(f"F-statistic: {f_stat:.3f}")
        print(f"p-value: {p_value:.6f}")
        print(f"Significant difference between algorithms: {'Yes' if p_value < 0.05 else 'No'}")


def create_performance_ranking(data):
    """
    Create performance ranking table for all algorithms.
    
    Args:
        data (dict): Dictionary containing algorithm data
        
    Returns:
        pd.DataFrame: Performance ranking dataframe
    """
    ranking_data = []
    
    for instance_name, instance_data in data.items():
        df = instance_data['df']
        df_analysis = df.copy()
        df_analysis['base_algorithm'] = df_analysis['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        for algorithm in df_analysis['base_algorithm'].unique():
            algo_data = df_analysis[df_analysis['base_algorithm'] == algorithm]
            
            ranking_data.append({
                'Instance': instance_name,
                'Algorithm': algorithm,
                'Mean_Objective': algo_data['objective_value'].mean(),
                'Std_Objective': algo_data['objective_value'].std(),
                'Best_Objective': algo_data['objective_value'].min(),
                'Worst_Objective': algo_data['objective_value'].max(),
                'Mean_Time_ms': algo_data['computation_time_ms'].mean(),
                'Success_Rate': 1.0,  # All algorithms find valid solutions
                'Runs': len(algo_data)
            })
    
    ranking_df = pd.DataFrame(ranking_data)
    
    # Display ranking for each instance
    for instance in ranking_df['Instance'].unique():
        print(f"\n{'='*80}")
        print(f"PERFORMANCE RANKING - {instance}")
        print(f"{'='*80}")
        
        instance_ranking = ranking_df[ranking_df['Instance'] == instance].copy()
        instance_ranking = instance_ranking.sort_values('Mean_Objective')
        instance_ranking['Rank'] = range(1, len(instance_ranking) + 1)
        
        # Format for display
        display_cols = ['Rank', 'Algorithm', 'Mean_Objective', 'Best_Objective', 'Mean_Time_ms']
        display_df = instance_ranking[display_cols].round(2)
        print(display_df.to_string(index=False))
        
        # Calculate improvement percentages
        best_mean = instance_ranking['Mean_Objective'].iloc[0]
        worst_mean = instance_ranking['Mean_Objective'].iloc[-1]
        improvement = ((worst_mean - best_mean) / worst_mean) * 100
        
        print(f"\nBest vs Worst Algorithm:")
        print(f"Best: {instance_ranking['Algorithm'].iloc[0]} (avg: {best_mean:.2f})")
        print(f"Worst: {instance_ranking['Algorithm'].iloc[-1]} (avg: {worst_mean:.2f})")
        print(f"Improvement: {improvement:.1f}%")
    
    return ranking_df


def perform_statistical_analysis(data):
    """
    Perform comprehensive statistical analysis of algorithm performance.
    
    Args:
        data (dict): Dictionary containing algorithm data
    """
    for instance_name, instance_data in data.items():
        print(f"\n{'='*60}")
        print(f"STATISTICAL ANALYSIS - {instance_name}")
        print(f"{'='*60}")
        
        df = instance_data['df']
        df_analysis = df.copy()
        df_analysis['base_algorithm'] = df_analysis['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        # Group by algorithm
        algorithm_groups = df_analysis.groupby('base_algorithm')['objective_value']
        
        print("\nDescriptive Statistics:")
        print("-" * 50)
        
        stats_df = df_analysis.groupby('base_algorithm')['objective_value'].agg([
            'count', 'mean', 'std', 'min', 'max', 
            lambda x: x.quantile(0.25),
            lambda x: x.quantile(0.75)
        ]).round(2)
        
        stats_df.columns = ['Count', 'Mean', 'Std', 'Min', 'Max', 'Q1', 'Q3']
        print(stats_df)
        
        # Coefficient of variation
        print("\nCoefficient of Variation (CV %):")
        print("-" * 30)
        cv = (df_analysis.groupby('base_algorithm')['objective_value'].std() / 
              df_analysis.groupby('base_algorithm')['objective_value'].mean() * 100).round(2)
        for algo, cv_val in cv.items():
            print(f"{algo}: {cv_val}%")
        
        # Performance improvement analysis
        print("\nPerformance Improvement Analysis:")
        print("-" * 40)
        algo_means = df_analysis.groupby('base_algorithm')['objective_value'].mean().sort_values()
        best_mean = algo_means.iloc[0]
        
        print(f"Best algorithm: {algo_means.index[0]} (avg: {best_mean:.2f})")
        print("Improvement over other algorithms:")
        for algo, mean_val in algo_means.iloc[1:].items():
            improvement = ((mean_val - best_mean) / mean_val) * 100
            print(f"  vs {algo}: {improvement:.1f}% improvement")


def display_validation_results(data):
    """
    Display detailed validation results for best solutions.
    
    Args:
        data (dict): Dictionary containing algorithm data with visualization information
    """
    for instance_name, instance_data in data.items():
        if 'viz_data' not in instance_data or instance_data['viz_data'] is None:
            print(f"No validation data available for {instance_name}")
            continue
            
        print(f"\n{'='*60}")
        print(f"SOLUTION VALIDATION - {instance_name}")
        print(f"{'='*60}")
        
        viz_data = instance_data['viz_data']
        best_solutions = viz_data['best_solutions']
        
        for algorithm, solution_data in best_solutions.items():
            print(f"\n{algorithm}:")
            print(f"  Objective Value: {solution_data['objective_value']:.2f}")
            print(f"  Path Length: {solution_data['path_length']:.2f}")
            print(f"  Node Costs: {solution_data['node_costs']:.2f}")
            print(f"  Selected Nodes: {len(solution_data['selected_nodes'])}")
            print(f"  Validation Status: {'PASSED' if solution_data['is_validated'] else 'FAILED'}")
            
            if solution_data['validation_report']:
                print(f"  Validation Details:")
                for line in solution_data['validation_report'].strip().split('\n'):
                    if line.strip():
                        print(f"    {line}")


def display_algorithm_statistics(data):
    """
    Display min, max, and average values for each algorithm and instance.
    
    Args:
        data (dict): Dictionary containing algorithm data with summary information
    """
    for instance_name, instance_data in data.items():
        print(f"\n{'='*60}")
        print(f"INSTANCE: {instance_name}")
        print(f"{'='*60}")
        
        df = instance_data['df']
        summary = instance_data['summary_data']
        
        # Extract algorithm statistics from summary
        algorithm_stats = summary['algorithm_statistics']
        
        print(f"\nAlgorithm Performance:")
        print("-" * 80)
        print(f"{'Algorithm':<20} {'Runs':<8} {'Min':<10} {'Max':<10} {'Average':<10} {'Validated':<10}")
        print("-" * 80)
        
        for algorithm, stats in algorithm_stats.items():
            validated = "YES" if stats.get('best_solution_validated', False) else "NO"
            print(f"{algorithm:<20} {stats['total_runs']:<8} "
                  f"{stats['min_objective']:<10.2f} "
                  f"{stats['max_objective']:<10.2f} "
                  f"{stats['avg_objective']:<10.2f} "
                  f"{validated:<10}")


# ==============================================================================
# VISUALIZATION EXPORTER CLASS
# ==============================================================================

class VisualizationExporter:
    """
    Comprehensive visualization exporter for TSP results.
    
    This class handles exporting of best solutions data, statistical summaries,
    and visualization plots for any algorithm type results.
    """

    def __init__(self, algorithm_folder="greedy", results_base_path=RESULTS_PATH):
        """
        Initialize the visualization exporter.
        
        Args:
            algorithm_folder (str): Name of the algorithm folder to export from
            results_base_path (Path): Base path to the results directory
        """
        self.algorithm_folder = algorithm_folder
        self.results_base_path = results_base_path
        self.output_dir = self.results_base_path / algorithm_folder
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def export_best_solutions_data(self, data, filename_suffix=""):
        """
        Export best solutions data to JSON.
        
        Args:
            data (dict): Dictionary containing algorithm data
            filename_suffix (str): Suffix to add to filename
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        for instance_name, instance_data in data.items():
            if 'viz_data' not in instance_data or instance_data['viz_data'] is None:
                print(f"No visualization data available for {instance_name}")
                continue
                
            viz_data = instance_data['viz_data']
            
            # Create export data structure
            export_data = {
                "instance": instance_name,
                "algorithm_folder": self.algorithm_folder,
                "export_timestamp": timestamp,
                "nodes": viz_data['nodes'],
                "best_solutions": viz_data['best_solutions'],
                "metadata": {
                    "total_nodes": len(viz_data['nodes']),
                    "required_nodes": len(list(viz_data['best_solutions'].values())[0]['selected_nodes']),
                    "algorithms_count": len(viz_data['best_solutions'])
                }
            }
            
            # Export to file
            export_filename = f"{instance_name}_best_solutions{filename_suffix}_{timestamp}.json"
            export_path = self.output_dir / export_filename
            
            with open(export_path, 'w') as f:
                json.dump(export_data, f, indent=2)
            
            print(f"Exported best solutions for {instance_name} to: {export_path}")
    
    def export_statistics_summary(self, data, filename_suffix=""):
        """
        Export statistical summary to JSON and CSV.
        
        Args:
            data (dict): Dictionary containing algorithm data
            filename_suffix (str): Suffix to add to filename
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        for instance_name, instance_data in data.items():
            df = instance_data['df']
            df['base_algorithm'] = df['algorithm'].str.replace(r'_start\d+', '', regex=True)
            
            # Create statistical summary
            stats_summary = {}
            
            for algorithm in df['base_algorithm'].unique():
                algo_data = df[df['base_algorithm'] == algorithm]['objective_value']
                stats_summary[algorithm] = {
                    "count": int(algo_data.count()),
                    "mean": float(algo_data.mean()),
                    "std": float(algo_data.std()),
                    "min": float(algo_data.min()),
                    "max": float(algo_data.max()),
                    "q1": float(algo_data.quantile(0.25)),
                    "q3": float(algo_data.quantile(0.75)),
                    "cv_percent": float(algo_data.std() / algo_data.mean() * 100)
                }
            
            # Export JSON summary
            json_filename = f"{instance_name}_statistics{filename_suffix}_{timestamp}.json"
            json_path = self.output_dir / json_filename
            
            with open(json_path, 'w') as f:
                json.dump({
                    "instance": instance_name,
                    "algorithm_folder": self.algorithm_folder,
                    "export_timestamp": timestamp,
                    "statistics": stats_summary
                }, f, indent=2)
            
            # Export CSV data
            csv_filename = f"{instance_name}_raw_data{filename_suffix}_{timestamp}.csv"
            csv_path = self.output_dir / csv_filename
            df.to_csv(csv_path, index=False)
            
            print(f"Exported statistics for {instance_name} to:")
            print(f"  JSON: {json_path}")
            print(f"  CSV: {csv_path}")
    
    def export_visualization_plots(self, data, filename_suffix="", save_format='png', dpi=300):
        """
        Export matplotlib plots to image files.
        
        Args:
            data (dict): Dictionary containing algorithm data
            filename_suffix (str): Suffix to add to filename
            save_format (str): Format for saving plots ('png', 'pdf', 'svg')
            dpi (int): DPI for saved plots
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        for instance_name, instance_data in data.items():
            # Create plots directory
            plots_dir = self.output_dir / "plots"
            plots_dir.mkdir(exist_ok=True)
            
            # Export best solutions plot
            if 'viz_data' in instance_data and instance_data['viz_data'] is not None:
                self._create_and_save_best_solutions_plot(
                    instance_name, instance_data, plots_dir, 
                    filename_suffix, timestamp, save_format, dpi
                )
            
            # Export performance comparison plot
            self._create_and_save_performance_plot(
                instance_name, instance_data, plots_dir, 
                filename_suffix, timestamp, save_format, dpi
            )
    
    def _create_and_save_best_solutions_plot(self, instance_name, instance_data, 
                                           plots_dir, filename_suffix, timestamp, save_format, dpi):
        """Create and save best solutions visualization plot."""
        viz_data = instance_data['viz_data']
        best_solutions = viz_data['best_solutions']
        nodes = viz_data['nodes']
        
        # Create the plot (using existing plotting logic) - one per row
        n_algorithms = len(best_solutions)
        cols = 1  # One plot per row
        rows = n_algorithms
        
        fig, axes = plt.subplots(rows, cols, figsize=(12, 8 * rows))
        if rows == 1:
            axes = [axes]  # Ensure axes is always a list
        
        fig.suptitle(f'Best Solutions - {instance_name}', fontsize=16, fontweight='bold')
        
        # Prepare node data
        node_coords = {node['id']: (node['x'], node['y']) for node in nodes}
        node_costs = {node['id']: node['cost'] for node in nodes}
        max_cost = max(node['cost'] for node in nodes)
        min_cost = min(node['cost'] for node in nodes)
        
        for idx, (algorithm, solution_data) in enumerate(best_solutions.items()):
            ax = axes[idx]
            
            # Plot all nodes (unselected) in light gray
            for node in nodes:
                x, y = node['x'], node['y']
                ax.scatter(x, y, c='lightgray', s=20, alpha=0.5, zorder=1)
                ax.text(x, y-50, str(node['id']), ha='center', va='top', 
                       fontsize=6, alpha=0.7)
            
            # Plot selected nodes and route
            selected_nodes = solution_data['selected_nodes']
            route = solution_data['route']
            
            for node_id in selected_nodes:
                x, y = node_coords[node_id]
                cost = node_costs[node_id]
                size = 50 + 200 * (cost - min_cost) / (max_cost - min_cost)
                ax.scatter(x, y, c=cost, s=size, cmap='viridis', 
                          vmin=min_cost, vmax=max_cost, 
                          edgecolors='black', linewidth=1, zorder=3)
            
            # Plot route
            route_coords = [node_coords[node_id] for node_id in route]
            route_coords.append(route_coords[0])
            route_x = [coord[0] for coord in route_coords]
            route_y = [coord[1] for coord in route_coords]
            ax.plot(route_x, route_y, 'r-', linewidth=2, alpha=0.8, zorder=2)
            
            # Add direction arrows
            for i in range(len(route)):
                x1, y1 = route_coords[i]
                x2, y2 = route_coords[i + 1]
                dx, dy = x2 - x1, y2 - y1
                ax.annotate('', xy=(x1 + 0.7*dx, y1 + 0.7*dy), xytext=(x1 + 0.3*dx, y1 + 0.3*dy),
                           arrowprops=dict(arrowstyle='->', color='red', lw=1.5), zorder=4)
            
            # Formatting
            ax.set_title(f'{algorithm}\nObjective: {solution_data["objective_value"]:.2f}\n'
                        f'Path: {solution_data["path_length"]:.2f}, Costs: {solution_data["node_costs"]:.2f}\n'
                        f'Validated: {"YES" if solution_data["is_validated"] else "NO"}')
            ax.set_xlabel('X Coordinate')
            ax.set_ylabel('Y Coordinate')
            ax.grid(True, alpha=0.3)
            ax.set_aspect('equal')
        
        # Add colorbar
        sm = plt.cm.ScalarMappable(cmap='viridis', norm=plt.Normalize(vmin=min_cost, vmax=max_cost))
        sm.set_array([])
        cbar = fig.colorbar(sm, ax=axes, orientation='horizontal', pad=0.1, shrink=0.8)
        cbar.set_label('Node Cost', fontsize=12)
        
        # Save plot
        plot_filename = f"{instance_name}_best_solutions{filename_suffix}_{timestamp}.{save_format}"
        plot_path = plots_dir / plot_filename
        plt.savefig(plot_path, dpi=dpi, bbox_inches='tight')
        plt.close()
        
        print(f"Saved best solutions plot: {plot_path}")
    
    def _create_and_save_performance_plot(self, instance_name, instance_data, 
                                        plots_dir, filename_suffix, timestamp, save_format, dpi):
        """Create and save performance comparison plot."""
        df = instance_data['df']
        df['base_algorithm'] = df['algorithm'].str.replace(r'_start\d+', '', regex=True)
        
        fig, axes = plt.subplots(1, 2, figsize=(15, 6))
        
        # Box plot of objective values
        sns.boxplot(data=df, x='base_algorithm', y='objective_value', ax=axes[0])
        axes[0].set_title(f'{instance_name} - Objective Value Distribution')
        axes[0].tick_params(axis='x', rotation=45)
        axes[0].set_xlabel('Algorithm')
        
        # Computation time comparison
        sns.boxplot(data=df, x='base_algorithm', y='computation_time_ms', ax=axes[1])
        axes[1].set_title(f'{instance_name} - Computation Time')
        axes[1].tick_params(axis='x', rotation=45)
        axes[1].set_xlabel('Algorithm')
        axes[1].set_ylabel('Time (ms)')
        
        plt.tight_layout()
        
        # Save plot
        plot_filename = f"{instance_name}_performance{filename_suffix}_{timestamp}.{save_format}"
        plot_path = plots_dir / plot_filename
        plt.savefig(plot_path, dpi=dpi, bbox_inches='tight')
        plt.close()
        
        print(f"Saved performance plot: {plot_path}")


# ==============================================================================
# CONVENIENCE FUNCTIONS
# ==============================================================================

def setup_plotting_style():
    """Set up the default plotting style for consistent visualizations."""
    plt.style.use('seaborn-v0_8')
    sns.set_palette("husl")
    plt.rcParams['figure.figsize'] = (12, 8)


def get_available_algorithm_folders(results_path: Path=RESULTS_PATH):
    """
    Get list of available algorithm folders in the results directory.
    
    Args:
        results_path (Path): Base path to the results directory
        
    Returns:
        list: List of algorithm folder names
    """
    if not results_path.exists():
        return []
    
    algorithm_folders = [d.name for d in results_path.iterdir() if d.is_dir()]
    return sorted(algorithm_folders)


def get_available_instances(algorithm_folder="greedy", results_path=RESULTS_PATH):
    """
    Get list of available instances for a given algorithm folder.
    
    Args:
        algorithm_folder (str): Name of the algorithm folder
        results_path (Path): Base path to the results directory
        
    Returns:
        list: List of instance names found in the folder
    """
    folder_path = results_path / algorithm_folder
    if not folder_path.exists():
        return []
    
    instances = set()
    for file in folder_path.glob("*_results.json"):
        # Extract instance name from filename
        filename = file.stem
        if filename.endswith("_results"):
            instance_name = filename.replace(f"_{algorithm_folder}_results", "")
            instances.add(instance_name)
    
    return sorted(list(instances))