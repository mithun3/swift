package com.fx.common.telemetry;

/**
 * {@code TelemetryBootstrap} — shared resolution of the telemetry system-property
 * configuration ({@code fx.telemetry.enabled}, {@code fx.telemetry.log.path}) used
 * identically by every {@code *Main} entry point.
 *
 * <p>Startup-only helper (not on the event-loop hot path), so ordinary String
 * operations here carry no zero-GC implications.
 */
public final class TelemetryBootstrap {

    private static final String ENABLED_PROPERTY = "fx.telemetry.enabled";
    private static final String LOG_PATH_PROPERTY = "fx.telemetry.log.path";
    private static final String DEFAULT_LOG_PATH = "/tmp/fx-latency.hlog";
    private static final String HLOG_SUFFIX = ".hlog";

    private TelemetryBootstrap() {
    }

    /** Whether telemetry recording is enabled ({@code fx.telemetry.enabled}, default {@code true}). */
    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"));
    }

    /** Raw configured log path ({@code fx.telemetry.log.path}, default {@code /tmp/fx-latency.hlog}). */
    public static String logPath() {
        return System.getProperty(LOG_PATH_PROPERTY, DEFAULT_LOG_PATH);
    }

    /** {@link #logPath()} with the {@code .hlog} suffix stripped, for per-stage file naming. */
    public static String basePath() {
        return logPath().replace(HLOG_SUFFIX, "");
    }
}
