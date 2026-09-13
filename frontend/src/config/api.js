const API_URL =
    import.meta.env.VITE_API_URL ||
    (window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1"
        ? "http://localhost:8080/api"
        : "https://ambulance-os-backend.onrender.com/api");

export default API_URL;