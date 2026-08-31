import os
import json
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional

def parse_hgrm_file(filepath: str) -> Optional[pd.DataFrame]:
    """
    Parses a standard HdrHistogram .hgrm file into a Pandas DataFrame.
    """
    try:
        # Read the file, skip lines starting with '#' or '"'
        lines = []
        with open(filepath, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#') or line.startswith('"'):
                    continue
                parts = line.split()
                if len(parts) >= 4:
                    try:
                        value = float(parts[0])
                        percentile = float(parts[1])
                        count = int(parts[2])
                        inverse = float(parts[3])
                        lines.append((value, percentile, count, inverse))
                    except ValueError:
                        pass
        
        if not lines:
            return None
            
        df = pd.DataFrame(lines, columns=['Value', 'Percentile', 'TotalCount', '1/(1-Percentile)'])
        return df
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return None

def extract_summary_stats(df: pd.DataFrame) -> Dict[str, float]:
    """
    Extracts standard summary percentiles from a parsed .hgrm DataFrame.
    The values are in nanoseconds.
    """
    def get_closest(target: float) -> float:
        # Find the row with Percentile closest to target
        idx = (df['Percentile'] - target).abs().idxmin()
        return df.loc[idx, 'Value']

    return {
        'P50': get_closest(0.50),
        'P90': get_closest(0.90),
        'P99': get_closest(0.99),
        'P99.9': get_closest(0.999),
        'Max': df['Value'].max()
    }

def discover_runs(base_dir: str = None) -> List[Dict]:
    """
    Walks the benchmark-runs directory to find all archived runs.
    Returns a list of dictionaries containing run metadata and paths.
    """
    runs = []
    if base_dir is None:
        # Default to benchmark-runs in the parent directory of this script (project root)
        base_path = Path(__file__).parent.parent / "benchmark-runs"
    else:
        base_path = Path(base_dir)
    
    if not base_path.exists():
        return runs

    # Directory structure: benchmark-runs/<env>/<run_id>/
    for env_dir in base_path.iterdir():
        if not env_dir.is_dir():
            continue
            
        env_name = env_dir.name
        for run_dir in env_dir.iterdir():
            if not run_dir.is_dir():
                continue
                
            run_id = run_dir.name
            
            # Check for required files
            hgrm_file = run_dir / "fx-latency.hlog.hgrm"
            metadata_file = run_dir / "run_manifest.json"
            
            if hgrm_file.exists():
                metadata = {}
                if metadata_file.exists():
                    try:
                        with open(metadata_file, 'r') as f:
                            metadata = json.load(f)
                    except Exception:
                        pass
                
                # Extract from run_manifest
                env = metadata.get("environment_label", env_name)
                timestamp = metadata.get("timestamp_utc", metadata.get("timestamp"))
                commit = metadata.get("git_sha", metadata.get("commit", "unknown"))
                run_id = metadata.get("run_id", run_id)
                
                if not timestamp:
                    if run_id.startswith("run-"):
                        timestamp = run_id[4:]
                    else:
                        timestamp = run_id
                
                runs.append({
                    "env": env,
                    "run_id": run_id,
                    "timestamp": timestamp,
                    "commit": commit,
                    "hgrm_path": str(hgrm_file),
                    "metadata_path": str(metadata_file) if metadata_file.exists() else None
                })
                
    # Sort by timestamp descending
    runs.sort(key=lambda x: x["timestamp"], reverse=True)
    return runs

def load_run_data(run_info: Dict) -> Optional[Dict]:
    """
    Loads the full .hgrm dataframe and summary stats for a run.
    """
    df = parse_hgrm_file(run_info["hgrm_path"])
    if df is None or df.empty:
        return None
        
    stats = extract_summary_stats(df)
    return {
        "info": run_info,
        "dataframe": df,
        "stats": stats
    }
