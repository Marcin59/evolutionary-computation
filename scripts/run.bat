@echo off
echo Building TSP Solver Docker image...

:: Save the original directory
set "ORIGINAL_DIR=%cd%"

:: Change to experiments directory
cd /d "%~dp0..\experiments"

:: Check if we're in the right directory
if not exist "Dockerfile" (
    echo ERROR: Dockerfile not found in experiments directory!
    echo Current directory: %cd%
    pause
    exit /b 1
)

docker build -t tsp-solver .

if %ERRORLEVEL% NEQ 0 (
    echo Failed to build Docker image!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Running TSP experiments...
docker run --rm -v "%ORIGINAL_DIR%\results:/app/results" -v "%cd%\data:/app/data" tsp-solver

if %ERRORLEVEL% NEQ 0 (
    echo Failed to run TSP experiments!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ====================================
echo TSP Solver completed successfully!
echo ====================================
echo Results saved to: %ORIGINAL_DIR%\results\
echo.
echo To analyze results:
echo 1. cd %ORIGINAL_DIR%\analysis
echo 2. pip install -r requirements.txt
echo 3. jupyter lab TSP_Greedy_Analysis.ipynb
echo.

:: Return to original directory
cd /d "%ORIGINAL_DIR%"
pause