#!/bin/bash
# This script checks if FFmpeg is installed and available in the environment.

echo "Checking for FFmpeg..."

if ! command -v ffmpeg &> /dev/null
then
    echo "Error: FFmpeg could not be found."
    echo "Please ensure FFmpeg is installed in the Docker image and is in the system's PATH."
    exit 1
fi

FFMPEG_VERSION=$(ffmpeg -version | head -n 1)
echo "FFmpeg found!"
echo "Version: ${FFMPEG_VERSION}"

exit 0
