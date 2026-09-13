import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import API_URL from "../config/api";
import {
    Activity,
    CheckCircle2,
    Clock3,
    Gauge,
    GitCompare,
    Map,
    Network,
    Route,
    Timer,
    XCircle,
    Zap,
    Ambulance,
    Hospital,
    ClipboardList,
    TrendingUp,
} from "lucide-react";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from "recharts";
import { compareAlgorithms } from "../services/algorithmService";

const DEFAULT_SOURCE = "11955558230";
const DEFAULT_DESTINATION = "3206828997";

function MetricCard({
                        icon: Icon,
                        label,
                        value,
                        suffix = "",
                        description = "",
                    }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
            <div className="mb-4 flex items-center justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-800">
                    <Icon size={20} className="text-red-400" />
                </div>
            </div>

            <p className="text-sm text-slate-400">{label}</p>

            <div className="mt-1 flex items-baseline gap-1">
                <span className="text-2xl font-bold text-white">{value}</span>
                {suffix && (
                    <span className="text-sm text-slate-400">{suffix}</span>
                )}
            </div>

            {description && (
                <p className="mt-2 text-xs text-slate-500">{description}</p>
            )}
        </div>
    );
}

function AlgorithmCard({ result, title, accent }) {
    const isDijkstra = result.algorithm === "TRAFFIC_DIJKSTRA";

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-widest text-slate-500">
                        Algorithm
                    </p>
                    <h3 className={`mt-1 text-xl font-bold ${accent}`}>
                        {title}
                    </h3>
                </div>

                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-slate-800">
                    {isDijkstra ? (
                        <Network size={21} className="text-red-400" />
                    ) : (
                        <Zap size={21} className="text-amber-400" />
                    )}
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <Stat label="Distance" value={`${result.distanceKm} km`} />
                <Stat
                    label="Travel Time"
                    value={`${result.estimatedTravelTimeMinutes} min`}
                />
                <Stat
                    label="Nodes Explored"
                    value={result.nodesExplored.toLocaleString()}
                />
                <Stat
                    label="Avg Time"
                    value={`${result.averageExecutionTimeMillis.toFixed(1)} ms`}
                />
                <Stat
                    label="Median Time"
                    value={`${result.medianExecutionTimeMillis.toFixed(1)} ms`}
                />
                <Stat
                    label="Traffic"
                    value={result.trafficLevel}
                />
            </div>
        </div>
    );
}

function Stat({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
            <p className="text-xs text-slate-500">{label}</p>
            <p className="mt-1 text-sm font-semibold text-slate-200">
                {value}
            </p>
        </div>
    );
}

function ComparisonBadge({ label, value }) {
    return (
        <div className="flex items-center justify-between rounded-xl border border-slate-800 bg-slate-900/60 px-4 py-3">
            <span className="text-sm text-slate-400">{label}</span>

            <span
                className={`flex items-center gap-2 text-sm font-semibold ${
                    value ? "text-emerald-400" : "text-red-400"
                }`}
            >
                {value ? (
                    <CheckCircle2 size={17} />
                ) : (
                    <XCircle size={17} />
                )}
                {value ? "MATCH" : "DIFFERENT"}
            </span>
        </div>
    );
}

function formatNumber(value) {
    if (value === null || value === undefined) {
        return "—";
    }

    return Number(value).toLocaleString(undefined, {
        maximumFractionDigits: 1,
    });
}

export default function AlgorithmAnalytics() {
    const [sourceNode, setSourceNode] = useState(() => {
        return localStorage.getItem("ambulanceos_algorithm_source") || DEFAULT_SOURCE;
    });

    const [destinationNode, setDestinationNode] = useState(() => {
        return localStorage.getItem("ambulanceos_algorithm_destination") || DEFAULT_DESTINATION;
    });

    const [result, setResult] = useState(() => {
        try {
            const savedResult = localStorage.getItem("ambulanceos_algorithm_result");
            return savedResult ? JSON.parse(savedResult) : null;
        } catch {
            return null;
        }
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        localStorage.setItem("ambulanceos_algorithm_source", sourceNode);
    }, [sourceNode]);

    useEffect(() => {
        localStorage.setItem("ambulanceos_algorithm_destination", destinationNode);
    }, [destinationNode]);

    useEffect(() => {
        if (result) {
            localStorage.setItem(
                "ambulanceos_algorithm_result",
                JSON.stringify(result)
            );
        } else {
            localStorage.removeItem("ambulanceos_algorithm_result");
        }
    }, [result]);

    // Operational analytics are derived from persisted dispatch history.
    const [dispatchHistory, setDispatchHistory] = useState([]);
    const [historyLoading, setHistoryLoading] = useState(true);
    const [historyError, setHistoryError] = useState("");

    useEffect(() => {
        loadOperationalAnalytics();
    }, []);

    const loadOperationalAnalytics = async () => {
        try {
            setHistoryLoading(true);
            setHistoryError("");

            const response = await axios.get(
                `${API_URL}/dispatches`
            );

            setDispatchHistory(response.data || []);
        } catch (err) {
            console.error("Failed to load dispatch analytics:", err);
            setHistoryError(
                "Unable to load operational dispatch data from the backend."
            );
        } finally {
            setHistoryLoading(false);
        }
    };

    const operationalStats = useMemo(() => {
        const total = dispatchHistory.length;
        const completed = dispatchHistory.filter(
            (dispatch) => dispatch.status === "COMPLETED"
        ).length;
        const inProgress = dispatchHistory.filter(
            (dispatch) => dispatch.status === "IN_PROGRESS"
        ).length;

        const averageDistance = total
            ? dispatchHistory.reduce(
            (sum, dispatch) =>
                sum + Number(dispatch.totalDistanceKm || 0),
            0
        ) / total
            : 0;

        const averageEstimatedTime = total
            ? dispatchHistory.reduce(
            (sum, dispatch) =>
                sum + Number(
                    dispatch.totalEstimatedTimeMinutes || 0
                ),
            0
        ) / total
            : 0;

        const ambulanceCounts = {};
        const hospitalCounts = {};

        dispatchHistory.forEach((dispatch) => {
            const ambulance = dispatch.ambulanceNumber || "Unknown";
            const hospital = dispatch.hospitalName || "Unknown";

            ambulanceCounts[ambulance] =
                (ambulanceCounts[ambulance] || 0) + 1;
            hospitalCounts[hospital] =
                (hospitalCounts[hospital] || 0) + 1;
        });

        const ambulanceChart = Object.entries(ambulanceCounts)
            .map(([name, dispatches]) => ({ name, dispatches }))
            .sort((a, b) => b.dispatches - a.dispatches);

        const hospitalChart = Object.entries(hospitalCounts)
            .map(([name, dispatches]) => ({ name, dispatches }))
            .sort((a, b) => b.dispatches - a.dispatches);

        return {
            total,
            completed,
            inProgress,
            averageDistance,
            averageEstimatedTime,
            completionRate: total ? (completed / total) * 100 : 0,
            ambulanceChart,
            hospitalChart,
        };
    }, [dispatchHistory]);

    const runComparison = async () => {
        if (!sourceNode.trim() || !destinationNode.trim()) {
            setError("Please enter both source and destination node IDs.");
            return;
        }

        setLoading(true);
        setError("");

        try {
            const data = await compareAlgorithms(
                sourceNode.trim(),
                destinationNode.trim()
            );

            setResult(data);
        } catch (err) {
            console.error("Algorithm comparison failed:", err);

            const message =
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                "Unable to run algorithm comparison. Check the node IDs and backend.";

            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const dijkstra = result?.dijkstra;
    const aStar = result?.aStar;

    const chartData =
        dijkstra && aStar
            ? [
                {
                    metric: "Average Time",
                    Dijkstra: Number(
                        dijkstra.averageExecutionTimeMillis
                    ),
                    AStar: Number(aStar.averageExecutionTimeMillis),
                },
                {
                    metric: "Median Time",
                    Dijkstra: Number(
                        dijkstra.medianExecutionTimeMillis
                    ),
                    AStar: Number(aStar.medianExecutionTimeMillis),
                },
                {
                    metric: "Min Time",
                    Dijkstra: Number(
                        dijkstra.minimumExecutionTimeMillis
                    ),
                    AStar: Number(aStar.minimumExecutionTimeMillis),
                },
                {
                    metric: "Max Time",
                    Dijkstra: Number(
                        dijkstra.maximumExecutionTimeMillis
                    ),
                    AStar: Number(aStar.maximumExecutionTimeMillis),
                },
            ]
            : [];

    const nodeChartData =
        dijkstra && aStar
            ? [
                {
                    metric: "Nodes Explored",
                    Dijkstra: Number(dijkstra.averageNodesExplored),
                    AStar: Number(aStar.averageNodesExplored),
                },
            ]
            : [];

    return (
        <div className="space-y-6">
            {/* Operational analytics */}
            <div className="space-y-6">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <div className="flex items-center gap-3">
                            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-red-500/10">
                                <TrendingUp size={22} className="text-red-400" />
                            </div>
                            <div>
                                <p className="text-xs font-semibold uppercase tracking-widest text-red-400">
                                    Operations
                                </p>
                                <h1 className="text-2xl font-bold text-white">
                                    Operational Analytics
                                </h1>
                            </div>
                        </div>
                        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-400">
                            Performance metrics derived from persisted ambulance dispatch records in PostgreSQL.
                        </p>
                    </div>

                    <button
                        onClick={loadOperationalAnalytics}
                        disabled={historyLoading}
                        className="rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {historyLoading ? "Refreshing..." : "Refresh Data"}
                    </button>
                </div>

                {historyError && (
                    <div className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                        {historyError}
                    </div>
                )}

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
                    <MetricCard
                        icon={ClipboardList}
                        label="Total Dispatches"
                        value={operationalStats.total}
                        description="Persisted operations"
                    />
                    <MetricCard
                        icon={CheckCircle2}
                        label="Completed"
                        value={operationalStats.completed}
                        description={`${operationalStats.completionRate.toFixed(1)}% completion rate`}
                    />
                    <MetricCard
                        icon={Activity}
                        label="In Progress"
                        value={operationalStats.inProgress}
                        description="Currently active records"
                    />
                    <MetricCard
                        icon={Route}
                        label="Average Distance"
                        value={operationalStats.averageDistance.toFixed(2)}
                        suffix="km"
                        description="Per dispatch"
                    />
                    <MetricCard
                        icon={Clock3}
                        label="Average Estimated Time"
                        value={operationalStats.averageEstimatedTime.toFixed(2)}
                        suffix="min"
                        description="Traffic-adjusted trip estimate"
                    />
                </div>

                {dispatchHistory.length > 0 && (
                    <div className="grid gap-6 lg:grid-cols-2">
                        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                            <div className="mb-5 flex items-center gap-3">
                                <Ambulance size={20} className="text-red-400" />
                                <div>
                                    <h2 className="font-semibold text-white">
                                        Ambulance Utilization
                                    </h2>
                                    <p className="text-xs text-slate-500">
                                        Dispatches handled by each ambulance.
                                    </p>
                                </div>
                            </div>
                            <div className="h-64">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={operationalStats.ambulanceChart}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                                        <XAxis dataKey="name" stroke="#94a3b8" />
                                        <YAxis allowDecimals={false} stroke="#94a3b8" />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: "#0f172a",
                                                border: "1px solid #334155",
                                                borderRadius: "12px",
                                                color: "#fff",
                                            }}
                                        />
                                        <Bar dataKey="dispatches" name="Dispatches" fill="#ef4444" radius={[6, 6, 0, 0]} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                            <div className="mb-5 flex items-center gap-3">
                                <Hospital size={20} className="text-emerald-400" />
                                <div>
                                    <h2 className="font-semibold text-white">
                                        Hospital Utilization
                                    </h2>
                                    <p className="text-xs text-slate-500">
                                        Number of dispatches routed to each hospital.
                                    </p>
                                </div>
                            </div>
                            <div className="h-64">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={operationalStats.hospitalChart}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                                        <XAxis dataKey="name" stroke="#94a3b8" />
                                        <YAxis allowDecimals={false} stroke="#94a3b8" />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: "#0f172a",
                                                border: "1px solid #334155",
                                                borderRadius: "12px",
                                                color: "#fff",
                                            }}
                                        />
                                        <Bar dataKey="dispatches" name="Dispatches" fill="#10b981" radius={[6, 6, 0, 0]} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Header */}
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                    <div className="flex items-center gap-3">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-red-500/10">
                            <GitCompare
                                size={22}
                                className="text-red-400"
                            />
                        </div>

                        <div>
                            <p className="text-xs font-semibold uppercase tracking-widest text-red-400">
                                DSA Performance
                            </p>
                            <h1 className="text-2xl font-bold text-white">
                                Algorithm Analytics
                            </h1>
                        </div>
                    </div>

                    <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-400">
                        Compare traffic-aware Dijkstra and A* on the Gurgaon
                        OSM road graph using repeated warm-up and benchmark
                        runs.
                    </p>
                </div>

                {result && (
                    <div className="flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-400">
                        <CheckCircle2 size={16} />
                        Benchmark completed
                    </div>
                )}
            </div>

            {/* Input panel */}
            <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
                <div className="mb-5 flex items-center gap-3">
                    <Map size={19} className="text-slate-400" />
                    <div>
                        <h2 className="font-semibold text-white">
                            Route Benchmark
                        </h2>
                        <p className="text-xs text-slate-500">
                            Enter valid node IDs from the loaded Gurgaon graph.
                        </p>
                    </div>
                </div>

                <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
                    <div>
                        <label className="mb-2 block text-xs font-medium text-slate-400">
                            Source Node
                        </label>
                        <input
                            value={sourceNode}
                            onChange={(event) =>
                                setSourceNode(event.target.value)
                            }
                            className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-red-500"
                            placeholder="e.g. 11955558230"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-xs font-medium text-slate-400">
                            Destination Node
                        </label>
                        <input
                            value={destinationNode}
                            onChange={(event) =>
                                setDestinationNode(event.target.value)
                            }
                            className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-red-500"
                            placeholder="e.g. 3206828997"
                        />
                    </div>

                    <button
                        onClick={runComparison}
                        disabled={loading}
                        className="self-end rounded-xl bg-red-500 px-6 py-3 text-sm font-semibold text-white transition hover:bg-red-400 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <Activity
                                    size={17}
                                    className="animate-pulse"
                                />
                                Running...
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <Zap size={17} />
                                Compare
                            </span>
                        )}
                    </button>
                </div>

                {error && (
                    <div className="mt-4 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                        {error}
                    </div>
                )}
            </div>

            {/* Empty state */}
            {!result && !loading && (
                <div className="rounded-2xl border border-dashed border-slate-800 bg-slate-900/40 px-6 py-16 text-center">
                    <GitCompare
                        size={38}
                        className="mx-auto text-slate-600"
                    />
                    <h2 className="mt-4 text-lg font-semibold text-slate-300">
                        No benchmark results yet
                    </h2>
                    <p className="mx-auto mt-2 max-w-lg text-sm text-slate-500">
                        Run a comparison to see route correctness, search-space
                        exploration, execution time, and benchmark statistics.
                    </p>
                </div>
            )}

            {result && dijkstra && aStar && (
                <>
                    {/* Route summary */}
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                        <MetricCard
                            icon={Route}
                            label="Optimal Distance"
                            value={dijkstra.distanceKm}
                            suffix="km"
                            description="Both algorithms"
                        />

                        <MetricCard
                            icon={Clock3}
                            label="Estimated Travel Time"
                            value={dijkstra.estimatedTravelTimeMinutes}
                            suffix="min"
                            description="Traffic-adjusted"
                        />

                        <MetricCard
                            icon={Network}
                            label="Dijkstra Nodes"
                            value={formatNumber(
                                dijkstra.averageNodesExplored
                            )}
                            description="Average explored"
                        />

                        <MetricCard
                            icon={Gauge}
                            label="A* Nodes"
                            value={formatNumber(aStar.averageNodesExplored)}
                            description="Average explored"
                        />
                    </div>

                    {/* Algorithm cards */}
                    <div className="grid gap-6 lg:grid-cols-2">
                        <AlgorithmCard
                            result={dijkstra}
                            title="Traffic-Aware Dijkstra"
                            accent="text-red-400"
                        />

                        <AlgorithmCard
                            result={aStar}
                            title="A* Search"
                            accent="text-amber-400"
                        />
                    </div>

                    {/* Correctness */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="mb-5 flex items-center gap-3">
                            <CheckCircle2
                                size={20}
                                className="text-emerald-400"
                            />
                            <div>
                                <h2 className="font-semibold text-white">
                                    Correctness Verification
                                </h2>
                                <p className="text-xs text-slate-500">
                                    A* is checked against the Dijkstra baseline.
                                </p>
                            </div>
                        </div>

                        <div className="grid gap-3 md:grid-cols-3">
                            <ComparisonBadge
                                label="Same optimal distance"
                                value={result.sameOptimalDistance}
                            />

                            <ComparisonBadge
                                label="Same travel time"
                                value={result.sameOptimalTravelTime}
                            />

                            <ComparisonBadge
                                label="Same node path"
                                value={result.samePath}
                            />
                        </div>
                    </div>

                    {/* Execution chart */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="mb-6 flex items-center justify-between">
                            <div>
                                <div className="flex items-center gap-3">
                                    <Timer
                                        size={20}
                                        className="text-red-400"
                                    />
                                    <h2 className="font-semibold text-white">
                                        Execution Time
                                    </h2>
                                </div>
                                <p className="mt-1 text-xs text-slate-500">
                                    Lower is better. Values are milliseconds.
                                </p>
                            </div>

                            <div className="rounded-lg border border-slate-800 bg-slate-950 px-3 py-2 text-xs text-slate-400">
                                {result.warmupRuns} warm-up +{" "}
                                {result.measuredRuns} measured runs
                            </div>
                        </div>

                        <div className="h-80 w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={chartData}
                                    margin={{
                                        top: 10,
                                        right: 10,
                                        left: 0,
                                        bottom: 10,
                                    }}
                                >
                                    <CartesianGrid
                                        strokeDasharray="3 3"
                                        stroke="#334155"
                                    />
                                    <XAxis
                                        dataKey="metric"
                                        tick={{ fill: "#94a3b8", fontSize: 12 }}
                                    />
                                    <YAxis
                                        tick={{ fill: "#94a3b8", fontSize: 12 }}
                                    />
                                    <Tooltip
                                        contentStyle={{
                                            backgroundColor: "#0f172a",
                                            border: "1px solid #334155",
                                            borderRadius: "12px",
                                            color: "#fff",
                                        }}
                                    />
                                    <Bar
                                        dataKey="Dijkstra"
                                        fill="#ef4444"
                                        radius={[6, 6, 0, 0]}
                                    />
                                    <Bar
                                        dataKey="AStar"
                                        fill="#f59e0b"
                                        radius={[6, 6, 0, 0]}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Nodes explored chart */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="mb-6">
                            <div className="flex items-center gap-3">
                                <Network
                                    size={20}
                                    className="text-red-400"
                                />
                                <h2 className="font-semibold text-white">
                                    Search Space
                                </h2>
                            </div>
                            <p className="mt-1 text-xs text-slate-500">
                                Average number of graph nodes explored during
                                the benchmark.
                            </p>
                        </div>

                        <div className="h-72 w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={nodeChartData}
                                    margin={{
                                        top: 10,
                                        right: 10,
                                        left: 0,
                                        bottom: 10,
                                    }}
                                >
                                    <CartesianGrid
                                        strokeDasharray="3 3"
                                        stroke="#334155"
                                    />
                                    <XAxis
                                        dataKey="metric"
                                        tick={{ fill: "#94a3b8", fontSize: 12 }}
                                    />
                                    <YAxis
                                        tick={{ fill: "#94a3b8", fontSize: 12 }}
                                    />
                                    <Tooltip
                                        formatter={(value) =>
                                            Number(value).toLocaleString()
                                        }
                                        contentStyle={{
                                            backgroundColor: "#0f172a",
                                            border: "1px solid #334155",
                                            borderRadius: "12px",
                                            color: "#fff",
                                        }}
                                    />
                                    <Bar
                                        dataKey="Dijkstra"
                                        fill="#ef4444"
                                        radius={[6, 6, 0, 0]}
                                    />
                                    <Bar
                                        dataKey="AStar"
                                        fill="#f59e0b"
                                        radius={[6, 6, 0, 0]}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Benchmark statistics */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="mb-5 flex items-center gap-3">
                            <Gauge size={20} className="text-red-400" />
                            <div>
                                <h2 className="font-semibold text-white">
                                    Benchmark Statistics
                                </h2>
                                <p className="text-xs text-slate-500">
                                    Repeated measurements reduce the impact of
                                    one-off runtime fluctuations.
                                </p>
                            </div>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full min-w-[650px] text-left">
                                <thead>
                                <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
                                    <th className="px-4 py-3">
                                        Metric
                                    </th>
                                    <th className="px-4 py-3">
                                        Dijkstra
                                    </th>
                                    <th className="px-4 py-3">A*</th>
                                </tr>
                                </thead>

                                <tbody className="text-sm">
                                <TableRow
                                    label="Average execution"
                                    dijkstra={`${dijkstra.averageExecutionTimeMillis.toFixed(
                                        1
                                    )} ms`}
                                    aStar={`${aStar.averageExecutionTimeMillis.toFixed(
                                        1
                                    )} ms`}
                                />

                                <TableRow
                                    label="Median execution"
                                    dijkstra={`${dijkstra.medianExecutionTimeMillis.toFixed(
                                        1
                                    )} ms`}
                                    aStar={`${aStar.medianExecutionTimeMillis.toFixed(
                                        1
                                    )} ms`}
                                />

                                <TableRow
                                    label="Minimum execution"
                                    dijkstra={`${dijkstra.minimumExecutionTimeMillis} ms`}
                                    aStar={`${aStar.minimumExecutionTimeMillis} ms`}
                                />

                                <TableRow
                                    label="Maximum execution"
                                    dijkstra={`${dijkstra.maximumExecutionTimeMillis} ms`}
                                    aStar={`${aStar.maximumExecutionTimeMillis} ms`}
                                />

                                <TableRow
                                    label="Average nodes explored"
                                    dijkstra={formatNumber(
                                        dijkstra.averageNodesExplored
                                    )}
                                    aStar={formatNumber(
                                        aStar.averageNodesExplored
                                    )}
                                />
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Route information */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="mb-5 flex items-center gap-3">
                            <Route size={20} className="text-red-400" />
                            <div>
                                <h2 className="font-semibold text-white">
                                    Route Information
                                </h2>
                                <p className="text-xs text-slate-500">
                                    Both algorithms returned the same path.
                                </p>
                            </div>
                        </div>

                        <div className="grid gap-4 md:grid-cols-3">
                            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                                <p className="text-xs text-slate-500">
                                    Source
                                </p>
                                <p className="mt-1 break-all font-mono text-sm text-slate-200">
                                    {result.sourceNode}
                                </p>
                            </div>

                            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                                <p className="text-xs text-slate-500">
                                    Destination
                                </p>
                                <p className="mt-1 break-all font-mono text-sm text-slate-200">
                                    {result.destinationNode}
                                </p>
                            </div>

                            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                                <p className="text-xs text-slate-500">
                                    Path Nodes
                                </p>
                                <p className="mt-1 text-sm font-semibold text-slate-200">
                                    {dijkstra.path.length.toLocaleString()}
                                </p>
                            </div>
                        </div>

                        <div className="mt-4 rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                            <p className="mb-2 text-xs text-slate-500">
                                Path preview
                            </p>
                            <p className="break-all font-mono text-xs leading-6 text-slate-400">
                                {dijkstra.path.slice(0, 12).join(" → ")}
                                {dijkstra.path.length > 12
                                    ? " → ... → " +
                                    dijkstra.path
                                        .slice(-3)
                                        .join(" → ")
                                    : ""}
                            </p>
                        </div>
                    </div>

                    {/* Interpretation */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                        <div className="flex items-start gap-4">
                            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
                                <Activity
                                    size={20}
                                    className="text-red-400"
                                />
                            </div>

                            <div>
                                <h2 className="font-semibold text-white">
                                    Result Interpretation
                                </h2>

                                <p className="mt-2 text-sm leading-6 text-slate-400">
                                    Both algorithms produced the same optimal
                                    route and traffic-adjusted travel time.
                                    Dijkstra acts as the optimality baseline,
                                    while A* uses a geographic heuristic to
                                    guide its search toward the destination.
                                </p>

                                <p className="mt-3 text-sm leading-6 text-slate-400">
                                    Runtime is measured on the local Java
                                    process, so individual results can vary
                                    because of JVM warm-up, CPU scheduling,
                                    memory pressure, and other system activity.
                                    The repeated benchmark statistics provide a
                                    more useful comparison than a single run.
                                </p>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

function TableRow({ label, dijkstra, aStar }) {
    return (
        <tr className="border-b border-slate-800/70">
            <td className="px-4 py-3 text-slate-400">{label}</td>
            <td className="px-4 py-3 font-medium text-slate-200">
                {dijkstra}
            </td>
            <td className="px-4 py-3 font-medium text-slate-200">
                {aStar}
            </td>
        </tr>
    );
}
