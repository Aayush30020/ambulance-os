import { useEffect, useState } from "react";
import axios from "axios";

import {
  Activity,
  Ambulance,
  Bell,
  Clock3,
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

import {
  createDispatchPlan,
} from "./services/dispatchPlanService";


// =========================================================
// API CONFIGURATION
// =========================================================

const API_URL = "http://localhost:8080/api";


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
                          onDispatch,
                        }) {

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
      subtitle: "Review the latest Dijkstra-optimized route",
    },
    Dispatch: {
      title: "Dispatch",
      subtitle: "Create and monitor ambulance dispatch operations",
    },
    Analytics: {
      title: "Analytics",
      subtitle: "Operational statistics from the current simulation",
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
                      <p className="text-sm text-slate-500 mt-1">Traffic-aware Dijkstra route generated by the backend</p>
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
                  {latestActiveEmergency && !dispatchPlan && (
                      <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-5">
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                          <div>
                            <p className="text-xs text-red-400">ACTIVE EMERGENCY</p>
                            <h3 className="font-semibold mt-1">EM-{String(latestActiveEmergency.id).padStart(4, "0")} · {latestActiveEmergency.location}</h3>
                            <p className="text-sm text-slate-500 mt-1">{latestActiveEmergency.priority} · {latestActiveEmergency.facility}</p>
                          </div>
                          <button onClick={() => onDispatch(latestActiveEmergency.id)} className="bg-red-500 hover:bg-red-600 px-5 py-2.5 rounded-xl font-semibold text-sm">
                            Dispatch Ambulance
                          </button>
                        </div>
                      </div>
                  )}

                  {dispatchPlan ? (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                        <div className="flex items-center justify-between">
                          <div><h3 className="text-lg font-semibold">Current Dispatch</h3><p className="text-sm text-slate-500 mt-1">Priority Queue + Traffic-Aware Dijkstra</p></div>
                          <span className="text-green-400 text-xs font-semibold">{dispatchPlan.ambulance.status}</span>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
                          <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Ambulance</p><p className="font-semibold mt-1">{dispatchPlan.ambulance.ambulanceNumber}</p><p className="text-xs text-slate-500 mt-1">{dispatchPlan.ambulance.estimatedTravelTimeMinutes.toFixed(2)} min to emergency</p></div>
                          <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Hospital</p><p className="font-semibold mt-1">{dispatchPlan.hospital.name}</p><p className="text-xs text-slate-500 mt-1">{dispatchPlan.hospital.availableBeds} beds</p></div>
                          <div className="bg-slate-950 rounded-xl p-4"><p className="text-xs text-slate-500">Traffic</p><p className="font-semibold mt-1">{dispatchPlan.ambulance.trafficLevel}</p><p className="text-xs text-slate-500 mt-1">{dispatchPlan.ambulance.estimatedTravelTimeMinutes.toFixed(2)} min ETA</p></div>
                        </div>
                      </div>
                  ) : !latestActiveEmergency ? (
                      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-10 text-center text-slate-500">No active emergency is waiting for dispatch.</div>
                  ) : null}
                </div>
            )}

            {page === "Analytics" && (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard icon={Siren} label="Total Emergencies" value={String(emergencies.length)} description="All database records" />
                    <StatCard icon={Ambulance} label="Dispatch Ready" value={String(availableAmbulances.length)} description="Available ambulances" />
                    <StatCard icon={Hospital} label="Hospital Beds" value={String(hospitals.reduce((sum, hospital) => sum + Number(hospital.availableBeds || 0), 0))} description="Simulated capacity" />
                    <StatCard icon={Route} label="Completed" value={String(completedEmergencies.length)} description="Completed emergencies" />
                  </div>

                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                    <h3 className="font-semibold">Operational Overview</h3>
                    <div className="space-y-4 mt-5">
                      <div className="flex justify-between text-sm"><span className="text-slate-400">Emergency completion rate</span><span>{emergencies.length ? Math.round((completedEmergencies.length / emergencies.length) * 100) : 0}%</span></div>
                      <div className="h-2 bg-slate-800 rounded-full overflow-hidden"><div className="h-full bg-green-500" style={{ width: `${emergencies.length ? (completedEmergencies.length / emergencies.length) * 100 : 0}%` }} /></div>
                      <div className="flex justify-between text-sm"><span className="text-slate-400">Ambulance availability</span><span>{ambulances.length ? Math.round((availableAmbulances.length / ambulances.length) * 100) : 0}%</span></div>
                      <div className="h-2 bg-slate-800 rounded-full overflow-hidden"><div className="h-full bg-cyan-500" style={{ width: `${ambulances.length ? (availableAmbulances.length / ambulances.length) * 100 : 0}%` }} /></div>
                    </div>
                  </div>
                </div>
            )}

            {page === "Settings" && (
                <div className="space-y-5">
                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
                    <h3 className="font-semibold">Simulation Configuration</h3>
                    <div className="mt-5 space-y-4">
                      <div className="flex items-center justify-between p-4 bg-slate-950 rounded-xl"><div><p className="text-sm font-medium">Simulation Mode</p><p className="text-xs text-slate-500 mt-1">Use simulated traffic and operational capacity</p></div><span className="text-green-400 text-xs font-semibold">ENABLED</span></div>
                      <div className="flex items-center justify-between p-4 bg-slate-950 rounded-xl"><div><p className="text-sm font-medium">Routing Algorithm</p><p className="text-xs text-slate-500 mt-1">Backend graph-based route optimization</p></div><span className="text-cyan-400 text-xs font-semibold">DIJKSTRA</span></div>
                      <div className="flex items-center justify-between p-4 bg-slate-950 rounded-xl"><div><p className="text-sm font-medium">Map Data</p><p className="text-xs text-slate-500 mt-1">Gurgaon OpenStreetMap road network</p></div><span className="text-green-400 text-xs font-semibold">CONNECTED</span></div>
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
  // ANIMATE AMBULANCE ALONG OSM GRAPH ROUTE
  // =========================================================

  useEffect(() => {

    if (
        !dispatchPlan?.trip ||
        routeToEmergencyCoordinates.length < 2 ||
        routeToHospitalCoordinates.length < 2
    ) {
      return;
    }

    let cancelled = false;
    const timers = [];

    const sleep = (ms) =>
        new Promise((resolve) => {
          const timer =
              window.setTimeout(resolve, ms);
          timers.push(timer);
        });

    const animateSegment =
        async (coordinates, phase, routeData) => {

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

          for (
              let index = 0;
              index < coordinates.length;
              index++
          ) {

            if (cancelled) {
              return;
            }

            setMovingAmbulancePosition(
                coordinates[index]
            );

            await sleep(45);
          }
        };

    const runJourney = async () => {

      await animateSegment(
          routeToEmergencyCoordinates,
          "TO_EMERGENCY",
          dispatchPlan.trip.routeToEmergency
      );

      if (cancelled) {
        return;
      }

      // Patient pickup pause.
      setTripPhase("AT_EMERGENCY");

      setMovingAmbulancePosition(
          routeToEmergencyCoordinates[
          routeToEmergencyCoordinates.length - 1
              ]
      );

      await updateAmbulanceStatus(
          dispatchPlan.ambulance.id,
          "AT_EMERGENCY"
      );

      await updateEmergencyStatus(
          dispatchPlan.emergencyId,
          "RESPONDING"
      );

      await sleep(1200);

      if (cancelled) {
        return;
      }

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

      setTripPhase("COMPLETED");

      setMovingAmbulancePosition(
          routeToHospitalCoordinates[
          routeToHospitalCoordinates.length - 1
              ]
      );

      await updateEmergencyStatus(
          dispatchPlan.emergencyId,
          "COMPLETED"
      );

      await updateAmbulanceStatus(
          dispatchPlan.ambulance.id,
          "AVAILABLE"
      );

      await loadAmbulances();
      await loadEmergencies();

      setAnimationRunning(false);
    };

    runJourney();

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
            onDispatch={handleViewOptimizedRoute}
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
                            Priority Queue + Traffic-Aware Dijkstra + Traffic
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
                            Dijkstra Route
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
                              Priority Queue + Traffic-Aware Dijkstra
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
                              Traffic-Aware Dijkstra
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
                          ? "Dijkstra Active"
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