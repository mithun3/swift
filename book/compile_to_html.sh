#!/usr/bin/env bash

# Compile the compiled markdown into a standalone, print-friendly HTML file
# using Pandoc and the provided print_style.css.

cd "$(dirname "$0")"

# Add MacTeX/BasicTeX to PATH in case it was just installed and terminal hasn't been restarted
export PATH="/Library/TeX/texbin:$PATH"

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

echo "Compiling full_book_compiled.md to PDF..."

pandoc full_book_compiled.md \
    -o full_book_compiled.pdf \
    --metadata title="Principles of Software Engineering, Concurrency & Architecture" \
    --toc \
    --toc-depth=3 \
    -f markdown-yaml_metadata_block \
    --pdf-engine=xelatex \
    -V monofont="Menlo"

if [ $? -eq 0 ]; then
    echo "Successfully generated full_book_compiled.pdf"
else
    echo "Error generating PDF. (You may need to install a PDF engine like MacTeX: brew install --cask mactex-no-gui or basictex)"
    exit 1
fi
