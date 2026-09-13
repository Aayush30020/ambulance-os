import axios from "axios";
import API_URL from "../config/api";

export const getAllHospitals = async () => {
    const response = await axios.get(
        `${API_URL}/hospitals`
    );

    return response.data;
};