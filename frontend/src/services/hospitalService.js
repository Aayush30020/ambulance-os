import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

export const getAllHospitals = async () => {
    const response = await axios.get(
        `${API_BASE_URL}/hospitals`
    );

    return response.data;
};