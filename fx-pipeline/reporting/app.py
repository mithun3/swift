import streamlit as st
import pandas as pd
import plotly.graph_objects as go
import plotly.express as px
import logging
from typing import List, Dict, Any, Set, Optional
from collections import defaultdict

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(name)s - %(message)s")
logger = logging.getLogger(__name__)

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
def get_all_runs() -> List[Dict[str, Any]]:
    """Fetches all historical runs from the benchmark-runs directory."""
    logger.info("Discovering all historical benchmark runs.")
    return discover_runs()

@st.cache_data
def get_run_data(run_info: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Loads and parses the dataframe and stats for a specific run."""
    logger.info("Loading run data for run_id: %s", run_info.get("run_id"))
    return load_run_data(run_info)

# 1. Discover runs
runs = get_all_runs()
if not runs:
    logger.warning("No benchmark runs found.")
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
env_to_runs = defaultdict(list)
for run in filtered_runs:
    env_to_runs[run["env"]].append(run)

st.sidebar.header("Select Runs to Compare")
st.sidebar.caption("Used for Latency Overlay & Stage Diff comparisons.")
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

st.sidebar.header("Display Settings")
unit_options = {
    "Nanoseconds (ns)": ("ns", 1.0),
    "Microseconds (µs)": ("µs", 1000.0),
    "Milliseconds (ms)": ("ms", 1000000.0)
}
selected_unit_label = st.sidebar.selectbox("Latency Unit", list(unit_options.keys()), index=0)
unit_symbol, unit_divisor = unit_options[selected_unit_label]

# Load All Filtered Data (for Full History Summary & Longitudinal Analysis)
all_run_datasets = []
for r in filtered_runs:
    data = get_run_data(r)
    if data:
        all_run_datasets.append(data)

if not all_run_datasets:
    logger.error("Data loading failed for filtered runs.")
    st.warning("Could not load data for the filtered runs.")
    st.stop()

# Filter comparison datasets based on sidebar selections
selected_run_ids = {r["run_id"] for r in selected_run_infos}
comparison_datasets = [d for d in all_run_datasets if d["info"]["run_id"] in selected_run_ids]

def get_converted_stats(stats: Dict[str, float]) -> Dict[str, float]:
    """Converts the raw nanosecond stats into the user-selected unit."""
    return {k: v / unit_divisor for k, v in stats.items()}

def render_summary_tab(tab, all_datasets: List[Dict[str, Any]], sel_run_ids: Set[str]) -> pd.DataFrame:
    """
    Renders the summary tab showing KPIs and a data table of all filtered runs.
    """
    with tab:
        st.header("Summary Metrics (Full History)")
        st.markdown(f"Displaying all **{len(all_datasets)}** benchmark runs matching active filters. Latency values in **{unit_symbol}**.")
        
        summary_records = []
        best_p99 = float('inf')
        best_run_env = ""
        
        for dataset in all_datasets:
            info = dataset["info"]
            stats = get_converted_stats(dataset["stats"])
            is_comparing = info["run_id"] in sel_run_ids
            
            if stats['P99'] < best_p99:
                best_p99 = stats['P99']
                best_run_env = info["env"]
                
            record = {
                "Comparing": "✓" if is_comparing else "",
                "Environment": info["env"],
                "Timestamp": info["timestamp"],
                "CPU Model": info.get("cpu_model", "Unknown"),
                "Rate": info.get("target_rate", "N/A"),
                "Commit": info["commit"][:7],
                "P50": stats['P50'],
                "P90": stats['P90'],
                "P99": stats['P99'],
                "P99.9": stats['P99.9'],
                "P99.99": stats.get('P99.99', 0.0),
                "Max": stats['Max']
            }
            summary_records.append(record)

        if summary_records:
            col1, col2, col3 = st.columns(3)
            col1.metric(label="Total Filtered Runs", value=len(summary_records))
            
            best_p99_str = f"{best_p99:,.2f}" if unit_divisor > 1 else f"{best_p99:,.0f}"
            col2.metric(label=f"Best P99 Latency ({unit_symbol})", value=best_p99_str, delta=f"Env: {best_run_env}", delta_color="off")
            
            latest_run = max(summary_records, key=lambda x: x["Timestamp"])
            latest_p99 = latest_run["P99"]
            latest_p99_str = f"{latest_p99:,.2f}" if unit_divisor > 1 else f"{latest_p99:,.0f}"
            col3.metric(label=f"Latest P99 Latency ({unit_symbol})", value=latest_p99_str, delta=f"Env: {latest_run['Environment']}", delta_color="off")
            
            st.divider()

        df_summary = pd.DataFrame(summary_records)
        
        # Add a download button
        csv = df_summary.to_csv(index=False).encode('utf-8')
        st.download_button(
            label=f"Download Summary Data as CSV",
            data=csv,
            file_name='fx_pipeline_summary.csv',
            mime='text/csv',
        )
        
        numeric_cols = ["P50", "P90", "P99", "P99.9", "P99.99", "Max"]
        styled_df = df_summary.style.background_gradient(cmap='Blues', subset=numeric_cols).format({col: "{:,.2f}" if unit_divisor > 1 else "{:,.0f}" for col in numeric_cols})
        st.dataframe(styled_df, use_container_width=True)
        return df_summary

def render_overlay_tab(tab, comp_datasets: List[Dict[str, Any]]):
    """
    Renders the Latency Distribution Overlay tab, 
    plotting the 1/(1-Percentile) latency curve.
    """
    with tab:
        st.header("Latency Distribution Overlay")
        st.markdown(f"X-axis: Percentile (Log Scale) | Y-axis: Latency ({unit_symbol})")

        if not comp_datasets:
            st.info("Please select at least one run in the sidebar under **Select Runs to Compare** to view the latency overlay.")
        else:
            fig = go.Figure()
            for dataset in comp_datasets:
                info = dataset["info"]
                df = dataset["dataframe"]
                label = f"{info['env'].upper()} - {info['timestamp']}"
                fig.add_trace(go.Scatter(
                    x=df['1/(1-Percentile)'], 
                    y=df['Value'] / unit_divisor,
                    mode='lines',
                    name=label,
                    line=dict(width=2)
                ))

            tickvals = [1, 10, 100, 1000, 10000, 100000]
            ticktext = ['0%', '90%', '99%', '99.9%', '99.99%', '99.999%']
            fig.update_layout(
                xaxis_type="log",
                xaxis=dict(title="Percentile", tickmode='array', tickvals=tickvals, ticktext=ticktext, gridcolor='rgba(200, 200, 200, 0.2)'),
                yaxis=dict(title=f"Latency ({unit_symbol})", rangemode="tozero", gridcolor='rgba(200, 200, 200, 0.2)'),
                plot_bgcolor='white', hovermode="x unified", height=600,
                legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1)
            )
            st.plotly_chart(fig, use_container_width=True)

def render_trend_tab(tab, df_summary: pd.DataFrame):
    """
    Renders the Longitudinal Trend Analysis tab to track latency creep.
    """
    with tab:
        st.header("Longitudinal Trend Analysis")
        st.markdown(f"Track P99 latency creep over time across all historical runs (sorted by timestamp).")
        
        trend_df = df_summary.sort_values(by="Timestamp")
        fig_trend = px.line(trend_df, x="Timestamp", y="P99", color="Environment", markers=True, 
                            hover_data=["Commit", "CPU Model"], title=f"P99 Latency Over Time ({unit_symbol})")
        fig_trend.update_layout(yaxis_title=f"P99 Latency ({unit_symbol})")
        st.plotly_chart(fig_trend, use_container_width=True)

def render_variance_tab(tab, df_summary: pd.DataFrame):
    """
    Renders the Variance Analysis tab for environment stability comparison.
    """
    with tab:
        st.header("Variance Analysis (Box Plots)")
        st.markdown("Compare the stability (variance) of P99 latencies across environments.")
        
        fig_box = px.box(df_summary, x="Environment", y="P99", color="Environment", points="all",
                         hover_data=["Timestamp", "Commit", "CPU Model"], title=f"P99 Variance Across Environments ({unit_symbol})")
        fig_box.update_layout(yaxis_title=f"P99 Latency ({unit_symbol})")
        st.plotly_chart(fig_box, use_container_width=True)

def render_stages_tab(tab, comp_datasets: List[Dict[str, Any]]):
    """
    Renders the Stage Breakdown & A/B Diffing tab 
    for detailed latency analysis per pipeline stage.
    """
    with tab:
        st.header("Stage Breakdown & A/B Diffing")
        options = {f"{d['info']['env'].upper()} - {d['info']['timestamp']}": d for d in comp_datasets}
        if len(options) >= 2:
            col1, col2, col3 = st.columns(3)
            with col1:
                base_label = st.selectbox("Baseline Run", list(options.keys()), index=0, key="base_run_sel")
            with col2:
                comp_label = st.selectbox("Comparison Run", list(options.keys()), index=1, key="comp_run_sel")
            with col3:
                metric = st.selectbox("Metric to Diff", ["P50", "P90", "P99", "P99.9", "P99.99", "Max"], index=2)
                
            base_stages = options[base_label].get("stages", {})
            comp_stages = options[comp_label].get("stages", {})
            
            if base_stages and comp_stages:
                diff_records = []
                all_stages = set(base_stages.keys()).intersection(set(comp_stages.keys()))
                for stage_name in all_stages:
                    base_val = base_stages[stage_name].get(metric, 0) / unit_divisor
                    comp_val = comp_stages[stage_name].get(metric, 0) / unit_divisor
                    diff_val = comp_val - base_val
                    pct_change = (diff_val / base_val * 100) if base_val > 0 else 0
                    
                    fmt = "{:,.2f}" if unit_divisor > 1 else "{:,.0f}"
                    
                    diff_records.append({
                        "Stage": stage_name,
                        f"Base {metric}": fmt.format(base_val),
                        f"Comp {metric}": fmt.format(comp_val),
                        "Delta": fmt.format(diff_val),
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
            st.info("Please select at least 2 runs under **Select Runs to Compare** in the sidebar to diff stages.")

# Setup Tabs and Render
tab_summary, tab_overlay, tab_trend, tab_variance, tab_stages = st.tabs([
    "Summary Metrics", "Latency Overlay", "Trend Analysis", "Variance Box Plots", "Stage Breakdown"
])

summary_df = render_summary_tab(tab_summary, all_run_datasets, selected_run_ids)
render_overlay_tab(tab_overlay, comparison_datasets)
render_trend_tab(tab_trend, summary_df)
render_variance_tab(tab_variance, summary_df)
render_stages_tab(tab_stages, comparison_datasets)
