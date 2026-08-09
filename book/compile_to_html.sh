#!/usr/bin/env bash

# Compile the compiled markdown into a standalone, print-friendly HTML file
# using Pandoc and the provided print_style.css.

cd "$(dirname "$0")"

echo "Compiling full_book_compiled.md to HTML..."

pandoc full_book_compiled.md \
    -o full_book_compiled.html \
    --css print_style.css \
    --metadata title="Principles of Software Engineering, Concurrency & Architecture" \
    --embed-resources \
    --standalone \
    --toc \
    --toc-depth=3 \
    -f markdown-yaml_metadata_block \
    --mathjax

if [ $? -eq 0 ]; then
    echo "Successfully generated full_book_compiled.html"
else
    echo "Error generating HTML."
    exit 1
fi
