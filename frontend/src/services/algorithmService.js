import axios from "axios";

const API_URL = "http://localhost:8080/api";

export const compareAlgorithms = async (
    sourceNode,
    destinationNode
) => {
    const response = await axios.get(
        `${API_URL}/algorithm-comparison/${sourceNode}/${destinationNode}`
    );

    return response.data;
};