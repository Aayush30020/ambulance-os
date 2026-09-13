import axios from "axios";
import API_URL from "../config/api";

export const completeDispatch = async (
    dispatchId
) => {

    const response = await axios.put(
        `${API_URL}/dispatches/${dispatchId}/complete`
    );

    return response.data;
};


export const getAllDispatches = async () => {

    const response = await axios.get(
        `${API_URL}/dispatches`
    );

    return response.data;
};


export const getDispatchById = async (
    dispatchId
) => {

    const response = await axios.get(
        `${API_URL}/dispatches/${dispatchId}`
    );

    return response.data;
};