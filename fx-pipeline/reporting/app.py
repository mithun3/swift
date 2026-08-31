import streamlit as st
import pandas as pd
import plotly.graph_objects as go
from typing import List, Dict

# Import from our parser module
from parser import discover_runs, load_run_data

st.set_page_config(
    page_title="FX-Pipeline Benchmark History",
    page_icon="📈",
    layout="wide"
)

st.title("FX-Pipeline Benchmark History")
st.markdown("Compare historical performance runs across different environments.")

@st.cache_data
def get_all_runs() -> List[Dict]:
    return discover_runs()

@st.cache_data
def get_run_data(run_info: Dict) -> Dict:
    return load_run_data(run_info)

# 1. Discover runs
runs = get_all_runs()
if not runs:
    st.warning("No benchmark runs found in `benchmark-runs/` directory.")
    st.stop()

# 2. Group by environment
env_to_runs = {}
for run in runs:
    env = run["env"]
    if env not in env_to_runs:
        env_to_runs[env] = []
    env_to_runs[env].append(run)

environments = list(env_to_runs.keys())

st.sidebar.header("Configuration")
selected_envs = st.sidebar.multiselect("Select Environments", environments, default=environments)

# 3. Select runs
st.sidebar.subheader("Select Runs to Compare")
st.sidebar.markdown("*Soft cap recommended: 4-5 runs to prevent chart clutter.*")

selected_run_infos = []

for env in selected_envs:
    st.sidebar.markdown(f"**{env.upper()}**")
    env_runs = env_to_runs[env]
    # Format options for the dropdown
    options = {f"{r['timestamp']} (Commit: {r['commit'][:7]})": r for r in env_runs}
    
    # By default, select the most recent one
    default_sel = [list(options.keys())[0]] if options else []
    
    selected_for_env = st.sidebar.multiselect(
        f"Runs for {env}", 
        options=list(options.keys()), 
        default=default_sel,
        key=f"select_{env}"
    )
    
    for sel in selected_for_env:
        selected_run_infos.append(options[sel])

if not selected_run_infos:
    st.info("Please select at least one run from the sidebar to view the report.")
    st.stop()

# 4. Load Data
run_datasets = []
for r in selected_run_infos:
    data = get_run_data(r)
    if data:
        run_datasets.append(data)

if not run_datasets:
    st.error("Could not load data for the selected runs. The .hgrm files might be missing or corrupt.")
    st.stop()

# 5. Build Summary Table
st.header("Summary Metrics")
st.markdown("All values in **nanoseconds (ns)**.")

summary_records = []
for dataset in run_datasets:
    info = dataset["info"]
    stats = dataset["stats"]
    record = {
        "Environment": info["env"],
        "Timestamp": info["timestamp"],
        "Commit": info["commit"][:7],
        "P50": f"{stats['P50']:,}",
        "P90": f"{stats['P90']:,}",
        "P99": f"{stats['P99']:,}",
        "P99.9": f"{stats['P99.9']:,}",
        "Max": f"{stats['Max']:,}"
    }
    summary_records.append(record)

st.dataframe(pd.DataFrame(summary_records), use_container_width=True)

# 6. Plotting
st.header("Latency Distribution Overlay")
st.markdown("X-axis: Percentile (Log Scale) | Y-axis: Latency (ns)")

fig = go.Figure()

for dataset in run_datasets:
    info = dataset["info"]
    df = dataset["dataframe"]
    
    # Generate a readable label for the legend
    label = f"{info['env'].upper()} - {info['timestamp']} ({info['commit'][:7]})"
    
    # We plot the Inverse Percentile (1/(1-P)) on a log scale for the X-axis
    # This matches the standard HdrHistogram plotting style to stretch the tail
    fig.add_trace(go.Scatter(
        x=df['1/(1-Percentile)'], 
        y=df['Value'],
        mode='lines',
        name=label,
        line=dict(width=2)
    ))

# Standard HdrHistogram tick values
# 1/(1-P) where P = 0, 0.9, 0.99, 0.999, 0.9999, 0.99999
tickvals = [1, 10, 100, 1000, 10000, 100000]
ticktext = ['0%', '90%', '99%', '99.9%', '99.99%', '99.999%']

fig.update_layout(
    xaxis_type="log",
    xaxis=dict(
        title="Percentile",
        tickmode='array',
        tickvals=tickvals,
        ticktext=ticktext,
        gridcolor='rgba(200, 200, 200, 0.2)'
    ),
    yaxis=dict(
        title="Latency (ns)",
        rangemode="tozero",
        gridcolor='rgba(200, 200, 200, 0.2)'
    ),
    plot_bgcolor='white',
    hovermode="x unified",
    legend=dict(
        orientation="h",
        yanchor="bottom",
        y=1.02,
        xanchor="right",
        x=1
    ),
    margin=dict(l=20, r=20, t=60, b=20),
    height=600
)

st.plotly_chart(fig, use_container_width=True)

# Instructions for user if they add too many
if len(selected_run_infos) > 5:
    st.info("💡 You have selected more than 5 runs. You can click on the legend items above to toggle lines on and off for better readability.")
