import { useState } from "react";
import {
    MapContainer,
    TileLayer,
    Marker,
    CircleMarker,
    Popup,
    useMap,
    useMapEvents,
} from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

/*
 * Gurgaon coordinates
 * Used as the initial center of the map.
 */
const GURGAON_CENTER = [28.4595, 77.0266];


/*
 * Component used to move the map
 * when user's current location is detected.
 */
function MapController({ position }) {
    const map = useMap();

    if (position) {
        map.flyTo(position, 15, {
            duration: 1.5,
        });
    }

    return null;
}


/*
 * Allows the user to click anywhere on the map
 * and select that location.
 */
function MapClickHandler({ onLocationSelect }) {
    useMapEvents({
        click(event) {
            const { lat, lng } = event.latlng;

            onLocationSelect({
                lat,
                lng,
            });
        },
    });

    return null;
}


/*
 * Main LocationPicker component.
 *
 * Props:
 * onLocationSelect(location)
 *
 * location:
 * {
 *    lat: number,
 *    lng: number
 * }
 */
export default function LocationPicker({ onLocationSelect }) {
    const [position, setPosition] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    /*
     * Get user's current location
     * using browser Geolocation API.
     */
    const getCurrentLocation = () => {
        setError("");
        setLoading(true);

        /*
         * Check whether browser supports
         * geolocation.
         */
        if (!navigator.geolocation) {
            setError(
                "Geolocation is not supported by your browser."
            );

            setLoading(false);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (location) => {
                const lat = location.coords.latitude;
                const lng = location.coords.longitude;

                const newPosition = [lat, lng];

                setPosition(newPosition);

                /*
                 * Send coordinates to parent component.
                 */
                onLocationSelect({
                    lat,
                    lng,
                });

                setLoading(false);
            },

            (error) => {
                console.error(
                    "Geolocation error:",
                    error
                );

                let message =
                    "Unable to get your location.";

                switch (error.code) {
                    case error.PERMISSION_DENIED:
                        message =
                            "Location permission was denied. Please allow location access.";
                        break;

                    case error.POSITION_UNAVAILABLE:
                        message =
                            "Your location is currently unavailable.";
                        break;

                    case error.TIMEOUT:
                        message =
                            "Location request timed out. Please try again.";
                        break;

                    default:
                        message =
                            "Unable to determine your location.";
                }

                setError(message);
                setLoading(false);
            },

            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0,
            }
        );
    };


    /*
     * Called when user clicks somewhere
     * on the Gurgaon map.
     */
    const handleMapLocationSelect = ({ lat, lng }) => {
        setPosition([lat, lng]);

        setError("");

        onLocationSelect({
            lat,
            lng,
        });
    };


    return (
        <div className="space-y-4">

            {/* Location button */}
            <button
                type="button"
                onClick={getCurrentLocation}
                disabled={loading}
                className="
                    w-full
                    rounded-lg
                    border
                    border-red-500/40
                    bg-red-500/10
                    px-4
                    py-3
                    text-sm
                    font-medium
                    text-red-400
                    transition
                    hover:bg-red-500/20
                    disabled:cursor-not-allowed
                    disabled:opacity-50
                "
            >
                {loading
                    ? "📍 Detecting your location..."
                    : "📍 Use My Current Location"}
            </button>


            {/* Error message */}
            {error && (
                <div
                    className="
                        rounded-lg
                        border
                        border-red-500/30
                        bg-red-500/10
                        px-4
                        py-3
                        text-sm
                        text-red-400
                    "
                >
                    {error}
                </div>
            )}


            {/* Map */}
            <div
                className="
                    overflow-hidden
                    rounded-xl
                    border
                    border-slate-700
                "
            >
                <MapContainer
                    center={GURGAON_CENTER}
                    zoom={12}
                    scrollWheelZoom={true}
                    style={{
                        height: "400px",
                        width: "100%",
                    }}
                >

                    {/* OpenStreetMap tiles */}
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />


                    {/* Handle map clicks */}
                    <MapClickHandler
                        onLocationSelect={
                            handleMapLocationSelect
                        }
                    />


                    {/* Move map to detected location */}
                    <MapController
                        position={position}
                    />


                    {/* Selected location */}
                    {position && (
                        <>
                            <CircleMarker
                                center={position}
                                radius={12}
                                pathOptions={{
                                    color: "#ef4444",
                                    fillColor: "#ef4444",
                                    fillOpacity: 0.8,
                                }}
                            >
                                <Popup>
                                    <strong>
                                        Emergency Location
                                    </strong>

                                    <br />

                                    Latitude:{" "}
                                    {position[0].toFixed(6)}

                                    <br />

                                    Longitude:{" "}
                                    {position[1].toFixed(6)}
                                </Popup>
                            </CircleMarker>
                        </>
                    )}
                </MapContainer>
            </div>


            {/* Selected coordinates */}
            {position && (
                <div
                    className="
                        rounded-lg
                        border
                        border-green-500/30
                        bg-green-500/10
                        p-4
                    "
                >
                    <div className="mb-2 text-sm font-medium text-green-400">
                        ✓ Location selected
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-sm">

                        <div>
                            <div className="text-slate-400">
                                Latitude
                            </div>

                            <div className="font-mono text-white">
                                {position[0].toFixed(6)}
                            </div>
                        </div>

                        <div>
                            <div className="text-slate-400">
                                Longitude
                            </div>

                            <div className="font-mono text-white">
                                {position[1].toFixed(6)}
                            </div>
                        </div>

                    </div>
                </div>
            )}

            {!position && (
                <div className="text-center text-sm text-slate-500">
                    Click anywhere on the map or use your current
                    location to select the emergency location.
                </div>
            )}

        </div>
    );
}