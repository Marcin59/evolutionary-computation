@echo off
echo Building and running TSP Solver with Docker...

REM Build the Docker image
echo Building Docker image...
docker build -t tsp-solver .

REM Run the TSP solver
echo Running TSP experiments...
docker run --rm -v "%cd%\results:/app/results" -v "%cd%\data:/app/data" tsp-solver

echo.
echo TSP Solver completed! Results are available in the ./results directory.
echo.
echo To analyze results:
echo 1. Install Python dependencies: pip install -r requirements.txt
echo 2. Start Jupyter: jupyter lab TSP_Analysis.ipynb

pause Building and running TSP Solver with Docker...

REM Build and run the TSP solver
docker-compose up --build tsp-solver

echo.
echo TSP Solver completed! Results are available in the ./results directory.
echo.
echo To analyze results:
echo 1. Install Python dependencies: pip install pandas numpy matplotlib seaborn scipy jupyter
echo 2. Start Jupyter: jupyter lab
echo 3. Open TSP_Analysis.ipynb

pause