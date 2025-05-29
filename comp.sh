#!/bin/bash

# Usage: ./compare_dirs.sh dir1 dir2

# Check for correct number of arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 dir1 dir2"
    exit 1
fi

DIR1="$1"
DIR2="$2"

# Ensure both directories exist
if [ ! -d "$DIR1" ] || [ ! -d "$DIR2" ]; then
    echo "Both arguments must be directories."
    exit 1
fi

# Create temporary files to store file lists
TMP1=$(mktemp)
TMP2=$(mktemp)

# Function to clean up temporary files on exit
cleanup() {
    rm -f "$TMP1" "$TMP2"
}
trap cleanup EXIT

# Generate sorted list of files (excluding specified directories and empty directories) for each directory
find "$DIR1" -type f ! -path "*/.git/*" ! -path "*/.protegedata/*" | sed "s|^$DIR1/||" | sort > "$TMP1"
find "$DIR2" -type f ! -path "*/.git/*" ! -path "*/.protegedata/*" | sed "s|^$DIR2/||" | sort > "$TMP2"

# Compare the file lists to find common files
comm -12 "$TMP1" "$TMP2" | while read -r file; do
    # Compare files, ignoring line ending differences
    if ! diff --strip-trailing-cr "$DIR1/$file" "$DIR2/$file" > /dev/null; then
        echo "Files differ: $file"
    fi
done

# Find files only in DIR1
comm -23 "$TMP1" "$TMP2" | while read -r file; do
    echo "Only in $DIR1: $file"
done

# Find files only in DIR2
comm -13 "$TMP1" "$TMP2" | while read -r file; do
    echo "Only in $DIR2: $file"
done
