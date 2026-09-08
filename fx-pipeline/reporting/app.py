import streamlit as st
import pandas as pd
import plotly.graph_objects as go
import plotly.express as px
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

# Extract unique filter dimensions
environments = list(set(r["env"] for r in runs))
cpu_models = list(set(r.get("cpu_model", "Unknown") for r in runs))
transport_modes = list(set(r.get("transport_mode", "tcp") for r in runs))
target_rates = list(set(r.get("target_rate", "N/A") for r in runs))

st.sidebar.header("Filters")
selected_envs = st.sidebar.multiselect("Environments", environments, default=environments)
selected_cpus = st.sidebar.multiselect("CPU Models", cpu_models, default=cpu_models)
selected_transports = st.sidebar.multiselect("Transport Mode", transport_modes, default=transport_modes)
selected_rates = st.sidebar.multiselect("Target Rate", target_rates, default=target_rates)

# Filter runs
filtered_runs = [
    r for r in runs 
    if r["env"] in selected_envs 
    and r.get("cpu_model", "Unknown") in selected_cpus
    and r.get("transport_mode", "tcp") in selected_transports
    and r.get("target_rate", "N/A") in selected_rates
]

# Group by environment for the selector
env_to_runs = {}
for run in filtered_runs:
    env = run["env"]
    if env not in env_to_runs:
        env_to_runs[env] = []
    env_to_runs[env].append(run)

st.sidebar.header("Select Runs to Compare")
selected_run_infos = []

for env in selected_envs:
    if env not in env_to_runs: continue
    st.sidebar.markdown(f"**{env.upper()}**")
    env_runs = env_to_runs[env]
    options = {f"{r['timestamp']} (Commit: {r['commit'][:7]})": r for r in env_runs}
    
    # Select the most recent one by default
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

# Load Data
run_datasets = []
for r in selected_run_infos:
    data = get_run_data(r)
    if data:
        run_datasets.append(data)

if not run_datasets:
    st.error("Could not load data for the selected runs.")
    st.stop()

tab_summary, tab_overlay, tab_trend, tab_variance, tab_stages = st.tabs([
    "Summary Metrics", "Latency Overlay", "Trend Analysis", "Variance Box Plots", "Stage Breakdown"
])

with tab_summary:
    st.header("Summary Metrics")
    st.markdown("All values in **nanoseconds (ns)**.")
    summary_records = []
    for dataset in run_datasets:
        info = dataset["info"]
        stats = dataset["stats"]
        record = {
            "Environment": info["env"],
            "Timestamp": info["timestamp"],
            "CPU Model": info.get("cpu_model", "Unknown"),
            "Rate": info.get("target_rate", "N/A"),
            "Commit": info["commit"][:7],
            "P50": stats['P50'],
            "P90": stats['P90'],
            "P99": stats['P99'],
            "P99.9": stats['P99.9'],
            "Max": stats['Max']
        }
        summary_records.append(record)

    df_summary = pd.DataFrame(summary_records)
    # Apply heatmap gradient on latency columns
    numeric_cols = ["P50", "P90", "P99", "P99.9", "Max"]
    styled_df = df_summary.style.background_gradient(cmap='YlOrRd', subset=numeric_cols).format({col: "{:,.0f}" for col in numeric_cols})
    st.dataframe(styled_df, use_container_width=True)

with tab_overlay:
    st.header("Latency Distribution Overlay")
    st.markdown("X-axis: Percentile (Log Scale) | Y-axis: Latency (ns)")

    fig = go.Figure()
    for dataset in run_datasets:
        info = dataset["info"]
        df = dataset["dataframe"]
        label = f"{info['env'].upper()} - {info['timestamp']}"
        fig.add_trace(go.Scatter(
            x=df['1/(1-Percentile)'], 
            y=df['Value'],
            mode='lines',
            name=label,
            line=dict(width=2)
        ))

    tickvals = [1, 10, 100, 1000, 10000, 100000]
    ticktext = ['0%', '90%', '99%', '99.9%', '99.99%', '99.999%']
    fig.update_layout(
        xaxis_type="log",
        xaxis=dict(title="Percentile", tickmode='array', tickvals=tickvals, ticktext=ticktext, gridcolor='rgba(200, 200, 200, 0.2)'),
        yaxis=dict(title="Latency (ns)", rangemode="tozero", gridcolor='rgba(200, 200, 200, 0.2)'),
        plot_bgcolor='white', hovermode="x unified", height=600,
        legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1)
    )
    st.plotly_chart(fig, use_container_width=True)

with tab_trend:
    st.header("Longitudinal Trend Analysis")
    st.markdown("Track P99 latency creep over time (sorted by timestamp).")
    
    # Sort for time series
    trend_df = df_summary.sort_values(by="Timestamp")
    fig_trend = px.line(trend_df, x="Timestamp", y="P99", color="Environment", markers=True, 
                        hover_data=["Commit", "CPU Model"], title="P99 Latency Over Time")
    fig_trend.update_layout(yaxis_title="P99 Latency (ns)")
    st.plotly_chart(fig_trend, use_container_width=True)

with tab_variance:
    st.header("Variance Analysis (Box Plots)")
    st.markdown("Compare the stability (variance) of P99 latencies across environments.")
    
    fig_box = px.box(df_summary, x="Environment", y="P99", color="Environment", points="all",
                     hover_data=["Timestamp", "Commit", "CPU Model"], title="P99 Variance Across Environments")
    fig_box.update_layout(yaxis_title="P99 Latency (ns)")
    st.plotly_chart(fig_box, use_container_width=True)

with tab_stages:
    st.header("Stage Breakdown & A/B Diffing")
    options = {f"{d['info']['env'].upper()} - {d['info']['timestamp']}": d for d in run_datasets}
    if len(options) >= 2:
        col1, col2, col3 = st.columns(3)
        with col1:
            base_label = st.selectbox("Baseline Run", list(options.keys()), index=0, key="base_run_sel")
        with col2:
            comp_label = st.selectbox("Comparison Run", list(options.keys()), index=1, key="comp_run_sel")
        with col3:
            metric = st.selectbox("Metric to Diff", ["P50", "P90", "P99", "P99.9", "Max"], index=2)
            
        base_stages = options[base_label].get("stages", {})
        comp_stages = options[comp_label].get("stages", {})
        
        if base_stages and comp_stages:
            diff_records = []
            all_stages = set(base_stages.keys()).intersection(set(comp_stages.keys()))
            for stage_name in all_stages:
                base_val = base_stages[stage_name][metric]
                comp_val = comp_stages[stage_name][metric]
                diff_val = comp_val - base_val
                pct_change = (diff_val / base_val * 100) if base_val > 0 else 0
                diff_records.append({
                    "Stage": stage_name,
                    f"Base {metric}": f"{base_val:,}",
                    f"Comp {metric}": f"{comp_val:,}",
                    "Delta": f"{diff_val:,}",
                    "% Change": pct_change
                })
            
            stage_order = ['serv-0', 'queue-a', 'serv-a', 'queue-b', 'serv-b', 'queue-c', 'serv-c']
            diff_records.sort(key=lambda x: stage_order.index(x['Stage']) if x['Stage'] in stage_order else 99)
            diff_df = pd.DataFrame(diff_records)
            
            def color_pct(val):
                color = 'red' if val > 5 else 'green' if val < -5 else 'gray'
                return f'color: {color}'
                
            formatted_df = diff_df.style.map(color_pct, subset=['% Change']).format({"% Change": "{:+.2f}%"})
            st.dataframe(formatted_df, use_container_width=True)
        else:
            st.warning("Missing stage breakdown data for selected runs.")
    else:
        st.info("Select at least 2 runs to diff stages.")
