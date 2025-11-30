#!/bin/bash

echo "Building and running TSP Solver with Docker..."

# Build the Docker image from the 'experiments' directory context
echo "Building Docker image..."
docker build -t tsp-solver experiments

# Run the TSP solver
echo "Running TSP experiments..."
docker run --rm -v "$(pwd)/results:/app/results" tsp-solver

echo "TSP Solver completed! Results are available in the ./results directory."
echo ""
echo "To analyze results:"
echo "1. Install Python dependencies: pip install -r analysis/requirements.txt"
echo "2. Start Jupyter: jupyter lab"
echo "3. Open a notebook from the 'analysis/Lab7' directory to see the analysis for LNS."