#!/usr/bin/env -S uv run
"""
TSP Algorithm Report Generator

Automatically generates a markdown report for TSP algorithm results.
Uses the utility functions from utils.py for data loading and visualization.

Usage:
    uv run generate_report.py <algorithm_folder> [--instances TSPA TSPB] [--output-dir Lab3]
    
Example:
    uv run generate_report.py local_search --instances TSPA TSPB --output-dir Lab3
"""

import sys
import os
import argparse
from pathlib import Path
from datetime import datetime
import json

# Add analysis directory to Python path
analysis_dir = Path(__file__).parent
if str(analysis_dir) not in sys.path:
    sys.path.insert(0, str(analysis_dir))

import utils
import pandas as pd
import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for saving figures
import matplotlib.pyplot as plt


class TSPReportGenerator:
    """Generate comprehensive markdown reports for TSP algorithm experiments."""
    
    def __init__(self, algorithm_folder, instances=None, output_dir=None):
        """
        Initialize the report generator.
        
        Args:
            algorithm_folder (str): Name of the algorithm folder in results/
            instances (list): List of instance names to analyze
            output_dir (str): Output directory for report and images
        """
        self.algorithm_folder = algorithm_folder
        self.instances = instances or ['TSPA', 'TSPB']
        
        # Set up output directory
        if output_dir:
            self.output_dir = Path(output_dir)
        else:
            self.output_dir = analysis_dir / f"Lab{self._get_lab_number()}"
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.images_dir = self.output_dir / "images"
        self.images_dir.mkdir(exist_ok=True)
        
        # Load algorithm data
        self.algorithm_data = utils.load_all_algorithm_results(
            self.algorithm_folder, 
            self.instances
        )
        
        if not self.algorithm_data:
            raise ValueError(f"No data found for algorithm folder: {algorithm_folder}")
        
        # Storage for visualizations
        self.visualizations = {}
    
    def _get_lab_number(self):
        """Infer lab number from algorithm folder name."""
        folder_map = {
            'greedy': '1',
            'regret': '2', 
            'local_search': '3',
            'candidate_moves': '4',
        }
        return folder_map.get(self.algorithm_folder, 'X')
    
    def _get_algorithm_display_name(self, algorithm_folder):
        """Convert algorithm folder name to display name."""
        name_map = {
            'greedy': 'Greedy',
            'regret': 'Regret',
            'local_search': 'Local Search',
            'candidate_moves': 'Candidate Moves',
        }
        return name_map.get(algorithm_folder, algorithm_folder.replace('_', ' ').title())
    
    def generate_visualizations(self):
        """Generate visualization images for best solutions."""
        print("Generating visualizations...")
        
        for instance_name, instance_data in self.algorithm_data.items():
            if 'viz_data' not in instance_data or instance_data['viz_data'] is None:
                print(f"  No visualization data available for {instance_name}")
                continue
            
            viz_data = instance_data['viz_data']
            best_solutions = viz_data['best_solutions']
            
            print(f"  Creating visualizations for {instance_name}...")
            
            # Generate one plot per algorithm
            for algorithm, solution_data in best_solutions.items():
                self._create_solution_plot(instance_name, algorithm, solution_data, viz_data)
        
        print(f"Visualizations saved to {self.images_dir}")
    
    def _create_solution_plot(self, instance_name, algorithm, solution_data, viz_data):
        """Create a single solution visualization plot."""
        nodes = viz_data['nodes']
        
        # Prepare node data
        node_coords = {node['id']: (node['x'], node['y']) for node in nodes}
        node_costs = {node['id']: node['cost'] for node in nodes}
        max_cost = max(node['cost'] for node in nodes)
        min_cost = min(node['cost'] for node in nodes)
        
        # Create figure
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
        
        # Formatting
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
        sm = plt.cm.ScalarMappable(cmap='viridis', 
                                   norm=plt.Normalize(vmin=min_cost, vmax=max_cost))
        sm.set_array([])
        cbar = fig.colorbar(sm, ax=ax, orientation='horizontal', pad=0.1, shrink=0.8)
        cbar.set_label('Node Cost', fontsize=12)
        
        plt.tight_layout()
        
        # Save figure
        safe_algorithm_name = algorithm.replace('/', '_').replace(' ', '_')
        filename = f"{instance_name}_{safe_algorithm_name}.png"
        filepath = self.images_dir / filename
        plt.savefig(filepath, dpi=300, bbox_inches='tight')
        plt.close(fig)
        
        # Store the reference for the report
        if instance_name not in self.visualizations:
            self.visualizations[instance_name] = []
        
        self.visualizations[instance_name].append({
            'algorithm': algorithm,
            'filename': filename,
            'solution_data': solution_data
        })
    
    def generate_markdown_report(self, authors=None, title=None):
        """
        Generate the complete markdown report.
        
        Args:
            authors (list): List of author names with student IDs
            title (str): Custom title for the report
        
        Returns:
            str: Complete markdown report content
        """
        algorithm_display = self._get_algorithm_display_name(self.algorithm_folder)
        
        if title is None:
            title = f"{algorithm_display} algorithm for TSP Problem"
        
        report = []
        
        # Header
        report.append(f"# {title}\n")
        
        # Authors
        if authors:
            report.append("## Authors")
            for author in authors:
                report.append(f"- {author}")
            report.append("")
        
        # Pseudocode section (blank for manual filling)
        report.append("## Implemented Algorithms\n")
        report.append("### Pseudocode\n")
        report.append("```")
        report.append("# TODO: Add algorithm pseudocode here")
        report.append("```\n")
        report.append("---\n")
        
        # Experiment Results
        report.append("## Experiment Results\n")
        
        # Objective function table
        report.append("### Objective function\n")
        report.append(self._generate_objective_table())
        report.append("\n---\n")
        
        # Computation time table
        report.append("### Computation Times (ms)\n")
        report.append(self._generate_time_table())
        report.append("")
        
        # 2D Visualizations
        report.append("## 2D Visualization of Best Solution\n")
        
        for instance_name in self.instances:
            if instance_name not in self.visualizations:
                continue
                
            report.append(f"### Instance: {instance_name}\n")
            
            for viz in self.visualizations[instance_name]:
                algorithm = viz['algorithm']
                filename = viz['filename']
                solution_data = viz['solution_data']
                
                report.append(f"#### {algorithm}\n")
                report.append(f"![{algorithm}](images/{filename})\n")
                
                # Node order
                report.append("**Node Order (Route):**")
                route = solution_data['route']
                report.append(f"{', '.join(map(str, route))}\n")
        
        # Conclusions section (blank for manual filling)
        report.append("---\n")
        report.append("## Conclusions\n")
        report.append("### Key Findings\n")
        report.append("<!-- TODO: Add analysis of results -->\n")
        report.append("\n")
        report.append("### Performance Comparison\n")
        report.append("<!-- TODO: Compare algorithms -->\n")
        report.append("\n")
        report.append("### Observations\n")
        report.append("<!-- TODO: Add observations -->\n")
        
        return "\n".join(report)
    
    def _generate_objective_table(self):
        """Generate markdown table for objective function results."""
        # Collect statistics for all algorithms across instances
        algorithm_stats = {}
        
        for instance_name, instance_data in self.algorithm_data.items():
            df = instance_data['df'].copy()
            
            # Clean algorithm names
            df['algorithm'] = df['algorithm'].str.replace(r'_start\d+$', '', regex=True)
            
            # Group by algorithm
            for algorithm in df['algorithm'].unique():
                if algorithm not in algorithm_stats:
                    algorithm_stats[algorithm] = {}
                
                algo_data = df[df['algorithm'] == algorithm]['objective_value']
                mean = algo_data.mean()
                min_val = algo_data.min()
                max_val = algo_data.max()
                
                algorithm_stats[algorithm][instance_name] = f"{mean:.2f} ({min_val:.2f} - {max_val:.2f})"
        
        # Build table
        lines = []
        lines.append("| Algorithm | " + " | ".join(self.instances) + " |")
        lines.append("|" + "|".join(["---"] * (len(self.instances) + 1)) + "|")
        
        for algorithm in sorted(algorithm_stats.keys()):
            row = [algorithm]
            for instance in self.instances:
                row.append(algorithm_stats[algorithm].get(instance, "N/A"))
            lines.append("| " + " | ".join(row) + " |")
        
        return "\n".join(lines)
    
    def _generate_time_table(self):
        """Generate markdown table for computation times."""
        # Collect statistics for all algorithms across instances
        algorithm_stats = {}
        
        for instance_name, instance_data in self.algorithm_data.items():
            df = instance_data['df'].copy()
            
            # Clean algorithm names
            df['algorithm'] = df['algorithm'].str.replace(r'_start\d+$', '', regex=True)
            
            # Group by algorithm
            for algorithm in df['algorithm'].unique():
                if algorithm not in algorithm_stats:
                    algorithm_stats[algorithm] = {}
                
                algo_data = df[df['algorithm'] == algorithm]['computation_time_ms']
                mean = algo_data.mean()
                min_val = algo_data.min()
                max_val = algo_data.max()
                
                algorithm_stats[algorithm][instance_name] = f"{mean:.2f} ({min_val:.0f} - {max_val:.0f})"
        
        # Build table
        lines = []
        lines.append("| Algorithm | " + " | ".join(self.instances) + " |")
        lines.append("|" + "|".join(["---"] * (len(self.instances) + 1)) + "|")
        
        for algorithm in sorted(algorithm_stats.keys()):
            row = [algorithm]
            for instance in self.instances:
                row.append(algorithm_stats[algorithm].get(instance, "N/A"))
            lines.append("| " + " | ".join(row) + " |")
        
        return "\n".join(lines)
    
    def save_report(self, report_content, filename=None):
        """Save the markdown report to a file."""
        if filename is None:
            filename = f"TSP_{self.algorithm_folder}_report.md"
        
        filepath = self.output_dir / filename
        
        with open(filepath, 'w') as f:
            f.write(report_content)
        
        print(f"\nReport saved to: {filepath}")
        return filepath
    
    def generate(self, authors=None, title=None, filename=None):
        """
        Complete workflow: generate visualizations and report.
        
        Args:
            authors (list): List of author names
            title (str): Custom title
            filename (str): Output filename
        
        Returns:
            Path: Path to the generated report
        """
        print(f"Generating report for {self.algorithm_folder}...")
        print(f"Output directory: {self.output_dir}")
        
        # Generate visualizations
        self.generate_visualizations()
        
        # Generate report
        report_content = self.generate_markdown_report(authors=authors, title=title)
        
        # Save report
        report_path = self.save_report(report_content, filename=filename)
        
        print(f"\n✓ Report generation complete!")
        print(f"  Report: {report_path}")
        print(f"  Images: {self.images_dir}")
        
        return report_path


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description='Generate TSP algorithm analysis report',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate report for candidate_moves algorithm
  python generate_report.py candidate_moves
  
  # Generate with custom authors
  python generate_report.py local_search --authors "John Doe 123456" "Jane Smith 654321"
  
  # Generate with custom output directory
  python generate_report.py greedy --output MyLab1 --title "Custom Greedy Analysis"
        """
    )
    
    parser.add_argument(
        'algorithm_folder',
        help='Algorithm folder name (e.g., greedy, local_search, candidate_moves)'
    )
    
    parser.add_argument(
        '--authors',
        nargs='+',
        help='List of authors with student IDs (e.g., "John Doe 123456")',
        default=None
    )
    
    parser.add_argument(
        '--instances',
        nargs='+',
        help='TSP instances to analyze (default: TSPA TSPB)',
        default=['TSPA', 'TSPB']
    )
    
    parser.add_argument(
        '--output',
        help='Output directory name (default: auto-generated based on algorithm)',
        default=None
    )
    
    parser.add_argument(
        '--title',
        help='Custom report title',
        default=None
    )
    
    parser.add_argument(
        '--filename',
        help='Output filename (default: TSP_{algorithm}_report.md)',
        default=None
    )
    
    args = parser.parse_args()
    
    try:
        # Create generator
        generator = TSPReportGenerator(
            algorithm_folder=args.algorithm_folder,
            instances=args.instances,
            output_dir=args.output
        )
        
        # Generate report
        generator.generate(
            authors=args.authors,
            title=args.title,
            filename=args.filename
        )
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    
    return 0


if __name__ == '__main__':
    sys.exit(main())
