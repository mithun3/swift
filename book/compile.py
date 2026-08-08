#!/usr/bin/env python3
"""
Book Compilation Script

This script concatenates all Markdown files in the book directory into a single 
printable file (`full_book_compiled.md`), preserving the intended reading order.
"""

import sys
from pathlib import Path

def main() -> None:
    # Resolve the book directory relative to this script's location
    book_dir = Path(__file__).resolve().parent
    output_file = book_dir / "full_book_compiled.md"
    
    # Collect all markdown files
    md_files = []
    for filepath in book_dir.rglob("*.md"):
        # Exclude the output file itself
        if filepath.name == output_file.name:
            continue
        md_files.append(filepath)
        
    # Sort files by their relative path to ensure correct chapter ordering
    md_files.sort(key=lambda p: str(p.relative_to(book_dir)))
    
    if not md_files:
        print("Error: No markdown files found to compile.", file=sys.stderr)
        sys.exit(1)
        
    print(f"Found {len(md_files)} markdown files. Compiling...")
    
    # Concatenate contents
    compiled_content = []
    for filepath in md_files:
        print(f"  -> Adding {filepath.relative_to(book_dir)}")
        content = filepath.read_text(encoding="utf-8")
        
        # Ensure it ends with a newline to prevent markdown formatting issues
        if not content.endswith("\n"):
            content += "\n"
            
        compiled_content.append(content)
        
    # Write the compiled result
    # We join with an extra newline to ensure proper spacing between sections
    output_file.write_text("\n".join(compiled_content), encoding="utf-8")
    
    print(f"\nCompilation successful! Output saved to: {output_file.relative_to(book_dir)}")

if __name__ == "__main__":
    main()
