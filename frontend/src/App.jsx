import { useEffect, useRef, useState } from "react";
import axios from "axios";

import {
  Activity,
  Ambulance,
  Bell,
  Clock3,
  ClipboardList,
  Hospital,
  MapPin,
  Navigation,
  Route,
  Settings,
  ShieldCheck,
  Siren,
  Users,
} from "lucide-react";

import {
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  Polyline,
} from "react-leaflet";

import L from "leaflet";

import "leaflet/dist/leaflet.css";

import LocationPicker from "./components/LocationPicker";

import { getAllHospitals } from "./services/hospitalService";
import AlgorithmAnalytics from "./components/AlgorithmAnalytics";

import {
  createDispatchPlan,
} from "./services/dispatchPlanService";

import {
  completeDispatch,
} from "./services/dispatchHistoryService";


// =========================================================
// API CONFIGURATION
// =========================================================

const API_URL =
    import.meta.env.VITE_API_URL ||
    (window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1"
        ? "http://localhost:8080/api"
        : "https://ambulance-os-backend.onrender.com/api");

// =========================================================
// GURGAON MAP CENTER
// =========================================================

const GURGAON_CENTER = [28.4595, 77.0266];


// =========================================================
// CUSTOM MAP ICONS
// =========================================================

const ambulanceIcon = L.divIcon({
  className: "custom-map-icon",

  html: `
    <div style="
      background:#ef4444;
      width:38px;
      height:38px;
      border-radius:50%;
      display:flex;
      align-items:center;
      justify-content:center;
      border:3px solid white;
      box-shadow:0 4px 12px rgba(0,0,0,.35);
      font-size:20px;
    ">
      🚑
    </div>
  `,

  iconSize: [38, 38],
  iconAnchor: [19, 19],
});


const hospitalIcon = L.divIcon({
  className: "custom-map-icon",

  html: `
    <div style="
      background:#22c55e;
      width:34px;
      height:34px;
      border-radius:50%;
      display:flex;
      align-items:center;
      justify-content:center;
      border:3px solid white;
      box-shadow:0 4px 12px rgba(0,0,0,.35);
      font-size:17px;
    ">
      🏥
    </div>
  `,

  iconSize: [34, 34],
  iconAnchor: [17, 17],
});


const patientIcon = L.divIcon({
  className: "custom-map-icon",

  html: `
    <div style="
      background:#f59e0b;
      width:34px;
      height:34px;
      border-radius:50%;
      display:flex;
      align-items:center;
      justify-content:center;
      border:3px solid white;
      box-shadow:0 4px 12px rgba(0,0,0,.35);
      font-size:17px;
    ">
      📍
    </div>
  `,

  iconSize: [34, 34],
  iconAnchor: [17, 17],
});


// =========================================================
// SIDEBAR ITEM
// =========================================================

function SidebarItem({
                       icon: Icon,
                       label,
                       active = false,
                       onClick,
                     }) {
  return (
      <div
          onClick={onClick}
          className={`
        flex items-center gap-3
        px-4 py-3
        rounded-xl
        cursor-pointer
        transition-all
        ${
              active
                  ? "bg-red-500/15 text-red-400 border border-red-500/20"
                  : "text-slate-400 hover:bg-slate-800 hover:text-white"
          }
      `}
      >
        <Icon size={19} />

        <span className="text-sm font-medium">
        {label}
      </span>
      </div>
  );
}


// =========================================================
// STATISTIC CARD
// =========================================================

function StatCard({
                    icon: Icon,
                    label,
                    value,
                    description,
                  }) {
  return (
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">

        <div className="flex items-center justify-between">

          <div>

            <p className="text-xs text-slate-500 uppercase tracking-wider">
              {label}
            </p>

            <p className="text-2xl font-bold text-white mt-1">
              {value}
            </p>

          </div>

          <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center">

            <Icon
                size={20}
                className="text-red-400"
            />

          </div>

        </div>

        <p className="text-xs text-slate-500 mt-3">
          {description}
        </p>

      </div>
  );
}


// =========================================================
// MANAGEMENT PAGE
// =========================================================

function ManagementPage({
                          page,
                          onNavigate,
                          emergencies,
                          ambulances,
                          hospitals,
                          dispatchPlan,
                          tripPhase,
                          onDispatch,
                          loadingDispatch,
                          errorMessage,
                          routingAlgorithm,
                          selectedRoutingAlgorithm,
                          onRoutingAlgorithmChange,
                          onSaveRoutingAlgorithm,
                          loadingRoutingAlgorithm,
                          savingRoutingAlgorithm,
                          routingSettingsMessage,
                        }) {

  const [dispatchHistory, setDispatchHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState("");

  const loadDispatchHistory = async () => {
    try {
      setLoadingHistory(true);
      setHistoryError("");

      const response = await axios.get(
          `${API_URL}/dispatches`
      );

      setDispatchHistory(response.data || []);
    } catch (error) {
      console.error("Failed to load dispatch history:", error);
      setHistoryError("Unable to load dispatch history from the backend.");
    } finally {
      setLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (page === "History") {
      loadDispatchHistory();
    }
  }, [page]);

  const activeEmergencies = emergencies.filter(
      (emergency) =>
          emergency.status === "ACTIVE" ||
          emergency.status === "RESPONDING"
  );

  const completedEmergencies = emergencies.filter(
      (emergency) => emergency.status === "COMPLETED"
  );

  const availableAmbulances = ambulances.filter(
      (ambulance) => ambulance.status === "AVAILABLE"
  );

  const enRouteAmbulances = ambulances.filter(
      (ambulance) => ambulance.status !== "AVAILABLE"
  );

  const latestActiveEmergency =
      activeEmergencies.length > 0
          ? activeEmergencies[0]
          : null;

  // Always display the live ambulance status from the
  // database-backed ambulance list. dispatchPlan contains the
  // initial status returned when the dispatch was created, so it
  // must not be used as the source of truth after the journey moves.
  const currentDispatchAmbulance =
      dispatchPlan
          ? ambulances.find(
              (ambulance) =>
                  ambulance.id ===
                  dispatchPlan.ambulance.id
          )
          : null;

  // tripPhase is the live frontend simulation phase. The backend
  // dispatchPlan.trip.status is only the initial TO_EMERGENCY
  // value and is intentionally not mutated during the animation.
  const currentTripPhase =
      tripPhase ||
      dispatchPlan?.trip?.status ||
      null;

  const pageConfig = {
    Emergencies: {
      title: "Emergencies",
      subtitle: "Monitor active and completed emergency requests",
    },
    Ambulances: {
      title: "Ambulances",
      subtitle: "Monitor ambulance availability and operational status",
    },
    Hospitals: {
      title: "Hospitals",
      subtitle: "Monitor connected hospitals and available capacity",
    },
    Routes: {
      title: "Routes",
      subtitle: routingAlgorithm === "ASTAR" ? "Review the latest A*-optimized route" : "Review the latest Dijkstra-optimized route",
    },
    Dispatch: {
      title: "Dispatch",
      subtitle: "Create and monitor ambulance dispatch operations",
    },
    Analytics: {
      title: "Analytics",
      subtitle: "Operational statistics from the current simulation",
    },
    History: {
      title: "Dispatch History",
      subtitle: "Review completed and in-progress ambulance operations",
    },
    Settings: {
      title: "Settings",
      subtitle: "AmbulanceOS simulation configuration",
    },
  };

  const config = pageConfig[page] || pageConfig.Emergencies;

  const navItems = [
    [Activity, "Operations"],
    [Siren, "Emergencies"],
    [Ambulance, "Ambulances"],
    [Hospital, "Hospitals"],
    [Route, "Routes"],
    [Navigation, "Dispatch"],
    [Activity, "Analytics"],
    [ClipboardList, "History"],
    [Settings, "Settings"],
  ];

  return (
      <div className="min-h-screen bg-slate-950 text-white">

        <header className="h-16 border-b border-slate-800 bg-slate-950 flex items-center justify-between px-6">

          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-red-500 flex items-center justify-center">
              <Ambulance size={21} />
            </div>

            <div>
              <h1 className="font-bold tracking-tight">
                AmbulanceOS
              </h1>
              <p className="text-[10px] text-slate-500 uppercase tracking-widest">
                Emergency Operations
              </p>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="hidden md:flex items-center gap-2 text-xs text-slate-400">
              <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              System Operational
            </div>
            <Bell size={19} className="text-slate-400" />
            <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center">
              <Users size={17} />
            </div>
          </div>

        </header>

        <div className="flex">

          <aside className="hidden md:block w-60 min-h-[calc(100vh-64px)] border-r border-slate-800 bg-slate-950 p-4 relative">

            <div className="space-y-2">
              {navItems.slice(0, 6).map(
                  ([Icon, label]) => (
                      <SidebarItem
                          key={label}
                          icon={Icon}
                          label={label}
                          active={page === label}
                          onClick={() => onNavigate(label)}
                      />
                  )
              )}
            </div>

            <div className="border-t border-slate-800 my-6" />

            <div className="space-y-2">
              {navItems.slice(6).map(
                  ([Icon, label]) => (
                      <SidebarItem
                          key={label}
                          icon={Icon}
                          label={label}
                          active={page === label}
                          onClick={() => onNavigate(label)}
                      />
                  )
              )}
            </div>

            <div className="absolute bottom-5 left-4 w-52 bg-slate-900 border border-slate-800 rounded-xl p-3">
              <div className="flex items-center gap-2">
                <ShieldCheck size={16} className="text-green-400" />
                <span className="text-xs text-slate-300">
                  Simulation Mode
                </span>
              </div>
              <p className="text-[10px] text-slate-500 mt-1">
                Gurgaon emergency network
              </p>
            </div>

          </aside>

          <main className="flex-1 p-5 md:p-6 overflow-auto">

            <div className="mb-6">
              <div className="flex items-center gap-2 text-xs text-slate-500 mb-1">
                <span>Operations</span>
                <span>/</span>
                <span className="text-slate-400">{config.title}</span>
              </div>
              <h2 className="text-2xl font-bold">{config.title}</h2>
              <p className="text-sm text-slate-500 mt-1">
                {config.subtitle}
              </p>
            </div>

            {page === "Emergencies" && (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <StatCard icon={Siren} label="Active" value={String(activeEmergencies.length).padStart(2, "0")} description="ACTIVE + RESPONDING" />
                    <StatCard icon={Clock3} label="Completed" value={String(completedEmergencies.length).padStart(2, "0")} description="Historical emergencies" />
                    <StatCard icon={Activity} label="Total Requests" value={String(emergencies.length).padStart(2, "0")} description="Stored in PostgreSQL" />
                  </div>

                  <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
                    <div className="p-5 border-b border-slate-800">
                      <h3 className="font-semibold">Emergency Requests</h3>
                    </div>
                    {emergencies.length === 0 ? (
                        <div className="p-10 text-center text-slate-500">No emergency requests found.</div>
                    ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead className="text-xs text-slate-500 uppercase bg-slate-950/50">
                            <tr>
                              <th className="text-left px-5 py-3">ID</th>
                              <th className="text-left px-5 py-3">Location</th>
                              <th className="text-left px-5 py-3">Priority</th>
                              <th className="text-left px-5 py-3">Facility</th>
                              <th className="text-left px-5 py-3">Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {emergencies.map((emergency) => (
                                <tr key={emergency.id} className="border-t border-slate-800">
                                  <td className="px-5 py-4 font-medium">EM-{String(emergency.id).padStart(4, "0")}</td>
                                  <td className="px-5 py-4 text-slate-300">{emergency.location}</td>
                                  <td className="px-5 py-4">{emergency.priority}</td>
                                  <td className="px-5 py-4 text-slate-400">{emergency.facility}</td>
                                  <td className="px-5 py-4">
                                <span className={emergency.status === "COMPLETED" ? "text-slate-400" : "text-green-400"}>
                                  {emergency.status}
                                </span>
                                  </td>
                                </tr>
                            ))}
                            </tbody>
                          </table>
                        </div>
                    )}
                  </div>
                </div>
            )}

            {page === "Ambulances" && (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <StatCard icon={Ambulance} label="Total" value={String(ambulances.length).padStart(2, "0")} description="Connected ambulances" />
                    <StatCard icon={Activity} label="Available" value={String(availableAmbulances.length).padStart(2, "0")} description="Ready for dispatch" />
                    <StatCard icon={Navigation} label="In Operation" value={String(enRouteAmbulances.length).padStart(2, "0")} description="Currently unavailable" />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {ambulances.map((ambulance) => (
                        <div key={ambulance.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                          <div className="flex items-start justify-between">
                            <div>
                              <p className="text-xs text-slate-500">AMBULANCE</p>
                              <h3 className="text-lg font-semibold mt-1">{ambulance.ambulanceNumber}</h3>
                            </div>
                            <span className={ambulance.status === "AVAILABLE" ? "text-green-400" : "text-yellow-400"}>
                          {ambulance.status}
                        </span>
                          </div>
                          <div className="grid grid-cols-2 gap-4 mt-5 text-sm">
                            <div><p className="text-xs text-slate-500">Type</p><p className="mt-1">{ambulance.type}</p></div>
                            <div><p className="text-xs text-slate-500">Location</p><p className="mt-1 text-slate-400">{Number(ambulance.latitude).toFixed(4)}, {Number(ambulance.longitude).toFixed(4)}</p></div>
                          </div>
                        </div>
                    ))}
                  </div>
                </div>
            )}

            {page === "Hospitals" && (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <StatCard icon={Hospital} label="Hospitals" value={String(hospitals.length).padStart(2, "0")} description="Connected facilities" />
                    <StatCard icon={Activity} label="Available Beds" value={String(hospitals.reduce((sum, hospital) => sum + Number(hospital.availableBeds || 0), 0))} description="Current simulated capacity" />
                    <StatCard icon={Siren} label="Trauma Facilities" value={String(hospitals.filter((hospital) => hospital.facilityType === "TRAUMA").length).padStart(2, "0")} description="Trauma-capable hospitals" />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {hospitals.map((hospital) => (
                        <div key={hospital.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-slate-500">{hospital.hospitalCode}</span>
                            <Hospital size={19} className="text-green-400" />
                          </div>
                          <h3 className="font-semibold mt-3">{hospital.name}</h3>
                          <p className="text-xs text-slate-500 mt-1">{hospital.facilityType}</p>
                          <div className="mt-5 pt-4 border-t border-slate-800">
                            <p className="text-xs text-slate-500">Available Beds</p>
                            <p className="text-2xl font-bold mt-1">{hospital.availableBeds}</p>
                          </div>
                        </div>
                    ))}
                  </div>
                </div>
            )}

            {page === "Routes" && (
                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-lg font-semibold">Latest Optimized Route</h3>
                      <p className="text-sm text-slate-500 mt-1">{routingAlgorithm === "ASTAR" ? "A* route generated by the backend" : "Traffic-aware Dijkstra route generated by the backend"}</p>
                    </div>
                    <Route size={22} className="text-cyan-400" />
                  </div>

                  {dispatchPlan ? (
                      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6">
                        <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Ambulance</p><p className="font-semibold mt-1">{dispatchPlan.ambulance.ambulanceNumber}</p></div>
                        <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Hospital</p><p className="font-semibold mt-1">{dispatchPlan.hospital.name}</p></div>
                        <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Distance</p><p className="font-semibold mt-1">{Number(dispatchPlan.route.distanceKm).toFixed(2)} km</p></div>
                        <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">ETA</p><p className="font-semibold mt-1">{Number(dispatchPlan.route.estimatedTravelTimeMinutes).toFixed(2)} min</p></div>
                      </div>
                  ) : (
                      <div className="mt-6 bg-slate-950 rounded-xl p-10 text-center text-slate-500">No optimized route has been generated yet.</div>
                  )}
                </div>
            )}

            {page === "Dispatch" && (
                <div className="space-y-5">

                  {errorMessage && (
                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300">
                        {errorMessage}
                      </div>
                  )}

                  {latestActiveEmergency && !dispatchPlan && (
                      <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-5">
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                          <div>
                            <p className="text-xs font-semibold uppercase tracking-wider text-red-400">
                              Active Emergency Ready for Dispatch
                            </p>
                            <h3 className="font-semibold mt-1">
                              EM-{String(latestActiveEmergency.id).padStart(4, "0")} · {latestActiveEmergency.location}
                            </h3>
                            <p className="text-sm text-slate-500 mt-1">
                              {latestActiveEmergency.priority} · {latestActiveEmergency.facility}
                            </p>
                          </div>

                          <button
                              onClick={() => onDispatch(latestActiveEmergency.id)}
                              disabled={loadingDispatch}
                              className="bg-red-500 hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed px-5 py-2.5 rounded-xl font-semibold text-sm"
                          >
                            {loadingDispatch ? "Creating Dispatch..." : "Dispatch Ambulance"}
                          </button>
                        </div>
                      </div>
                  )}

                  {loadingDispatch && (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center">
                        <div className="inline-flex items-center gap-3 text-sm text-slate-300">
                          <Activity size={18} className="text-red-400 animate-pulse" />
                          Calculating ambulance, hospital and traffic-aware route...
                        </div>
                      </div>
                  )}

                  {dispatchPlan ? (
                      <>
                        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                            <div>
                              <p className="text-xs font-semibold uppercase tracking-wider text-red-400">
                                Dispatch Plan Generated
                              </p>
                              <h3 className="text-lg font-semibold mt-1">
                                Emergency #{dispatchPlan.emergencyId}
                              </h3>
                              <p className="text-sm text-slate-500 mt-1">
                                {routingAlgorithm === "ASTAR" ? "A* Search" : "Priority Queue + Traffic-Aware Dijkstra"}
                              </p>
                            </div>

                            <span className={`px-3 py-1.5 rounded-full text-xs font-semibold ${
                                (currentDispatchAmbulance?.status || dispatchPlan.ambulance.status) === "AVAILABLE"
                                    ? "bg-green-500/10 text-green-400"
                                    : "bg-cyan-500/10 text-cyan-400"
                            }`}>
                          {currentDispatchAmbulance?.status || dispatchPlan.ambulance.status}
                        </span>
                          </div>

                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
                            <div className="bg-slate-950 rounded-xl p-4">
                              <p className="text-xs text-slate-500">Selected Ambulance</p>
                              <p className="font-semibold mt-1">{dispatchPlan.ambulance.ambulanceNumber}</p>
                              <p className="text-xs text-slate-500 mt-1">
                                {dispatchPlan.ambulance.type} · {Number(dispatchPlan.ambulance.distanceKm).toFixed(2)} km to emergency
                              </p>
                            </div>

                            <div className="bg-slate-950 rounded-xl p-4">
                              <p className="text-xs text-slate-500">Destination Hospital</p>
                              <p className="font-semibold mt-1">{dispatchPlan.hospital.name}</p>
                              <p className="text-xs text-slate-500 mt-1">
                                {dispatchPlan.hospital.facilityType} · {dispatchPlan.hospital.availableBeds} beds
                              </p>
                            </div>

                            <div className="bg-slate-950 rounded-xl p-4">
                              <p className="text-xs text-slate-500">Ambulance ETA</p>
                              <p className="font-semibold mt-1">
                                {Number(dispatchPlan.ambulance.estimatedTravelTimeMinutes).toFixed(2)} min
                              </p>
                              <p className="text-xs text-slate-500 mt-1">
                                Traffic: {dispatchPlan.ambulance.trafficLevel}
                              </p>
                            </div>
                          </div>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center">
                                <Navigation size={19} className="text-red-400" />
                              </div>
                              <div>
                                <h3 className="font-semibold">Ambulance → Emergency</h3>
                                <p className="text-xs text-slate-500 mt-1">First trip segment</p>
                              </div>
                            </div>

                            <div className="grid grid-cols-3 gap-3 mt-5">
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">Distance</p>
                                <p className="text-sm font-semibold mt-1">{Number(dispatchPlan.trip.routeToEmergency.distanceKm).toFixed(2)} km</p>
                              </div>
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">ETA</p>
                                <p className="text-sm font-semibold mt-1">{Number(dispatchPlan.trip.routeToEmergency.estimatedTravelTimeMinutes).toFixed(2)} min</p>
                              </div>
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">Traffic</p>
                                <p className="text-sm font-semibold mt-1">{dispatchPlan.trip.routeToEmergency.trafficLevel}</p>
                              </div>
                            </div>
                          </div>

                          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center">
                                <Hospital size={19} className="text-green-400" />
                              </div>
                              <div>
                                <h3 className="font-semibold">Emergency → Hospital</h3>
                                <p className="text-xs text-slate-500 mt-1">Second trip segment</p>
                              </div>
                            </div>

                            <div className="grid grid-cols-3 gap-3 mt-5">
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">Distance</p>
                                <p className="text-sm font-semibold mt-1">{Number(dispatchPlan.trip.routeToHospital.distanceKm).toFixed(2)} km</p>
                              </div>
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">ETA</p>
                                <p className="text-sm font-semibold mt-1">{Number(dispatchPlan.trip.routeToHospital.estimatedTravelTimeMinutes).toFixed(2)} min</p>
                              </div>
                              <div className="bg-slate-950 rounded-xl p-3">
                                <p className="text-[10px] text-slate-500">Traffic</p>
                                <p className="text-sm font-semibold mt-1">{dispatchPlan.trip.routeToHospital.trafficLevel}</p>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                            <div>
                              <h3 className="font-semibold">Live Route Simulation</h3>
                              <p className="text-sm text-slate-500 mt-1">
                                The ambulance route is available on the Operations map.
                              </p>
                            </div>

                            <button
                                onClick={() => onNavigate("Operations")}
                                className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-5 py-2.5 text-sm font-semibold text-slate-200 hover:border-red-500/40 hover:text-white"
                            >
                              <MapPin size={17} />
                              Open Live Operations
                            </button>
                          </div>

                          <div className="mt-5 grid grid-cols-1 md:grid-cols-3 gap-3">
                            <div className="rounded-xl bg-slate-950 p-4">
                              <p className="text-xs text-slate-500">Current Phase</p>
                              <p className={`text-sm font-semibold mt-1 ${
                                  currentTripPhase === "COMPLETED"
                                      ? "text-green-400"
                                      : currentTripPhase === "AT_EMERGENCY"
                                          ? "text-yellow-400"
                                          : "text-cyan-400"
                              }`}>
                                {currentTripPhase
                                    ? currentTripPhase.replaceAll("_", " ")
                                    : "—"}
                              </p>
                            </div>
                            <div className="rounded-xl bg-slate-950 p-4">
                              <p className="text-xs text-slate-500">Total Route Nodes</p>
                              <p className="text-sm font-semibold mt-1">
                                {(dispatchPlan.trip.routeToEmergency.nodePath?.length || 0) + (dispatchPlan.trip.routeToHospital.nodePath?.length || 0) - 1}
                              </p>
                            </div>
                            <div className="rounded-xl bg-slate-950 p-4">
                              <p className="text-xs text-slate-500">Routing Engine</p>
                              <p className="text-sm font-semibold mt-1">
                                {routingAlgorithm === "ASTAR"
                                    ? "A* Search"
                                    : "Traffic-Aware Dijkstra"}
                              </p>
                            </div>
                          </div>
                        </div>
                      </>
                  ) : !latestActiveEmergency ? (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-10 text-center text-slate-500">
                        No active emergency is waiting for dispatch.
                      </div>
                  ) : null}
                </div>
            )}

            {page === "History" && (
                <div className="space-y-5">

                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center">
                            <ClipboardList size={20} className="text-red-400" />
                          </div>
                          <div>
                            <h3 className="text-lg font-semibold">Dispatch History</h3>
                            <p className="text-sm text-slate-500 mt-1">
                              Persistent ambulance operations stored in PostgreSQL
                            </p>
                          </div>
                        </div>
                      </div>

                      <button
                          onClick={loadDispatchHistory}
                          disabled={loadingHistory}
                          className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-4 py-2.5 text-sm font-semibold text-slate-200 hover:border-red-500/40 hover:text-white disabled:opacity-50"
                      >
                        <Activity size={17} className={loadingHistory ? "animate-pulse" : ""} />
                        {loadingHistory ? "Refreshing..." : "Refresh"}
                      </button>
                    </div>
                  </div>

                  {historyError && (
                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300">
                        {historyError}
                      </div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <StatCard
                        icon={ClipboardList}
                        label="Total Dispatches"
                        value={String(dispatchHistory.length).padStart(2, "0")}
                        description="Recorded ambulance operations"
                    />
                    <StatCard
                        icon={Activity}
                        label="Completed"
                        value={String(dispatchHistory.filter((dispatch) => dispatch.status === "COMPLETED").length).padStart(2, "0")}
                        description="Successfully completed trips"
                    />
                    <StatCard
                        icon={Navigation}
                        label="In Progress"
                        value={String(dispatchHistory.filter((dispatch) => dispatch.status === "IN_PROGRESS").length).padStart(2, "0")}
                        description="Currently active dispatch records"
                    />
                  </div>

                  {loadingHistory ? (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-10 text-center text-slate-400">
                        Loading dispatch history...
                      </div>
                  ) : dispatchHistory.length === 0 ? (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-10 text-center">
                        <ClipboardList size={34} className="mx-auto text-slate-600" />
                        <p className="text-slate-300 font-medium mt-4">No dispatch history yet</p>
                        <p className="text-sm text-slate-500 mt-1">Create a dispatch to generate the first history record.</p>
                      </div>
                  ) : (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
                        <div className="overflow-x-auto">
                          <table className="w-full text-left text-sm">
                            <thead className="bg-slate-950 text-xs uppercase tracking-wider text-slate-500">
                            <tr>
                              <th className="px-5 py-4">Dispatch</th>
                              <th className="px-5 py-4">Emergency</th>
                              <th className="px-5 py-4">Ambulance</th>
                              <th className="px-5 py-4">Hospital</th>
                              <th className="px-5 py-4">Distance</th>
                              <th className="px-5 py-4">ETA</th>
                              <th className="px-5 py-4">Status</th>
                              <th className="px-5 py-4">Dispatched</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-800">
                            {dispatchHistory.map((dispatch) => (
                                <tr key={dispatch.id} className="hover:bg-slate-800/40">
                                  <td className="px-5 py-4 font-semibold">
                                    #{dispatch.id}
                                  </td>
                                  <td className="px-5 py-4 text-slate-300">
                                    #{dispatch.emergencyId}
                                  </td>
                                  <td className="px-5 py-4">
                                    <div className="font-medium">{dispatch.ambulanceNumber}</div>
                                    <div className="text-xs text-slate-500 mt-1">
                                      ID {dispatch.ambulanceId}
                                    </div>
                                  </td>
                                  <td className="px-5 py-4">
                                    <div className="font-medium">{dispatch.hospitalName}</div>
                                    <div className="text-xs text-slate-500 mt-1">
                                      Hospital ID {dispatch.hospitalId}
                                    </div>
                                  </td>
                                  <td className="px-5 py-4 whitespace-nowrap">
                                    {Number(dispatch.totalDistanceKm || 0).toFixed(2)} km
                                  </td>
                                  <td className="px-5 py-4 whitespace-nowrap">
                                    {Number(dispatch.totalEstimatedTimeMinutes || 0).toFixed(2)} min
                                  </td>
                                  <td className="px-5 py-4">
                                <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${
                                    dispatch.status === "COMPLETED"
                                        ? "bg-green-500/10 text-green-400"
                                        : dispatch.status === "IN_PROGRESS"
                                            ? "bg-cyan-500/10 text-cyan-400"
                                            : "bg-yellow-500/10 text-yellow-400"
                                }`}>
                                  {dispatch.status}
                                </span>
                                  </td>
                                  <td className="px-5 py-4 whitespace-nowrap text-slate-400">
                                    {dispatch.dispatchedAt
                                        ? new Date(dispatch.dispatchedAt).toLocaleString()
                                        : "—"}
                                  </td>
                                </tr>
                            ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                  )}

                  {dispatchHistory.length > 0 && (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                          <div className="bg-slate-950 rounded-xl p-4">
                            <p className="text-xs text-slate-500">Average Dispatch Distance</p>
                            <p className="text-xl font-bold mt-1">
                              {(dispatchHistory.reduce((sum, dispatch) => sum + Number(dispatch.totalDistanceKm || 0), 0) / dispatchHistory.length).toFixed(2)} km
                            </p>
                          </div>
                          <div className="bg-slate-950 rounded-xl p-4">
                            <p className="text-xs text-slate-500">Average Estimated Time</p>
                            <p className="text-xl font-bold mt-1">
                              {(dispatchHistory.reduce((sum, dispatch) => sum + Number(dispatch.totalEstimatedTimeMinutes || 0), 0) / dispatchHistory.length).toFixed(2)} min
                            </p>
                          </div>
                          <div className="bg-slate-950 rounded-xl p-4">
                            <p className="text-xs text-slate-500">Routing Algorithm</p>
                            <p className="text-xl font-bold mt-1">
                              {routingAlgorithm === "ASTAR" ? "A* Search" : "Traffic Dijkstra"}
                            </p>
                          </div>
                        </div>
                      </div>
                  )}
                </div>
            )}

            {page === "Analytics" && (
                <AlgorithmAnalytics />
            )}

            {page === "Settings" && (
                <div className="space-y-5">
                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                    <h3 className="font-semibold">Simulation Configuration</h3>

                    <div className="mt-5 space-y-4">
                      <div className="flex items-center justify-between p-4 bg-slate-950 rounded-xl">
                        <div>
                          <p className="text-sm font-medium">Simulation Mode</p>
                          <p className="text-xs text-slate-500 mt-1">Use simulated traffic and operational capacity</p>
                        </div>
                        <span className="text-green-400 text-xs font-semibold">ENABLED</span>
                      </div>

                      <div className="p-4 bg-slate-950 rounded-xl">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                          <div>
                            <p className="text-sm font-medium">Routing Algorithm</p>
                            <p className="text-xs text-slate-500 mt-1">Choose the graph algorithm used by the backend for route optimization</p>
                          </div>
                          <span className="text-cyan-400 text-xs font-semibold uppercase">
                            Active: {loadingRoutingAlgorithm ? "LOADING" : routingAlgorithm === "ASTAR" ? "A*" : "DIJKSTRA"}
                          </span>
                        </div>

                        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
                          <button
                              type="button"
                              onClick={() => onRoutingAlgorithmChange("DIJKSTRA")}
                              disabled={loadingRoutingAlgorithm || savingRoutingAlgorithm}
                              className={`text-left rounded-xl border p-4 transition-all disabled:cursor-not-allowed ${
                                  selectedRoutingAlgorithm === "DIJKSTRA"
                                      ? "border-cyan-400/60 bg-cyan-400/10"
                                      : "border-slate-800 bg-slate-900 hover:border-slate-700"
                              }`}
                          >
                            <div className="flex items-center justify-between gap-3">
                              <div>
                                <p className="text-sm font-semibold text-white">Dijkstra</p>
                                <p className="text-xs text-slate-500 mt-1">Traffic-aware shortest-path routing</p>
                              </div>
                              <span className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                  selectedRoutingAlgorithm === "DIJKSTRA" ? "border-cyan-400" : "border-slate-600"
                              }`}>
                                {selectedRoutingAlgorithm === "DIJKSTRA" && <span className="w-2 h-2 rounded-full bg-cyan-400" />}
                              </span>
                            </div>
                          </button>

                          <button
                              type="button"
                              onClick={() => onRoutingAlgorithmChange("ASTAR")}
                              disabled={loadingRoutingAlgorithm || savingRoutingAlgorithm}
                              className={`text-left rounded-xl border p-4 transition-all disabled:cursor-not-allowed ${
                                  selectedRoutingAlgorithm === "ASTAR"
                                      ? "border-cyan-400/60 bg-cyan-400/10"
                                      : "border-slate-800 bg-slate-900 hover:border-slate-700"
                              }`}
                          >
                            <div className="flex items-center justify-between gap-3">
                              <div>
                                <p className="text-sm font-semibold text-white">A*</p>
                                <p className="text-xs text-slate-500 mt-1">Heuristic-guided shortest-path routing</p>
                              </div>
                              <span className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                  selectedRoutingAlgorithm === "ASTAR" ? "border-cyan-400" : "border-slate-600"
                              }`}>
                                {selectedRoutingAlgorithm === "ASTAR" && <span className="w-2 h-2 rounded-full bg-cyan-400" />}
                              </span>
                            </div>
                          </button>
                        </div>

                        <div className="mt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                          <div className="text-xs min-h-5">
                            {routingSettingsMessage && (
                                <span className={routingSettingsMessage.type === "success" ? "text-green-400" : "text-red-400"}>
                                  {routingSettingsMessage.text}
                                </span>
                            )}
                          </div>

                          <button
                              type="button"
                              onClick={onSaveRoutingAlgorithm}
                              disabled={
                                  savingRoutingAlgorithm ||
                                  loadingRoutingAlgorithm ||
                                  selectedRoutingAlgorithm === routingAlgorithm
                              }
                              className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-500 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-red-600 disabled:cursor-not-allowed disabled:bg-red-500/40"
                          >
                            <Settings size={16} />
                            {savingRoutingAlgorithm ? "Saving..." : "Apply Algorithm"}
                          </button>
                        </div>
                      </div>

                      <div className="flex items-center justify-between p-4 bg-slate-950 rounded-xl">
                        <div>
                          <p className="text-sm font-medium">Map Data</p>
                          <p className="text-xs text-slate-500 mt-1">Gurgaon OpenStreetMap road network</p>
                        </div>
                        <span className="text-green-400 text-xs font-semibold">CONNECTED</span>
                      </div>
                    </div>
                  </div>
                </div>
            )}

          </main>
        </div>
      </div>
  );
}


// =========================================================
// MAIN APPLICATION
// =========================================================

function App() {

  const [activePage, setActivePage] = useState("Operations");

  // =========================================================
  // ROUTING ALGORITHM STATE
  // =========================================================

  const [routingAlgorithm, setRoutingAlgorithm] = useState("DIJKSTRA");
  const [selectedRoutingAlgorithm, setSelectedRoutingAlgorithm] = useState("DIJKSTRA");
  const [loadingRoutingAlgorithm, setLoadingRoutingAlgorithm] = useState(true);
  const [savingRoutingAlgorithm, setSavingRoutingAlgorithm] = useState(false);
  const [routingSettingsMessage, setRoutingSettingsMessage] = useState(null);


  // =========================================================
  // EMERGENCY MODAL STATE
  // =========================================================

  const [
    showEmergencyModal,
    setShowEmergencyModal,
  ] = useState(false);


  // =========================================================
  // EMERGENCY FORM STATE
  // =========================================================

  const [
    emergencyForm,
    setEmergencyForm,
  ] = useState({
    location: "",
    latitude: "",
    longitude: "",
    priority: "URGENT",
    facility: "GENERAL",
    notes: "",
  });


  // =========================================================
  // EMERGENCY DATA
  // =========================================================

  const [
    emergencies,
    setEmergencies,
  ] = useState([]);


  // =========================================================
  // AMBULANCE DATA
  // =========================================================

  const [
    ambulances,
    setAmbulances,
  ] = useState([]);


  // =========================================================
  // HOSPITAL DATA
  // =========================================================

  const [
    hospitals,
    setHospitals,
  ] = useState([]);


  // =========================================================
  // LOADING STATES
  // =========================================================

  const [
    loadingEmergencies,
    setLoadingEmergencies,
  ] = useState(true);


  const [
    loadingAmbulances,
    setLoadingAmbulances,
  ] = useState(true);


  const [
    loadingHospitals,
    setLoadingHospitals,
  ] = useState(true);


  // =========================================================
  // EMERGENCY SUBMITTING STATE
  // =========================================================

  const [
    submittingEmergency,
    setSubmittingEmergency,
  ] = useState(false);


  // =========================================================
  // DISPATCH STATE
  // =========================================================

  const [
    dispatchPlan,
    setDispatchPlan,
  ] = useState(null);


  // =========================================================
  // ROUTE COORDINATES
  // =========================================================

  const [
    routeCoordinates,
    setRouteCoordinates,
  ] = useState([]);

  // TWO-STAGE TRIP ROUTES
  const [
    routeToEmergencyCoordinates,
    setRouteToEmergencyCoordinates,
  ] = useState([]);

  const [
    routeToHospitalCoordinates,
    setRouteToHospitalCoordinates,
  ] = useState([]);

  // MOVING AMBULANCE
  const [
    movingAmbulancePosition,
    setMovingAmbulancePosition,
  ] = useState(null);

  const [
    tripPhase,
    setTripPhase,
  ] = useState(null);

  const [
    animationRunning,
    setAnimationRunning,
  ] = useState(false);


  // =========================================================
  // DISPATCH LOADING STATE
  // =========================================================

  const [
    loadingDispatch,
    setLoadingDispatch,
  ] = useState(false);


  // =========================================================
  // TRAFFIC DATA
  // =========================================================
  //
  // Contains:
  //
  // sourceNode
  // destinationNode
  // distanceKm
  // estimatedTravelTimeMinutes
  // trafficLevel
  // path
  //
  // =========================================================

  const [
    trafficData,
    setTrafficData,
  ] = useState(null);


  // =========================================================
  // ERROR STATE
  // =========================================================

  const [
    errorMessage,
    setErrorMessage,
  ] = useState("");


  // =========================================================
  // LOAD ROUTING ALGORITHM
  // =========================================================

  const loadRoutingAlgorithm = async () => {

    try {
      setLoadingRoutingAlgorithm(true);
      setRoutingSettingsMessage(null);

      const response = await axios.get(
          `${API_URL}/settings/routing`
      );

      const algorithm = String(
          response.data || "DIJKSTRA"
      ).toUpperCase();

      const normalizedAlgorithm =
          algorithm === "ASTAR" ? "ASTAR" : "DIJKSTRA";

      setRoutingAlgorithm(normalizedAlgorithm);
      setSelectedRoutingAlgorithm(normalizedAlgorithm);

    } catch (error) {
      console.error("Failed to load routing algorithm:", error);
      setRoutingAlgorithm("DIJKSTRA");
      setSelectedRoutingAlgorithm("DIJKSTRA");
      setRoutingSettingsMessage({
        type: "error",
        text: "Unable to load routing settings. Using Dijkstra."
      });
    } finally {
      setLoadingRoutingAlgorithm(false);
    }
  };


  // =========================================================
  // SAVE ROUTING ALGORITHM
  // =========================================================

  const handleSaveRoutingAlgorithm = async () => {

    if (
        selectedRoutingAlgorithm !== "DIJKSTRA" &&
        selectedRoutingAlgorithm !== "ASTAR"
    ) {
      return;
    }

    try {
      setSavingRoutingAlgorithm(true);
      setRoutingSettingsMessage(null);

      const response = await axios.put(
          `${API_URL}/settings/routing`,
          null,
          {
            params: {
              algorithm: selectedRoutingAlgorithm,
            },
          }
      );

      const savedAlgorithm = String(
          response.data || selectedRoutingAlgorithm
      ).toUpperCase();

      const normalizedAlgorithm =
          savedAlgorithm === "ASTAR" ? "ASTAR" : "DIJKSTRA";

      setRoutingAlgorithm(normalizedAlgorithm);
      setSelectedRoutingAlgorithm(normalizedAlgorithm);

      setRoutingSettingsMessage({
        type: "success",
        text: `Routing algorithm changed to ${
            normalizedAlgorithm === "ASTAR" ? "A*" : "Dijkstra"
        }.`
      });

    } catch (error) {
      console.error("Failed to save routing algorithm:", error);
      setRoutingSettingsMessage({
        type: "error",
        text:
            error?.response?.data?.message ||
            "Failed to update routing algorithm."
      });
    } finally {
      setSavingRoutingAlgorithm(false);
    }
  };


  // =========================================================
  // LOAD EMERGENCIES
  // =========================================================

  const loadEmergencies = async () => {

    try {

      setLoadingEmergencies(true);

      const response =
          await axios.get(
              `${API_URL}/emergencies`
          );

      setEmergencies(
          response.data
      );

      console.log(
          "Emergencies loaded:",
          response.data
      );

    } catch (error) {

      console.error(
          "Failed to load emergencies:",
          error
      );

      setErrorMessage(
          "Unable to load emergencies from the backend."
      );

    } finally {

      setLoadingEmergencies(false);

    }
  };


  // =========================================================
  // LOAD AMBULANCES
  // =========================================================

  const loadAmbulances = async () => {

    try {

      setLoadingAmbulances(true);

      const response =
          await axios.get(
              `${API_URL}/ambulances`
          );

      setAmbulances(
          response.data
      );

      console.log(
          "Ambulances loaded:",
          response.data
      );

    } catch (error) {

      console.error(
          "Failed to load ambulances:",
          error
      );

      setErrorMessage(
          "Unable to load ambulances from the backend."
      );

    } finally {

      setLoadingAmbulances(false);

    }
  };


  // =========================================================
  // UPDATE AMBULANCE STATUS WITH RETRY
  // =========================================================
  //
  // Completion is an important state transition. If a single
  // HTTP request is interrupted, retry it before giving up.
  //
  const updateAmbulanceStatusWithRetry = async (
      ambulanceId,
      status
  ) => {

    let lastError = null;

    for (let attempt = 1; attempt <= 3; attempt++) {

      try {

        return await updateAmbulanceStatus(
            ambulanceId,
            status
        );

      } catch (error) {

        lastError = error;

        console.error(
            `Ambulance status update attempt ${attempt} failed:`,
            error
        );

        if (attempt < 3) {
          await new Promise((resolve) =>
              window.setTimeout(resolve, 500)
          );
        }
      }
    }

    throw lastError;
  };


  // =========================================================
  // UPDATE AMBULANCE STATUS
  // =========================================================

  const updateAmbulanceStatus = async (
      ambulanceId,
      status
  ) => {

    try {

      const response = await axios.put(
          `${API_URL}/ambulances/${ambulanceId}/status`,
          null,
          {
            params: {
              status: status,
            },
          }
      );

      setAmbulances((previous) =>
          previous.map((ambulance) =>
              ambulance.id === ambulanceId
                  ? response.data
                  : ambulance
          )
      );

      return response.data;

    } catch (error) {

      console.error(
          "Failed to update ambulance status:",
          error
      );

      throw error;
    }
  };


  // =========================================================
  // UPDATE EMERGENCY STATUS
  // =========================================================

  const updateEmergencyStatus = async (
      emergencyId,
      status
  ) => {

    try {

      const response = await axios.put(
          `${API_URL}/emergencies/${emergencyId}/status`,
          null,
          {
            params: {
              status: status,
            },
          }
      );

      setEmergencies((previous) =>
          previous.map((emergency) =>
              emergency.id === emergencyId
                  ? response.data
                  : emergency
          )
      );

      return response.data;

    } catch (error) {

      console.error(
          "Failed to update emergency status:",
          error
      );

      throw error;
    }
  };


  // =========================================================
  // LOAD HOSPITALS
  // =========================================================

  const loadHospitals = async () => {

    try {

      setLoadingHospitals(true);

      const data =
          await getAllHospitals();

      setHospitals(data);

      console.log(
          "Hospitals loaded:",
          data
      );

    } catch (error) {

      console.error(
          "Failed to load hospitals:",
          error
      );

      setErrorMessage(
          "Unable to load hospitals from the backend."
      );

    } finally {

      setLoadingHospitals(false);

    }
  };


  // =========================================================
  // LOAD BACKEND DATA WHEN APP STARTS
  // =========================================================

  useEffect(() => {

    loadEmergencies();
    loadAmbulances();
    loadHospitals();
    loadRoutingAlgorithm();

  }, []);


  // =========================================================
  // HANDLE EMERGENCY FORM INPUT
  // =========================================================

  const handleEmergencyChange = (event) => {

    const {
      name,
      value,
    } = event.target;

    setEmergencyForm(
        (previous) => ({
          ...previous,
          [name]: value,
        })
    );
  };


  // =========================================================
  // HANDLE LOCATION SELECTION
  // =========================================================

  const handleLocationSelect = ({
                                  lat,
                                  lng,
                                }) => {

    setEmergencyForm(
        (previous) => ({
          ...previous,
          latitude: lat,
          longitude: lng,
        })
    );
  };


  // =========================================================
  // RESET EMERGENCY FORM
  // =========================================================

  const resetEmergencyForm = () => {

    setEmergencyForm({
      location: "",
      latitude: "",
      longitude: "",
      priority: "URGENT",
      facility: "GENERAL",
      notes: "",
    });

  };


  // =========================================================
  // CREATE EMERGENCY
  // =========================================================

  const handleCreateEmergency = async (
      event
  ) => {

    event.preventDefault();


    // -------------------------------------------------------
    // FRONTEND VALIDATION
    // -------------------------------------------------------

    if (
        !emergencyForm.location ||
        !emergencyForm.latitude ||
        !emergencyForm.longitude
    ) {

      setErrorMessage(
          "Please provide the patient location and select a location on the map."
      );

      return;
    }


    try {

      setSubmittingEmergency(true);

      setErrorMessage("");


      // -----------------------------------------------------
      // PREPARE REQUEST
      // -----------------------------------------------------

      const requestData = {

        location:
        emergencyForm.location,

        latitude:
            Number(
                emergencyForm.latitude
            ),

        longitude:
            Number(
                emergencyForm.longitude
            ),

        priority:
        emergencyForm.priority,

        facility:
        emergencyForm.facility,

        notes:
        emergencyForm.notes,
      };


      console.log(
          "Sending emergency request:",
          requestData
      );


      // -----------------------------------------------------
      // SEND POST REQUEST
      // -----------------------------------------------------

      const response =
          await axios.post(
              `${API_URL}/emergencies`,
              requestData
          );


      console.log(
          "Emergency created successfully:",
          response.data
      );


      // -----------------------------------------------------
      // UPDATE FRONTEND STATE
      // -----------------------------------------------------

      setEmergencies(
          (previous) => [
            response.data,
            ...previous,
          ]
      );


      // -----------------------------------------------------
      // RESET OPTIMIZED DISPATCH
      // -----------------------------------------------------

      setDispatchPlan(null);

      setRouteCoordinates([]);

      setTrafficData(null);


      // -----------------------------------------------------
      // CLOSE MODAL
      // -----------------------------------------------------

      setShowEmergencyModal(false);


      // -----------------------------------------------------
      // RESET FORM
      // -----------------------------------------------------

      resetEmergencyForm();

    } catch (error) {

      console.error(
          "Error creating emergency:",
          error
      );

      setErrorMessage(
          error?.response?.data?.message ||
          "Failed to create emergency. Please check the entered details."
      );

    } finally {

      setSubmittingEmergency(false);

    }
  };


  // =========================================================
  // AVAILABLE AMBULANCES
  // =========================================================

  const availableAmbulances =
      ambulances.filter(
          (ambulance) =>
              ambulance.status === "AVAILABLE"
      );


  // =========================================================
  // TOTAL AVAILABLE HOSPITAL BEDS
  // =========================================================

  const totalBeds =
      hospitals.reduce(
          (sum, hospital) =>
              sum +
              Number(
                  hospital.availableBeds || 0
              ),
          0
      );


  // =========================================================
  // ACTIVE EMERGENCY
  // Only ACTIVE/RESPONDING emergencies appear in the
  // Active Emergency panel and can be dispatched.
  // Completed emergencies remain in PostgreSQL history.
  // =========================================================

  const latestEmergency =
      emergencies.find(
          (emergency) =>
              emergency.status === "ACTIVE" ||
              emergency.status === "RESPONDING"
      ) || null;


  // =========================================================
  // CURRENT DISPATCH RECORDS
  // =========================================================

  const currentDispatchAmbulance =
      dispatchPlan
          ? ambulances.find(
              (ambulance) =>
                  ambulance.id ===
                  dispatchPlan.ambulance.id
          )
          : null;

  const currentDispatchEmergency =
      dispatchPlan
          ? emergencies.find(
              (emergency) =>
                  emergency.id ===
                  dispatchPlan.emergencyId &&
                  (emergency.status === "ACTIVE" ||
                      emergency.status === "RESPONDING")
          )
          : latestEmergency;


  // =========================================================
  // CREATE COMPLETE DISPATCH PLAN
  // =========================================================
  //
  // Flow:
  //
  // Emergency
  //     ↓
  // PriorityQueue
  //     ↓
  // Best Ambulance
  //     ↓
  // Hospital Selection
  //     ↓
  // Dijkstra
  //     ↓
  // Traffic Simulation
  //     ↓
  // ETA
  //     ↓
  // Leaflet Route
  //
  // =========================================================

  const handleViewOptimizedRoute =
      async () => {

        if (!latestEmergency) {
          setErrorMessage("No emergency is available.");
          return;
        }

        try {
          setLoadingDispatch(true);
          setErrorMessage("");
          setTrafficData(null);
          setMovingAmbulancePosition(null);
          setTripPhase(null);
          setAnimationRunning(false);

          // Backend now returns both legs:
          // Ambulance -> Emergency
          // Emergency  -> Hospital
          const plan =
              await createDispatchPlan(
                  latestEmergency.id
              );

          console.log(
              "Two-stage dispatch plan:",
              plan
          );

          if (
              !plan.trip?.routeToEmergency?.coordinates ||
              !plan.trip?.routeToHospital?.coordinates
          ) {
            throw new Error(
                "Backend did not return the two-stage trip routes."
            );
          }

          const toEmergency =
              plan.trip.routeToEmergency.coordinates.map(
                  (point) => [
                    Number(point.latitude),
                    Number(point.longitude),
                  ]
              );

          const toHospital =
              plan.trip.routeToHospital.coordinates.map(
                  (point) => [
                    Number(point.latitude),
                    Number(point.longitude),
                  ]
              );

          if (toEmergency.length < 2) {
            throw new Error(
                "Invalid ambulance-to-emergency route."
            );
          }

          if (toHospital.length < 2) {
            throw new Error(
                "Invalid emergency-to-hospital route."
            );
          }

          setDispatchPlan(plan);

          setRouteToEmergencyCoordinates(
              toEmergency
          );

          setRouteToHospitalCoordinates(
              toHospital
          );

          // Combined route is kept for existing UI logic.
          setRouteCoordinates([
            ...toEmergency,
            ...toHospital.slice(1),
          ]);

          // First leg is displayed initially.
          const firstLeg =
              plan.trip.routeToEmergency;

          setTrafficData({
            trafficLevel:
            firstLeg.trafficLevel,
            estimatedTravelTimeMinutes:
            firstLeg.estimatedTravelTimeMinutes,
            distanceKm:
            firstLeg.distanceKm,
          });

          await loadAmbulances();

        } catch (error) {

          console.error(
              "Failed to create dispatch plan:",
              error
          );

          const backendMessage =
              error?.response?.data?.message;

          setErrorMessage(
              backendMessage ||
              error?.message ||
              "Unable to create dispatch plan. Please check ambulance availability and hospital capacity."
          );

        } finally {
          setLoadingDispatch(false);
        }
      };


  // =========================================================
  // LIVE AMBULANCE JOURNEY
  // =========================================================
  //
  // The backend gives us the exact OSM graph coordinates for
  // both trip legs. We animate the ambulance through those
  // coordinates and synchronize operational status changes
  // with the important journey milestones.
  //
  // Phase 1: EN_ROUTE      -> ambulance -> emergency
  // Phase 2: AT_EMERGENCY  -> patient pickup pause
  // Phase 3: TO_HOSPITAL   -> emergency -> hospital
  // Phase 4: COMPLETED     -> arrival + reset ambulance
  //
  // =========================================================

  const completedDispatchRef = useRef(null);

  useEffect(() => {

    if (
        !dispatchPlan?.trip ||
        routeToEmergencyCoordinates.length < 2 ||
        routeToHospitalCoordinates.length < 2
    ) {
      return;
    }

    // A dispatch journey must run only once. Without this guard,
    // changing dispatchPlan after completion can cause React's
    // effect to start the same journey again from the beginning.
    if (completedDispatchRef.current === dispatchPlan.emergencyId) {
      return;
    }

    let cancelled = false;
    const timers = [];

    const sleep = (milliseconds) =>
        new Promise((resolve) => {
          const timer = window.setTimeout(
              resolve,
              milliseconds
          );

          timers.push(timer);
        });


    // ---------------------------------------------------------
    // Smoothly move between two consecutive OSM graph points.
    // ---------------------------------------------------------
    //
    // The graph contains road vertices, so simply jumping from
    // vertex to vertex can look slightly abrupt. We interpolate
    // a few positions between each pair while still following
    // the exact graph route.
    //
    const animateSegment = async (
        coordinates,
        phase,
        routeData
    ) => {

      setTripPhase(phase);
      setAnimationRunning(true);

      setTrafficData({
        trafficLevel:
        routeData.trafficLevel,
        estimatedTravelTimeMinutes:
        routeData.estimatedTravelTimeMinutes,
        distanceKm:
        routeData.distanceKm,
      });

      // Keep the animation quick enough for a dashboard demo
      // while making movement visually smoother.
      const interpolationSteps = 3;
      const frameDelay = 15;

      for (
          let index = 0;
          index < coordinates.length - 1;
          index++
      ) {

        if (cancelled) {
          return;
        }

        const start = coordinates[index];
        const end = coordinates[index + 1];

        for (
            let step = 0;
            step <= interpolationSteps;
            step++
        ) {

          if (cancelled) {
            return;
          }

          const progress =
              step / interpolationSteps;

          const latitude =
              start[0] +
              (end[0] - start[0]) * progress;

          const longitude =
              start[1] +
              (end[1] - start[1]) * progress;

          setMovingAmbulancePosition([
            latitude,
            longitude,
          ]);

          await sleep(frameDelay);
        }
      }

      // Guarantee the final coordinate is exact.
      if (!cancelled) {
        setMovingAmbulancePosition(
            coordinates[coordinates.length - 1]
        );
      }
    };


    const runJourney = async () => {

      // =======================================================
      // PHASE 1 — AMBULANCE TO EMERGENCY
      // =======================================================

      await animateSegment(
          routeToEmergencyCoordinates,
          "TO_EMERGENCY",
          dispatchPlan.trip.routeToEmergency
      );

      if (cancelled) {
        return;
      }


      // =======================================================
      // PHASE 2 — PATIENT PICKUP
      // =======================================================

      setTripPhase("AT_EMERGENCY");

      setMovingAmbulancePosition(
          routeToEmergencyCoordinates[
          routeToEmergencyCoordinates.length - 1
              ]
      );

      // Synchronize backend operational state.
      await updateAmbulanceStatus(
          dispatchPlan.ambulance.id,
          "AT_EMERGENCY"
      );

      await updateEmergencyStatus(
          dispatchPlan.emergencyId,
          "RESPONDING"
      );

      if (cancelled) {
        return;
      }

      // Simulated patient loading / pickup delay.
      await sleep(1200);

      if (cancelled) {
        return;
      }


      // =======================================================
      // PHASE 3 — EMERGENCY TO HOSPITAL
      // =======================================================

      await updateAmbulanceStatus(
          dispatchPlan.ambulance.id,
          "TO_HOSPITAL"
      );

      await animateSegment(
          routeToHospitalCoordinates,
          "TO_HOSPITAL",
          dispatchPlan.trip.routeToHospital
      );

      if (cancelled) {
        return;
      }


      // =======================================================
      // PHASE 4 — HOSPITAL ARRIVAL
      // =======================================================

      setTripPhase("COMPLETED");

      setMovingAmbulancePosition(
          routeToHospitalCoordinates[
          routeToHospitalCoordinates.length - 1
              ]
      );

      // =======================================================
      // 4A. MARK AMBULANCE AVAILABLE
      // =======================================================
      //
      // The ambulance must be released immediately after it
      // reaches the hospital so it can be selected again.
      //
      // We do this independently from emergency completion.
      // If the emergency status request fails, the ambulance
      // should still be released.
      //
      await updateAmbulanceStatusWithRetry(
          dispatchPlan.ambulance.id,
          "AVAILABLE"
      );


      // =======================================================
      // 4B. COMPLETE DISPATCH HISTORY
      // =======================================================
      //
      // The backend created a persistent dispatch record when
      // this journey started. Now that the ambulance has reached
      // the hospital, mark that exact dispatch as COMPLETED.
      //
      if (dispatchPlan.dispatchId) {

        try {

          await completeDispatch(
              dispatchPlan.dispatchId
          );

        } catch (error) {

          // Do not restart or interrupt the map animation if the
          // history request fails. The ambulance has already been
          // released and the journey itself is complete.
          console.error(
              "Failed to complete dispatch history:",
              error
          );
        }
      }


      // =======================================================
      // 4C. MARK EMERGENCY COMPLETED
      // =======================================================

      try {
        await updateEmergencyStatus(
            dispatchPlan.emergencyId,
            "COMPLETED"
        );
      } catch (error) {
        // The ambulance has already been released, so an
        // emergency-status failure must not leave it EN_ROUTE.
        console.error(
            "Emergency completion update failed:",
            error
        );
      }


      // =======================================================
      // 4D. REFRESH DATABASE-BACKED STATE
      // =======================================================

      await loadAmbulances();
      await loadEmergencies();

      // Permanently mark this dispatch journey as completed for
      // this mounted Operations screen. A later render or state
      // refresh cannot restart the ambulance from its base.
      if (!cancelled) {
        completedDispatchRef.current = dispatchPlan.emergencyId;
        setAnimationRunning(false);
      }
    };


    runJourney();


    // ---------------------------------------------------------
    // Cleanup when the route is cleared or a new dispatch starts.
    // ---------------------------------------------------------

    return () => {

      cancelled = true;

      timers.forEach(
          (timer) =>
              window.clearTimeout(timer)
      );
    };

  }, [
    dispatchPlan,
    routeToEmergencyCoordinates,
    routeToHospitalCoordinates,
  ]);


  // =========================================================
  // CLEAR ROUTE
  // =========================================================

  const clearRoute = () => {

    completedDispatchRef.current = null;

    setDispatchPlan(null);

    setRouteCoordinates([]);

    setRouteToEmergencyCoordinates([]);

    setRouteToHospitalCoordinates([]);

    setMovingAmbulancePosition(null);

    setTripPhase(null);

    setAnimationRunning(false);

    setTrafficData(null);

  };


  // =========================================================
  // TRAFFIC LEVEL COLOR
  // =========================================================

  const getTrafficColor = () => {

    if (!trafficData) {
      return "text-slate-400";
    }

    if (
        trafficData.trafficLevel ===
        "HEAVY"
    ) {
      return "text-red-400";
    }

    if (
        trafficData.trafficLevel ===
        "MODERATE"
    ) {
      return "text-yellow-400";
    }

    return "text-green-400";
  };


  // =========================================================
  // RENDER SECONDARY PAGES
  // =========================================================

  if (activePage !== "Operations") {
    return (
        <ManagementPage
            page={activePage}
            onNavigate={setActivePage}
            emergencies={emergencies}
            ambulances={ambulances}
            hospitals={hospitals}
            dispatchPlan={dispatchPlan}
            tripPhase={tripPhase}
            onDispatch={handleViewOptimizedRoute}
            loadingDispatch={loadingDispatch}
            errorMessage={errorMessage}
            routingAlgorithm={routingAlgorithm}
            selectedRoutingAlgorithm={selectedRoutingAlgorithm}
            onRoutingAlgorithmChange={setSelectedRoutingAlgorithm}
            onSaveRoutingAlgorithm={handleSaveRoutingAlgorithm}
            loadingRoutingAlgorithm={loadingRoutingAlgorithm}
            savingRoutingAlgorithm={savingRoutingAlgorithm}
            routingSettingsMessage={routingSettingsMessage}
        />
    );
  }


  // =========================================================
  // RENDER
  // =========================================================

  return (

      <div className="min-h-screen bg-slate-950 text-white">


        {/* =====================================================
          TOP BAR
      ===================================================== */}

        <header className="h-16 border-b border-slate-800 bg-slate-950 flex items-center justify-between px-6">

          <div className="flex items-center gap-3">

            <div className="w-9 h-9 rounded-xl bg-red-500 flex items-center justify-center">

              <Ambulance size={21} />

            </div>


            <div>

              <h1 className="font-bold tracking-tight">
                AmbulanceOS
              </h1>

              <p className="text-[10px] text-slate-500 uppercase tracking-widest">
                Emergency Operations
              </p>

            </div>

          </div>


          <div className="flex items-center gap-6">

            <div className="hidden md:flex items-center gap-2 text-xs text-slate-400">

              <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />

              System Operational

            </div>


            <Bell
                size={19}
                className="text-slate-400 cursor-pointer hover:text-white"
            />


            <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center">

              <Users size={17} />

            </div>

          </div>

        </header>


        {/* =====================================================
          MAIN LAYOUT
      ===================================================== */}

        <div className="flex">


          {/* ===================================================
            SIDEBAR
        =================================================== */}

          <aside className="hidden md:block w-60 min-h-[calc(100vh-64px)] border-r border-slate-800 bg-slate-950 p-4">

            <div className="space-y-2">

              <SidebarItem
                  icon={Activity}
                  label="Operations"
                  active={activePage === "Operations"}
                  onClick={() => setActivePage("Operations")}
              />

              <SidebarItem
                  icon={Siren}
                  label="Emergencies"
                  active={activePage === "Emergencies"}
                  onClick={() => setActivePage("Emergencies")}
              />

              <SidebarItem
                  icon={Ambulance}
                  label="Ambulances"
                  active={activePage === "Ambulances"}
                  onClick={() => setActivePage("Ambulances")}
              />

              <SidebarItem
                  icon={Hospital}
                  label="Hospitals"
                  active={activePage === "Hospitals"}
                  onClick={() => setActivePage("Hospitals")}
              />

              <SidebarItem
                  icon={Route}
                  label="Routes"
                  active={activePage === "Routes"}
                  onClick={() => setActivePage("Routes")}
              />

              <SidebarItem
                  icon={Navigation}
                  label="Dispatch"
                  active={activePage === "Dispatch"}
                  onClick={() => setActivePage("Dispatch")}
              />

            </div>


            <div className="border-t border-slate-800 my-6" />


            <div className="space-y-2">

              <SidebarItem
                  icon={Activity}
                  label="Analytics"
                  active={activePage === "Analytics"}
                  onClick={() => setActivePage("Analytics")}
              />

              <SidebarItem
                  icon={Settings}
                  label="Settings"
                  active={activePage === "Settings"}
                  onClick={() => setActivePage("Settings")}
              />

            </div>


            {/* SIMULATION MODE */}

            <div className="absolute bottom-5 left-4 w-52 bg-slate-900 border border-slate-800 rounded-xl p-3">

              <div className="flex items-center gap-2">

                <ShieldCheck
                    size={16}
                    className="text-green-400"
                />

                <span className="text-xs text-slate-300">
                Simulation Mode
              </span>

              </div>


              <p className="text-[10px] text-slate-500 mt-1">
                Gurgaon emergency network
              </p>

            </div>

          </aside>


          {/* =================================================
            MAIN CONTENT
        ================================================= */}

          <main className="flex-1 p-5 md:p-6 overflow-hidden">


            {/* PAGE HEADER */}

            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-5">

              <div>

                <div className="flex items-center gap-2 text-xs text-slate-500 mb-1">

                <span>
                  Operations
                </span>

                  <span>
                  /
                </span>

                  <span className="text-slate-400">
                  Live Dashboard
                </span>

                </div>


                <h2 className="text-2xl font-bold">
                  Emergency Operations
                </h2>


                <p className="text-sm text-slate-500 mt-1">
                  Real-time simulated ambulance coordination for Gurgaon
                </p>

              </div>


              {/* NEW EMERGENCY */}

              <button
                  onClick={() =>
                      setShowEmergencyModal(true)
                  }
                  className="
                bg-red-500
                hover:bg-red-600
                transition-colors
                px-4
                py-2.5
                rounded-xl
                flex
                items-center
                gap-2
                text-sm
                font-semibold
              "
              >

                <Siren size={17} />

                New Emergency

              </button>

            </div>


            {/* =================================================
              ERROR MESSAGE
          ================================================= */}

            {errorMessage && (

                <div className="mb-5 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl px-4 py-3 text-sm">

                  {errorMessage}

                  <button
                      onClick={() =>
                          setErrorMessage("")
                      }
                      className="ml-3 text-red-300 hover:text-white"
                  >
                    ×
                  </button>

                </div>

            )}


            {/* =================================================
              STATISTICS
          ================================================= */}

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-5">


              {/* ACTIVE EMERGENCIES */}

              <StatCard
                  icon={Siren}
                  label="Active Emergencies"
                  value={
                    loadingEmergencies
                        ? "..."
                        : String(
                            emergencies.filter(
                                (emergency) =>
                                    emergency.status === "ACTIVE" ||
                                    emergency.status === "RESPONDING"
                            ).length
                        ).padStart(2, "0")
                  }
                  description="Stored in PostgreSQL"
              />


              {/* AVAILABLE AMBULANCES */}

              <StatCard
                  icon={Ambulance}
                  label="Available Ambulances"
                  value={
                    loadingAmbulances
                        ? "..."
                        : String(
                            availableAmbulances.length
                        ).padStart(2, "0")
                  }
                  description={
                    loadingAmbulances
                        ? "Loading ambulance network"
                        : `${ambulances.length} total ambulances`
                  }
              />


              {/* AVAILABLE BEDS */}

              <StatCard
                  icon={Hospital}
                  label="Available Beds"
                  value={
                    loadingHospitals
                        ? "..."
                        : totalBeds
                  }
                  description={
                    loadingHospitals
                        ? "Loading hospital network"
                        : `${hospitals.length} hospitals connected`
                  }
              />


              {/* AVG ETA */}

              <StatCard
                  icon={Clock3}
                  label="Avg. ETA"
                  value={
                    trafficData
                        ? `${Number(
                            trafficData.estimatedTravelTimeMinutes
                        ).toFixed(1)} min`
                        : "08 min"
                  }
                  description={
                    trafficData
                        ? `Traffic: ${trafficData.trafficLevel}`
                        : "Current emergency average"
                  }
              />

            </div>


            {/* =================================================
              MAP + RIGHT PANEL
          ================================================= */}

            <div className="grid xl:grid-cols-[1fr_340px] gap-5">


              {/* =================================================
                MAP
            ================================================= */}

              <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">


                {/* MAP HEADER */}

                <div className="h-14 px-4 flex items-center justify-between border-b border-slate-800">

                  <div>

                    <h3 className="font-semibold text-sm">
                      Gurgaon Live Map
                    </h3>

                    <p className="text-[10px] text-slate-500">
                      Ambulance, hospital and optimized route network
                    </p>

                  </div>


                  <div className="flex items-center gap-4 text-[10px] text-slate-400">

                    <div className="flex items-center gap-1.5">

                      <span className="w-2 h-2 rounded-full bg-red-500" />

                      Ambulance

                    </div>


                    <div className="flex items-center gap-1.5">

                      <span className="w-2 h-2 rounded-full bg-green-500" />

                      Hospital

                    </div>


                    <div className="flex items-center gap-1.5">

                      <span className="w-2 h-2 rounded-full bg-yellow-500" />

                      Patient

                    </div>


                    {routeToEmergencyCoordinates.length > 1 && (

                        <>
                          <div className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-sky-400" />
                            Ambulance → Emergency
                          </div>

                          <div className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-violet-400" />
                            Emergency → Hospital
                          </div>
                        </>

                    )}

                  </div>

                </div>


                {/* MAP */}

                <div className="h-[650px]">

                  <MapContainer
                      center={GURGAON_CENTER}
                      zoom={12}
                      scrollWheelZoom={true}
                      className="h-full w-full"
                  >

                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />


                    {/* DATABASE AMBULANCES */}

                    {ambulances
                        .filter(
                            (ambulance) =>
                                !(
                                    dispatchPlan?.ambulance?.id ===
                                    ambulance.id &&
                                    movingAmbulancePosition
                                )
                        )
                        .map(
                            (ambulance) => (

                                <Marker
                                    key={ambulance.id}
                                    position={[
                                      Number(
                                          ambulance.latitude
                                      ),
                                      Number(
                                          ambulance.longitude
                                      ),
                                    ]}
                                    icon={ambulanceIcon}
                                >

                                  <Popup>

                                    <strong>
                                      {ambulance.ambulanceNumber}
                                    </strong>

                                    <br />

                                    Status:{" "}
                                    {ambulance.status}

                                    <br />

                                    Type:{" "}
                                    {ambulance.type}

                                    <br />

                                    Location:{" "}
                                    {Number(
                                        ambulance.latitude
                                    ).toFixed(6)}
                                    ,{" "}
                                    {Number(
                                        ambulance.longitude
                                    ).toFixed(6)}

                                  </Popup>

                                </Marker>

                            )
                        )}


                    {/* MOVING DISPATCHED AMBULANCE */}

                    {movingAmbulancePosition &&
                        dispatchPlan?.ambulance && (

                            <Marker
                                position={movingAmbulancePosition}
                                icon={ambulanceIcon}
                            >

                              <Popup>

                                <strong>
                                  {dispatchPlan.ambulance.ambulanceNumber}
                                </strong>

                                <br />

                                Status:{" "}
                                {tripPhase === "TO_EMERGENCY"
                                    ? "En route to emergency"
                                    : tripPhase === "AT_EMERGENCY"
                                        ? "Patient pickup"
                                        : tripPhase === "TO_HOSPITAL"
                                            ? "En route to hospital"
                                            : "Arrived at hospital"}

                              </Popup>

                            </Marker>


                        )}


                    {/* DATABASE HOSPITALS */}

                    {hospitals.map(
                        (hospital) => (

                            <Marker
                                key={hospital.id}
                                position={[
                                  Number(
                                      hospital.latitude
                                  ),
                                  Number(
                                      hospital.longitude
                                  ),
                                ]}
                                icon={hospitalIcon}
                            >

                              <Popup>

                                <strong>
                                  {hospital.name}
                                </strong>

                                <br />

                                Code:{" "}
                                {hospital.hospitalCode}

                                <br />

                                Facility:{" "}
                                {hospital.facilityType}

                                <br />

                                Available beds:{" "}
                                {hospital.availableBeds}

                              </Popup>

                            </Marker>

                        )
                    )}


                    {/* DATABASE EMERGENCIES */}

                    {emergencies
                        .filter(
                            (emergency) =>
                                emergency.status === "ACTIVE" ||
                                emergency.status === "RESPONDING"
                        )
                        .map(
                            (emergency) => (

                                <Marker
                                    key={
                                      `emergency-${emergency.id}`
                                    }
                                    position={[
                                      Number(
                                          emergency.latitude
                                      ),
                                      Number(
                                          emergency.longitude
                                      ),
                                    ]}
                                    icon={patientIcon}
                                >

                                  <Popup>

                                    <strong>
                                      Emergency #
                                      {emergency.id}
                                    </strong>

                                    <br />

                                    Location:{" "}
                                    {emergency.location}

                                    <br />

                                    Priority:{" "}
                                    {emergency.priority}

                                    <br />

                                    Facility:{" "}
                                    {emergency.facility}

                                    <br />

                                    Status:{" "}
                                    {emergency.status}

                                  </Popup>

                                </Marker>

                            )
                        )}


                    {/* AMBULANCE → EMERGENCY ROUTE */}

                    {routeToEmergencyCoordinates.length > 1 && (

                        <Polyline
                            positions={
                              routeToEmergencyCoordinates
                            }
                            pathOptions={{
                              color: "#38bdf8",
                              weight: 6,
                              opacity: 0.9,
                            }}
                        />

                    )}


                    {/* EMERGENCY → HOSPITAL ROUTE */}

                    {routeToHospitalCoordinates.length > 1 && (

                        <Polyline
                            positions={
                              routeToHospitalCoordinates
                            }
                            pathOptions={{
                              color: "#a78bfa",
                              weight: 6,
                              opacity: 0.9,
                              dashArray: "10 8",
                            }}
                        />

                    )}

                  </MapContainer>

                </div>

              </div>


              {/* =================================================
                RIGHT PANEL
            ================================================= */}

              <div className="space-y-4">


                {/* =================================================
                  ACTIVE EMERGENCY
              ================================================= */}

                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">


                  <div className="flex items-center justify-between mb-4">

                    <div>

                      <h3 className="font-semibold">
                        Active Emergency
                      </h3>

                      <p className="text-xs text-slate-500">

                        {currentDispatchEmergency
                            ? `Request #EM-${String(
                                currentDispatchEmergency.id
                            ).padStart(4, "0")}`
                            : "No active request"}

                      </p>

                    </div>


                    {currentDispatchEmergency && (

                        <span className="px-2.5 py-1 rounded-full bg-red-500/10 text-red-400 text-[10px] font-semibold">

                      {currentDispatchEmergency.priority}

                    </span>

                    )}

                  </div>


                  {currentDispatchEmergency ? (

                      <>

                        {/* LOCATION */}

                        <div className="bg-slate-950 rounded-xl p-3 mb-4">

                          <div className="flex items-start gap-3">

                            <MapPin
                                size={18}
                                className="text-yellow-400 mt-0.5"
                            />

                            <div>

                              <p className="text-sm font-medium">

                                {
                                  currentDispatchEmergency.location
                                }

                              </p>

                              <p className="text-xs text-slate-500">

                                {Number(
                                    currentDispatchEmergency.latitude
                                ).toFixed(6)}

                                ,{" "}

                                {Number(
                                    currentDispatchEmergency.longitude
                                ).toFixed(6)}

                              </p>

                            </div>

                          </div>

                        </div>


                        {/* EMERGENCY DETAILS */}

                        <div className="space-y-3">

                          <div className="flex justify-between">

                        <span className="text-xs text-slate-500">
                          Facility
                        </span>

                            <span className="text-xs font-semibold">

                          {
                            currentDispatchEmergency.facility
                          }

                        </span>

                          </div>


                          <div className="flex justify-between">

                        <span className="text-xs text-slate-500">
                          Status
                        </span>

                            <span className="text-xs font-semibold text-green-400">

                          {
                            currentDispatchEmergency.status
                          }

                        </span>

                          </div>


                          <div className="flex justify-between">

                        <span className="text-xs text-slate-500">
                          Created
                        </span>

                            <span className="text-xs font-semibold">

                          {new Date(
                              currentDispatchEmergency.createdAt
                          ).toLocaleTimeString(
                              [],
                              {
                                hour: "2-digit",
                                minute: "2-digit",
                              }
                          )}

                        </span>

                          </div>

                        </div>


                        {/* DISPATCH BUTTON */}

                        <button
                            onClick={
                              handleViewOptimizedRoute
                            }
                            disabled={
                              loadingDispatch
                            }
                            className="
                        w-full
                        mt-5
                        bg-slate-800
                        hover:bg-slate-700
                        disabled:bg-slate-800/50
                        disabled:cursor-not-allowed
                        transition-colors
                        py-2.5
                        rounded-xl
                        text-xs
                        font-semibold
                        flex
                        items-center
                        justify-center
                        gap-2
                      "
                        >

                          <Route size={15} />

                          {loadingDispatch
                              ? "Optimizing Route..."
                              : "View Optimized Route"}

                        </button>

                      </>

                  ) : (

                      <div className="bg-slate-950 rounded-xl p-5 text-center">

                        <Siren
                            size={24}
                            className="mx-auto text-slate-600 mb-2"
                        />

                        <p className="text-xs text-slate-500">
                          No emergencies found
                        </p>

                      </div>

                  )}

                </div>


                {/* =================================================
                  OPTIMIZED DISPATCH PLAN
              ================================================= */}

                {dispatchPlan && (

                    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">


                      {/* HEADER */}

                      <div className="flex items-center justify-between mb-4">

                        <div>

                          <h3 className="font-semibold text-sm">
                            Optimized Dispatch
                          </h3>

                          <p className="text-[10px] text-slate-500 mt-1">
                            {routingAlgorithm === "ASTAR"
                                ? "A* Search + Traffic-Aware Routing"
                                : "Priority Queue + Traffic-Aware Dijkstra + Traffic"}
                          </p>

                        </div>


                        <span className={`px-2.5 py-1 rounded-full text-[10px] font-semibold ${
                            tripPhase === "COMPLETED"
                                ? "bg-green-500/10 text-green-400"
                                : tripPhase === "AT_EMERGENCY"
                                    ? "bg-yellow-500/10 text-yellow-400"
                                    : "bg-cyan-500/10 text-cyan-400"
                        }`}>
                      {tripPhase === "COMPLETED"
                          ? "COMPLETED"
                          : tripPhase === "AT_EMERGENCY"
                              ? "PATIENT PICKUP"
                              : tripPhase === "TO_HOSPITAL"
                                  ? "TO HOSPITAL"
                                  : "EN ROUTE"}
                    </span>

                      </div>


                      {/* =================================================
                      SELECTED AMBULANCE
                  ================================================= */}

                      <div className="bg-slate-950 rounded-xl p-3 mb-3">

                        <div className="flex items-center gap-3">

                          <div className="w-9 h-9 rounded-lg bg-red-500/10 flex items-center justify-center">

                            <Ambulance
                                size={18}
                                className="text-red-400"
                            />

                          </div>


                          <div className="flex-1">

                            <p className="text-[10px] text-slate-500">
                              Selected Ambulance
                            </p>

                            <p className="text-sm font-semibold">

                              {
                                dispatchPlan
                                    .ambulance
                                    .ambulanceNumber
                              }

                            </p>

                            <p className="text-[10px] text-slate-500">

                              {
                                dispatchPlan
                                    .ambulance
                                    .type
                              }

                              {" • "}

                              {Number(
                                  dispatchPlan
                                      .ambulance
                                      .distanceKm
                              ).toFixed(2)}

                              {" km to emergency"}

                            </p>

                          </div>


                          <span className="text-[10px] text-green-400">

                        {
                            currentDispatchAmbulance
                                ?.status ||
                            dispatchPlan
                                .ambulance
                                .status
                        }

                      </span>

                        </div>

                      </div>


                      {/* =================================================
                      SELECTED HOSPITAL
                  ================================================= */}

                      <div className="bg-slate-950 rounded-xl p-3 mb-3">

                        <div className="flex items-center gap-3">

                          <div className="w-9 h-9 rounded-lg bg-green-500/10 flex items-center justify-center">

                            <Hospital
                                size={18}
                                className="text-green-400"
                            />

                          </div>


                          <div className="flex-1">

                            <p className="text-[10px] text-slate-500">
                              Destination Hospital
                            </p>

                            <p className="text-sm font-semibold">

                              {
                                dispatchPlan
                                    .hospital
                                    .name
                              }

                            </p>

                            <p className="text-[10px] text-slate-500">

                              {
                                dispatchPlan
                                    .hospital
                                    .facilityType
                              }

                              {" • "}

                              {
                                dispatchPlan
                                    .hospital
                                    .availableBeds
                              }

                              {" beds available"}

                            </p>

                          </div>

                        </div>

                      </div>


                      {/* =================================================
                      DIJKSTRA ROUTE
                  ================================================= */}

                      <div className="bg-slate-950 rounded-xl p-3">

                        <div className="flex items-center justify-between mb-3">

                          <span className="text-[10px] text-slate-500">
                            {routingAlgorithm === "ASTAR" ? "A* Route" : "Dijkstra Route"}
                          </span>

                          <span className="text-[10px] font-semibold text-cyan-400">
                            OPTIMIZED
                          </span>

                        </div>


                        <div className="grid grid-cols-2 gap-3 mb-4">

                          <div className="rounded-xl bg-slate-900/80 p-3">

                            <p className="text-[10px] text-slate-500">
                              Route Distance
                            </p>

                            <p className="mt-1 text-lg font-semibold text-cyan-400">
                              {Number(
                                  dispatchPlan.route.distanceKm
                              ).toFixed(2)} km
                            </p>

                          </div>


                          <div className="rounded-xl bg-slate-900/80 p-3">

                            <p className="text-[10px] text-slate-500">
                              Graph Nodes
                            </p>

                            <p className="mt-1 text-lg font-semibold text-white">
                              {dispatchPlan.route.nodes?.length || 0}
                            </p>

                          </div>

                        </div>


                        <div className="space-y-2">

                          <div className="flex items-center justify-between gap-3">

                            <span className="text-[10px] text-slate-500">
                              Source Node
                            </span>

                            <span className="text-[10px] font-mono text-slate-300 text-right break-all">
                              {dispatchPlan.route.sourceNode}
                            </span>

                          </div>


                          <div className="flex items-center justify-between gap-3">

                            <span className="text-[10px] text-slate-500">
                              Destination Node
                            </span>

                            <span className="text-[10px] font-mono text-slate-300 text-right break-all">
                              {dispatchPlan.route.destinationNode}
                            </span>

                          </div>


                          <div className="flex items-center justify-between gap-3">

                            <span className="text-[10px] text-slate-500">
                              Algorithm
                            </span>

                            <span className="text-[10px] font-semibold text-cyan-400 text-right">
                              {routingAlgorithm === "ASTAR"
                                  ? "A* Search"
                                  : "Priority Queue + Traffic-Aware Dijkstra"}
                            </span>

                          </div>

                        </div>

                      </div>



                      {/* =================================================
                      TRAFFIC + ETA
                  ================================================= */}

                      {trafficData && (

                          <div className="bg-slate-950 rounded-xl p-3 mt-3">

                            <div className="flex items-start gap-3">

                              <div className="w-9 h-9 rounded-lg bg-yellow-500/10 flex items-center justify-center">

                                <Clock3
                                    size={18}
                                    className="text-yellow-400"
                                />

                              </div>


                              <div className="flex-1">

                                <div className="flex items-center justify-between">

                                  <div>

                                    <p className="text-[10px] text-slate-500">
                                      Traffic & ETA
                                    </p>

                                    <p className="text-sm font-semibold">
                                      Live Traffic Simulation
                                    </p>

                                  </div>


                                  <span
                                      className={`text-[10px] font-semibold ${getTrafficColor()}`}
                                  >
                              {
                                trafficData
                                    .trafficLevel
                              }
                            </span>

                                </div>


                                {/* ETA */}

                                <div className="flex justify-between mt-3">

                            <span className="text-[10px] text-slate-500">
                              Estimated Arrival
                            </span>

                                  <span className="text-xs font-semibold text-cyan-400">

                              {Number(
                                  trafficData
                                      .estimatedTravelTimeMinutes
                              ).toFixed(2)}

                                    {" min"}

                            </span>

                                </div>


                                {/* TRAFFIC DISTANCE */}

                                <div className="flex justify-between mt-2">

                            <span className="text-[10px] text-slate-500">
                              Traffic Route Distance
                            </span>

                                  <span className="text-xs font-semibold">

                              {Number(
                                  trafficData
                                      .distanceKm
                              ).toFixed(2)}

                                    {" km"}

                            </span>

                                </div>


                                {/* TRAFFIC SOURCE */}

                                <div className="flex justify-between mt-2">

                            <span className="text-[10px] text-slate-500">
                              Traffic Source
                            </span>

                                  <span className="text-[10px] font-semibold">
                              {routingAlgorithm === "ASTAR" ? "Traffic-Aware A*" : "Traffic-Aware Dijkstra"}
                            </span>

                                </div>

                              </div>

                            </div>

                          </div>

                      )}


                      {/* LIVE AMBULANCE JOURNEY */}

                      {dispatchPlan.trip && (

                          <div className="bg-slate-950 rounded-xl p-3 mt-3">

                            <div className="flex items-center justify-between mb-3">

                              <span className="text-[10px] text-slate-500">
                                Ambulance Journey
                              </span>

                              <span className="text-[10px] font-semibold text-cyan-400">
                                {animationRunning
                                    ? "LIVE"
                                    : "COMPLETE"}
                              </span>

                            </div>

                            <div className="space-y-2">

                              <div className={`flex items-center gap-2 text-xs ${
                                  tripPhase === "TO_EMERGENCY"
                                      ? "text-cyan-400"
                                      : "text-slate-400"
                              }`}>
                                <span>🚑</span>
                                <span>En route to emergency</span>
                              </div>

                              <div className={`flex items-center gap-2 text-xs ${
                                  tripPhase === "AT_EMERGENCY"
                                      ? "text-yellow-400"
                                      : "text-slate-500"
                              }`}>
                                <span>📍</span>
                                <span>Patient picked up</span>
                              </div>

                              <div className={`flex items-center gap-2 text-xs ${
                                  tripPhase === "TO_HOSPITAL"
                                      ? "text-cyan-400"
                                      : "text-slate-500"
                              }`}>
                                <span>🚑</span>
                                <span>En route to hospital</span>
                              </div>

                              <div className={`flex items-center gap-2 text-xs ${
                                  tripPhase === "COMPLETED"
                                      ? "text-green-400"
                                      : "text-slate-500"
                              }`}>
                                <span>🏥</span>
                                <span>Arrived at hospital</span>
                              </div>

                            </div>

                          </div>

                      )}


                      {/* CLEAR ROUTE */}

                      <button
                          onClick={
                            clearRoute
                          }
                          className="
                      w-full
                      mt-4
                      bg-slate-800
                      hover:bg-slate-700
                      transition-colors
                      py-2
                      rounded-xl
                      text-[11px]
                      font-semibold
                      text-slate-400
                    "
                      >

                        Clear Optimized Route

                      </button>

                    </div>

                )}


                {/* =================================================
                  NETWORK STATUS
              ================================================= */}

                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">

                  <h3 className="font-semibold text-sm mb-4">
                    Network Status
                  </h3>


                  <div className="space-y-3">


                    {/* AMBULANCE NETWORK */}

                    <div className="flex items-center justify-between">

                    <span className="text-xs text-slate-400">
                      Ambulance Network
                    </span>

                      <span className="text-xs text-green-400">

                      {loadingAmbulances
                          ? "Loading"
                          : "Operational"}

                    </span>

                    </div>


                    {/* HOSPITAL CAPACITY */}

                    <div className="flex items-center justify-between">

                    <span className="text-xs text-slate-400">
                      Hospital Capacity
                    </span>

                      <span className="text-xs text-green-400">

                      {loadingHospitals
                          ? "Loading"
                          : "Updated"}

                    </span>

                    </div>


                    {/* ROUTE ENGINE */}

                    <div className="flex items-center justify-between">

                    <span className="text-xs text-slate-400">
                      Route Engine
                    </span>

                      <span className="text-xs text-green-400">

                      {dispatchPlan
                          ? routingAlgorithm === "ASTAR"
                              ? "A* Active"
                              : "Dijkstra Active"
                          : "Ready"}

                    </span>

                    </div>


                    {/* TRAFFIC */}

                    <div className="flex items-center justify-between">

                    <span className="text-xs text-slate-400">
                      Traffic Simulation
                    </span>

                      <span
                          className={`text-xs ${
                              trafficData
                                  ? getTrafficColor()
                                  : "text-yellow-400"
                          }`}
                      >

                      {trafficData
                          ? trafficData.trafficLevel
                          : "Ready"}

                    </span>

                    </div>


                    {/* ETA */}

                    <div className="flex items-center justify-between">

                    <span className="text-xs text-slate-400">
                      Estimated ETA
                    </span>

                      <span className="text-xs text-cyan-400">

                      {trafficData
                          ? `${Number(
                              trafficData.estimatedTravelTimeMinutes
                          ).toFixed(2)} min`
                          : "Waiting"}

                    </span>

                    </div>

                  </div>

                </div>

              </div>

            </div>

          </main>

        </div>


        {/* =========================================================
          CREATE EMERGENCY MODAL
      ========================================================= */}

        {showEmergencyModal && (

            <div className="
          fixed
          inset-0
          z-[2000]
          bg-black/70
          backdrop-blur-sm
          flex
          items-center
          justify-center
          p-4
        ">


              <div className="
            w-full
            max-w-2xl
            max-h-[90vh]
            overflow-y-auto
            bg-slate-900
            border
            border-slate-700
            rounded-2xl
            shadow-2xl
          ">


                {/* MODAL HEADER */}

                <div className="
              flex
              items-center
              justify-between
              px-6
              py-5
              border-b
              border-slate-800
            ">

                  <div className="flex items-center gap-3">

                    <div className="
                  w-9
                  h-9
                  rounded-xl
                  bg-red-500/15
                  flex
                  items-center
                  justify-center
                ">

                      <Siren
                          size={19}
                          className="text-red-400"
                      />

                    </div>


                    <div>

                      <h2 className="font-bold">
                        Create Emergency
                      </h2>

                      <p className="text-xs text-slate-500">
                        Dispatch a simulated emergency request
                      </p>

                    </div>

                  </div>


                  <button
                      type="button"
                      onClick={() =>
                          setShowEmergencyModal(false)
                      }
                      className="text-slate-500 hover:text-white text-xl"
                  >
                    ×
                  </button>

                </div>


                {/* FORM */}

                <form
                    onSubmit={
                      handleCreateEmergency
                    }
                    className="p-6 space-y-5"
                >


                  {/* PATIENT LOCATION */}

                  <div>

                    <label className="block text-xs font-medium text-slate-400 mb-2">
                      Patient Location
                    </label>

                    <input
                        type="text"
                        name="location"
                        value={
                          emergencyForm.location
                        }
                        onChange={
                          handleEmergencyChange
                        }
                        placeholder="e.g. Sector 15, Gurgaon"
                        required
                        className="
                    w-full
                    bg-slate-950
                    border
                    border-slate-800
                    focus:border-red-500
                    outline-none
                    rounded-xl
                    px-4
                    py-3
                    text-sm
                    text-white
                    placeholder:text-slate-600
                  "
                    />

                  </div>


                  {/* LOCATION PICKER */}

                  <div>

                    <LocationPicker
                        onLocationSelect={
                          handleLocationSelect
                        }
                    />

                  </div>


                  {/* SELECTED COORDINATES */}

                  {emergencyForm.latitude &&
                      emergencyForm.longitude && (

                          <div className="
                    rounded-xl
                    border
                    border-slate-800
                    bg-slate-950
                    p-3
                  ">

                            <p className="text-[10px] text-slate-500 mb-2">
                              Selected coordinates
                            </p>


                            <div className="grid grid-cols-2 gap-3">

                              <div>

                                <p className="text-[10px] text-slate-500">
                                  Latitude
                                </p>

                                <p className="text-xs font-mono text-white">

                                  {Number(
                                      emergencyForm.latitude
                                  ).toFixed(6)}

                                </p>

                              </div>


                              <div>

                                <p className="text-[10px] text-slate-500">
                                  Longitude
                                </p>

                                <p className="text-xs font-mono text-white">

                                  {Number(
                                      emergencyForm.longitude
                                  ).toFixed(6)}

                                </p>

                              </div>

                            </div>

                          </div>

                      )}


                  {/* PRIORITY */}

                  <div>

                    <label className="block text-xs font-medium text-slate-400 mb-2">
                      Emergency Priority
                    </label>


                    <div className="grid grid-cols-3 gap-2">

                      {[
                        "CRITICAL",
                        "URGENT",
                        "STANDARD",
                      ].map(
                          (priority) => (

                              <label
                                  key={priority}
                                  className={`
                          cursor-pointer
                          rounded-xl
                          border
                          px-3
                          py-3
                          text-center
                          transition-all
                          ${
                                      emergencyForm.priority ===
                                      priority
                                          ? "border-red-500 bg-red-500/10 text-red-400"
                                          : "border-slate-800 bg-slate-950 text-slate-500 hover:border-slate-700"
                                  }
                        `}
                              >

                                <input
                                    type="radio"
                                    name="priority"
                                    value={priority}
                                    checked={
                                        emergencyForm.priority ===
                                        priority
                                    }
                                    onChange={
                                      handleEmergencyChange
                                    }
                                    className="hidden"
                                />


                                <span className="text-xs font-semibold">
                          {priority}
                        </span>

                              </label>

                          )
                      )}

                    </div>

                  </div>


                  {/* FACILITY */}

                  <div>

                    <label className="block text-xs font-medium text-slate-400 mb-2">
                      Required Facility
                    </label>


                    <select
                        name="facility"
                        value={
                          emergencyForm.facility
                        }
                        onChange={
                          handleEmergencyChange
                        }
                        className="
                    w-full
                    bg-slate-950
                    border
                    border-slate-800
                    focus:border-red-500
                    outline-none
                    rounded-xl
                    px-4
                    py-3
                    text-sm
                    text-white
                  "
                    >

                      <option value="GENERAL">
                        General Emergency
                      </option>

                      <option value="ICU">
                        ICU
                      </option>

                      <option value="TRAUMA">
                        Trauma Center
                      </option>

                      <option value="CARDIAC">
                        Cardiac Care
                      </option>

                    </select>

                  </div>


                  {/* NOTES */}

                  <div>

                    <label className="block text-xs font-medium text-slate-400 mb-2">
                      Additional Information
                    </label>


                    <textarea
                        name="notes"
                        value={
                          emergencyForm.notes
                        }
                        onChange={
                          handleEmergencyChange
                        }
                        rows="3"
                        placeholder="Describe the emergency..."
                        className="
                    w-full
                    bg-slate-950
                    border
                    border-slate-800
                    focus:border-red-500
                    outline-none
                    rounded-xl
                    px-4
                    py-3
                    text-sm
                    text-white
                    placeholder:text-slate-600
                    resize-none
                  "
                    />

                  </div>


                  {/* BUTTONS */}

                  <div className="flex gap-3 pt-2">


                    {/* CANCEL */}

                    <button
                        type="button"
                        onClick={() =>
                            setShowEmergencyModal(
                                false
                            )
                        }
                        className="
                    flex-1
                    bg-slate-800
                    hover:bg-slate-700
                    text-slate-300
                    py-3
                    rounded-xl
                    text-sm
                    font-semibold
                    transition-colors
                  "
                    >
                      Cancel
                    </button>


                    {/* CREATE */}

                    <button
                        type="submit"
                        disabled={
                          submittingEmergency
                        }
                        className="
                    flex-1
                    bg-red-500
                    hover:bg-red-600
                    disabled:bg-red-500/50
                    disabled:cursor-not-allowed
                    py-3
                    rounded-xl
                    text-sm
                    font-semibold
                    transition-colors
                    flex
                    items-center
                    justify-center
                    gap-2
                  "
                    >

                      <Siren size={17} />

                      {submittingEmergency
                          ? "Creating..."
                          : "Create Emergency"}

                    </button>

                  </div>

                </form>

              </div>

            </div>

        )}

      </div>
  );
}


export default App;
