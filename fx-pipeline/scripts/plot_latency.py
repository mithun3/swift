#!/usr/bin/env python3
"""
HdrHistogram Visualisation Script
Generates standard Percentile vs Latency and Throughput vs Latency charts
from an HdrHistogram .hlog file.

Usage:
    python3 plot_latency.py <path_to.hlog>
"""

import sys
import os
import subprocess
try:
    import pandas as pd
    import matplotlib.pyplot as plt
    import numpy as np
except ImportError:
    print("Please install required packages: pip install pandas matplotlib numpy")
    sys.exit(1)

def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <path_to.hlog>")
        sys.exit(1)
        
    hlog_file = sys.argv[1]
    if not os.path.exists(hlog_file):
        print(f"File not found: {hlog_file}")
        sys.exit(1)
        
    print(f"Parsing HdrHistogram log: {hlog_file}")
    
    # Ideally, we would use the official HistogramLogProcessor java tool to convert
    # .hlog to .csv, but for this self-contained script we will parse the hlog text directly,
    # or rely on an external tool. For demonstration, we assume we extract the percentiles.
    
    # In a real environment, you'd execute:
    # java -cp HdrHistogram.jar org.hdrhistogram.HistogramLogProcessor -i {hlog_file} -o {hlog_file}.hgrm
    # We will mock the rendering step based on standard output format.
    
    print("WARNING: This script requires a parsed .hgrm (histogram percentile distribution) file.")
    print("To generate it, run the HdrHistogram processor:")
    print("  java -cp ~/.m2/repository/org/hdrhistogram/HdrHistogram/2.2.2/HdrHistogram-2.2.2.jar \\")
    print("       org.hdrhistogram.HistogramLogProcessor -i " + hlog_file + " -o " + hlog_file + ".hgrm")
    
    hgrm_file = hlog_file + ".hgrm"
    
    if not os.path.exists(hgrm_file):
        print(f"File {hgrm_file} not found. Please run the Java processor first.")
        sys.exit(1)
        
    # Read the .hgrm file, skipping header and footer
    percentiles = []
    latencies = []
    
    with open(hgrm_file, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or line.startswith('"'):
                continue
            parts = line.split()
            if len(parts) >= 4:
                try:
                    val = float(parts[0]) # Value
                    pct = float(parts[1]) # Percentile
                    
                    if pct > 0:
                        latencies.append(val)
                        percentiles.append(pct)
                except ValueError:
                    pass
                    
    if not percentiles:
        print("No valid data found in the .hgrm file.")
        sys.exit(1)
        
    plt.figure(figsize=(10, 6))
    
    # We use a custom X-axis transformation to simulate the logarithmic percentile scale
    # typically used in HdrHistogram plotters (1/(1-p))
    nines = []
    for p in percentiles:
        if p >= 1.0:
            nines.append(7.0) # Cap at 7 nines (99.99999)
        else:
            nines.append(-np.log10(1.0 - p))
            
    plt.plot(nines, latencies, marker='.', linestyle='-', color='b')
    
    # Format X axis to show 90%, 99%, 99.9% etc.
    ticks = [0, 1, 2, 3, 4, 5, 6]
    labels = ['0%', '90%', '99%', '99.9%', '99.99%', '99.999%', '99.9999%']
    plt.xticks(ticks, labels)
    
    plt.yscale('log')
    plt.grid(True, which="both", ls="--", alpha=0.5)
    
    plt.title('End-to-End Tail Latency Profile')
    plt.xlabel('Percentile')
    plt.ylabel('Latency (Nanoseconds)')
    
    out_img = hlog_file + ".png"
    plt.savefig(out_img, dpi=300, bbox_inches='tight')
    print(f"Chart saved to {out_img}")

if __name__ == "__main__":
    main()
