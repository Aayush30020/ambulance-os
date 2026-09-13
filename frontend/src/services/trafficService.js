import axios from "axios";
import API_URL from "../config/api";

export const getTrafficRoute = async (
    sourceNode,
    destinationNode
) => {
    const response = await axios.get(
        `${API_URL}/traffic-routes/${sourceNode}/${destinationNode}`
    );

    return response.data;
};