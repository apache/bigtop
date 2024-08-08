#!/bin/bash


if [ $# -eq 0 ]; then
    echo "Error: Please provide a directory as an argument."
    echo "Usage: $0 <directory>"
    exit 1
fi

DEST_DIR="$1"

if [ ! -d "$DEST_DIR" ]; then
    echo "Error: Directory '$DEST_DIR' does not exist."
    exit 1
fi

SCRIPT_DIR=$DEST_DIR
cd "$SCRIPT_DIR"

# 激活虚拟环境
if [ -f "venv.sh" ]; then
    source venv.sh
else
    echo "Warning: venv.sh not found in the current directory."
fi

python3 ci_tools/main.py -generate-conf
if [ $? -ne 0 ]; then
    echo "Error: Failed to generate configuration."
    exit 1
fi

python3 ci_tools/main.py -deploy
if [ $? -ne 0 ]; then
    echo "Error: Deployment failed."
    exit 1
fi

echo "Deployment completed successfully."
