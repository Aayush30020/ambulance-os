import axios from "axios";

const API_URL = "http://localhost:8080/api";

export const getTrafficRoute = async (
    sourceNode,
    destinationNode
) => {
    const response = await axios.get(
        `${API_URL}/traffic-routes/${sourceNode}/${destinationNode}`
    );

    return response.data;
};