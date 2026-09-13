import axios from "axios";
import API_URL from "../config/api";

export const compareAlgorithms = async (
    sourceNode,
    destinationNode
) => {
    const response = await axios.get(
        `${API_URL}/algorithm-comparison/${sourceNode}/${destinationNode}`
    );

    return response.data;
};