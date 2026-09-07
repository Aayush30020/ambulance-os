import axios from "axios";

const API_URL = "http://localhost:8080/api";


// =========================================================
// CREATE COMPLETE DISPATCH PLAN
// =========================================================
//
// This calls:
//
// POST /api/dispatch-plan/{emergencyId}
//
// The backend performs:
//
// Emergency
//     ↓
// PriorityQueue
//     ↓
// Best ambulance
//     ↓
// Hospital selection
//     ↓
// Dijkstra
//     ↓
// Complete dispatch plan
//
// =========================================================

export const createDispatchPlan = async (emergencyId) => {

    const response = await axios.post(
        `${API_URL}/dispatch-plan/${emergencyId}`
    );

    return response.data;
};


// =========================================================
// GET ROUTE COORDINATES
// =========================================================
//
// The dispatch-plan API gives us the selected ambulance
// and hospital IDs.
//
// We then call the existing route API to get the actual
// latitude/longitude points required by Leaflet.
//
// GET /api/routes/ambulance/{ambulanceId}/hospital/{hospitalId}
//
// =========================================================

export const getAmbulanceHospitalRoute = async (
    ambulanceId,
    hospitalId
) => {

    const response = await axios.get(
        `${API_URL}/routes/ambulance/${ambulanceId}/hospital/${hospitalId}`
    );

    return response.data;
};