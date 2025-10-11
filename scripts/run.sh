#!/bin/bash

echo "Building TSP Solver Docker image..."

# Save the original directory
ORIGINAL_DIR="$(pwd)"

# Change to experiments directory
cd "$(dirname "$0")/../experiments"

# Check if we're in the right directory
if [ ! -f "Dockerfile" ]; then
    echo "ERROR: Dockerfile not found in experiments directory!"
    echo "Current directory: $(pwd)"
    exit 1
fi

docker build -t tsp-solver .

if [ $? -ne 0 ]; then
    echo "Failed to build Docker image!"
    exit 1
fi

echo ""
echo "Running TSP experiments..."
docker run --rm -v "$ORIGINAL_DIR/results:/app/results" -v "$(pwd)/data:/app/data" tsp-solver

if [ $? -ne 0 ]; then
    echo "Failed to run TSP experiments!"
    exit 1
fi

echo ""
echo "===================================="
echo "TSP Solver completed successfully!"
echo "===================================="
echo "Results saved to: $ORIGINAL_DIR/results/"
echo ""
echo "To analyze results:"
echo "1. cd $ORIGINAL_DIR/analysis"
echo "2. pip install -r requirements.txt"
echo "3. jupyter lab TSP_Greedy_Analysis.ipynb"
echo ""

# Return to original directory
cd "$ORIGINAL_DIR"