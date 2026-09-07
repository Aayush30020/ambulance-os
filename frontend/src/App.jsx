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
  getAmbulanceHospitalRoute,
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
                     }) {
  return (
      <div
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
// MAIN APPLICATION
// =========================================================

function App() {

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
  //
  // These coordinates are passed to Leaflet Polyline.
  //
  // Example:
  //
  // [
  //   [28.4487, 77.0384],
  //   [28.4520, 77.0480],
  //   [28.4246, 76.9957]
  // ]
  //
  // =========================================================

  const [
    routeCoordinates,
    setRouteCoordinates,
  ] = useState([]);


  // =========================================================
  // DISPATCH LOADING STATE
  // =========================================================

  const [
    loadingDispatch,
    setLoadingDispatch,
  ] = useState(false);


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

      const response = await axios.get(
          `${API_URL}/emergencies`
      );

      setEmergencies(response.data);

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

      const response = await axios.get(
          `${API_URL}/ambulances`
      );

      setAmbulances(response.data);

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
      // MAKE NEW EMERGENCY THE CURRENT DISPATCH TARGET
      // -----------------------------------------------------

      setDispatchPlan(null);

      setRouteCoordinates([]);


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
  // LATEST EMERGENCY
  // =========================================================

  const latestEmergency =
      emergencies.length > 0
          ? emergencies[0]
          : null;


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
  // Leaflet Route
  //
  // =========================================================

  const handleViewOptimizedRoute =
      async () => {

        if (!latestEmergency) {

          setErrorMessage(
              "No emergency is available."
          );

          return;
        }


        try {

          setLoadingDispatch(true);

          setErrorMessage("");


          // -----------------------------------------------------
          // STEP 1
          // CREATE COMPLETE DISPATCH PLAN
          // -----------------------------------------------------

          console.log(
              "Creating dispatch plan for emergency:",
              latestEmergency.id
          );


          const plan =
              await createDispatchPlan(
                  latestEmergency.id
              );


          console.log(
              "Dispatch plan received:",
              plan
          );


          // -----------------------------------------------------
          // STEP 2
          // STORE DISPATCH PLAN
          // -----------------------------------------------------

          setDispatchPlan(plan);


          // -----------------------------------------------------
          // STEP 3
          // GET ROUTE COORDINATES
          // -----------------------------------------------------
          //
          // DispatchPlan gives us:
          //
          // ambulance.id
          // hospital.id
          //
          // Route API gives:
          //
          // latitude
          // longitude
          //
          // -----------------------------------------------------

          const route =
              await getAmbulanceHospitalRoute(
                  plan.ambulance.id,
                  plan.hospital.id
              );


          console.log(
              "Optimized route received:",
              route
          );


          // -----------------------------------------------------
          // STEP 4
          // CONVERT ROUTE TO LEAFLET FORMAT
          // -----------------------------------------------------

          const coordinates =
              route.route.map(
                  (point) => [
                    Number(point.latitude),
                    Number(point.longitude),
                  ]
              );


          setRouteCoordinates(
              coordinates
          );


          // -----------------------------------------------------
          // STEP 5
          // REFRESH AMBULANCES
          //
          // Selected ambulance:
          //
          // AVAILABLE → EN_ROUTE
          //
          // -----------------------------------------------------

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
              "Unable to create dispatch plan. Please check ambulance availability and hospital capacity."
          );

        } finally {

          setLoadingDispatch(false);

        }
      };


  // =========================================================
  // CLEAR ROUTE
  // =========================================================

  const clearRoute = () => {

    setDispatchPlan(null);

    setRouteCoordinates([]);

  };


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
                  active
              />

              <SidebarItem
                  icon={Siren}
                  label="Emergencies"
              />

              <SidebarItem
                  icon={Ambulance}
                  label="Ambulances"
              />

              <SidebarItem
                  icon={Hospital}
                  label="Hospitals"
              />

              <SidebarItem
                  icon={Route}
                  label="Routes"
              />

              <SidebarItem
                  icon={Navigation}
                  label="Dispatch"
              />

            </div>


            <div className="border-t border-slate-800 my-6" />


            <div className="space-y-2">

              <SidebarItem
                  icon={Activity}
                  label="Analytics"
              />

              <SidebarItem
                  icon={Settings}
                  label="Settings"
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


          {/* ===================================================
            MAIN CONTENT
        =================================================== */}

          <main className="flex-1 p-5 md:p-6 overflow-hidden">


            {/* =================================================
              PAGE HEADER
          ================================================= */}

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
                                    emergency.status ===
                                    "ACTIVE"
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
                  value="08 min"
                  description="Current emergency average"
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


                    {routeCoordinates.length > 1 && (

                        <div className="flex items-center gap-1.5">

                          <span className="w-2 h-2 rounded-full bg-sky-400" />

                          Dijkstra Route

                        </div>

                    )}

                  </div>

                </div>


                {/* MAP */}

                <div className="h-[560px]">

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


                    {/* =================================================
                      DATABASE AMBULANCES
                  ================================================= */}

                    {ambulances.map(
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


                    {/* =================================================
                      DATABASE HOSPITALS
                  ================================================= */}

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


                    {/* =================================================
                      DATABASE EMERGENCIES
                  ================================================= */}

                    {emergencies.map(
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


                    {/* =================================================
                      OPTIMIZED DIJKSTRA ROUTE
                  ================================================= */}

                    {routeCoordinates.length > 1 && (

                        <Polyline
                            positions={
                              routeCoordinates
                            }
                            pathOptions={{
                              color: "#38bdf8",
                              weight: 6,
                              opacity: 0.9,
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

                        {latestEmergency
                            ? `Request #EM-${String(
                                latestEmergency.id
                            ).padStart(4, "0")}`
                            : "No active request"}

                      </p>

                    </div>


                    {latestEmergency && (

                        <span className="px-2.5 py-1 rounded-full bg-red-500/10 text-red-400 text-[10px] font-semibold">

                      {latestEmergency.priority}

                    </span>

                    )}

                  </div>


                  {latestEmergency ? (

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
                                  latestEmergency.location
                                }

                              </p>

                              <p className="text-xs text-slate-500">

                                {Number(
                                    latestEmergency.latitude
                                ).toFixed(6)}

                                ,{" "}

                                {Number(
                                    latestEmergency.longitude
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
                            latestEmergency.facility
                          }
                        </span>

                          </div>


                          <div className="flex justify-between">

                        <span className="text-xs text-slate-500">
                          Status
                        </span>

                            <span className="text-xs font-semibold text-green-400">
                          {
                            latestEmergency.status
                          }
                        </span>

                          </div>


                          <div className="flex justify-between">

                        <span className="text-xs text-slate-500">
                          Created
                        </span>

                            <span className="text-xs font-semibold">

                          {new Date(
                              latestEmergency.createdAt
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
                            Priority Queue + Dijkstra
                          </p>

                        </div>


                        <span className="px-2.5 py-1 rounded-full bg-green-500/10 text-green-400 text-[10px] font-semibold">
                      ACTIVE
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

                              {
                                Number(
                                    dispatchPlan
                                        .ambulance
                                        .distanceKm
                                ).toFixed(2)
                              }

                              {" km to emergency"}

                            </p>

                          </div>


                          <span className="text-[10px] text-green-400">

                        {
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

                        <div className="flex items-start gap-3">

                          <Route
                              size={18}
                              className="text-sky-400 mt-0.5"
                          />


                          <div className="flex-1">

                            <p className="text-[10px] text-slate-500">
                              Dijkstra Route
                            </p>


                            <p className="text-sm font-semibold text-sky-400 mt-1 leading-relaxed">

                              {
                                dispatchPlan
                                    .route
                                    .nodes
                                    .join(" → ")
                              }

                            </p>


                            <div className="flex justify-between mt-3">

                          <span className="text-[10px] text-slate-500">
                            Route Distance
                          </span>

                              <span className="text-xs font-semibold">

                            {
                              Number(
                                  dispatchPlan
                                      .route
                                      .distanceKm
                              ).toFixed(2)
                            }

                                {" km"}

                          </span>

                            </div>


                            <div className="flex justify-between mt-2">

                          <span className="text-[10px] text-slate-500">
                            Source
                          </span>

                              <span className="text-[10px] font-semibold">
                            {
                              dispatchPlan
                                  .route
                                  .sourceNode
                            }
                          </span>

                            </div>


                            <div className="flex justify-between mt-2">

                          <span className="text-[10px] text-slate-500">
                            Destination
                          </span>

                              <span className="text-[10px] font-semibold">
                            {
                              dispatchPlan
                                  .route
                                  .destinationNode
                            }
                          </span>

                            </div>

                          </div>

                        </div>

                      </div>


                      {/* CLEAR ROUTE */}

                      <button
                          onClick={clearRoute}
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

                      <span className="text-xs text-yellow-400">
                      Active
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


                {/* =================================================
                MODAL HEADER
            ================================================= */}

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


                {/* =================================================
                FORM
            ================================================= */}

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