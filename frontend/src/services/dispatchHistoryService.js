import axios from "axios";

const API_URL = "http://localhost:8080/api";

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