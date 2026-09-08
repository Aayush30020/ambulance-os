import xml.etree.ElementTree as ET
import json
import math
import os


INPUT_FILE = "data/osm/gurgaon_roads.osm"
OUTPUT_FILE = "data/osm/gurgaon_graph.json"


# ---------------------------------------------------------
# Road types that we want in our ambulance routing graph
# ---------------------------------------------------------

ALLOWED_HIGHWAYS = {
    "motorway",
    "motorway_link",
    "trunk",
    "trunk_link",
    "primary",
    "primary_link",
    "secondary",
    "secondary_link",
    "tertiary",
    "tertiary_link",
    "unclassified",
    "residential",
    "living_street"
}


# ---------------------------------------------------------
# Default speed assumptions.
#
# OSM roads don't always contain maxspeed information.
# Therefore we use reasonable defaults based on road type.
# ---------------------------------------------------------

DEFAULT_SPEEDS = {

    "motorway": 80,
    "motorway_link": 60,

    "trunk": 70,
    "trunk_link": 50,

    "primary": 60,
    "primary_link": 50,

    "secondary": 50,
    "secondary_link": 40,

    "tertiary": 40,
    "tertiary_link": 35,

    "unclassified": 30,

    "residential": 30,

    "living_street": 15
}


# ---------------------------------------------------------
# Haversine distance
#
# Calculates distance between two latitude/longitude points
# in kilometers.
# ---------------------------------------------------------

def haversine(
    lat1,
    lon1,
    lat2,
    lon2
):

    earth_radius_km = 6371.0

    lat1 = math.radians(lat1)
    lon1 = math.radians(lon1)

    lat2 = math.radians(lat2)
    lon2 = math.radians(lon2)

    dlat = lat2 - lat1
    dlon = lon2 - lon1

    a = (
        math.sin(dlat / 2) ** 2
        +
        math.cos(lat1)
        * math.cos(lat2)
        * math.sin(dlon / 2) ** 2
    )

    c = 2 * math.atan2(
        math.sqrt(a),
        math.sqrt(1 - a)
    )

    return earth_radius_km * c


# ---------------------------------------------------------
# Parse speed from OSM maxspeed tag.
#
# Examples:
#
# "50"      -> 50
# "50 km/h" -> 50
# "30 mph"  -> converted to km/h
#
# If unavailable, use highway default.
# ---------------------------------------------------------

def parse_speed(
    maxspeed,
    highway
):

    if not maxspeed:
        return DEFAULT_SPEEDS.get(
            highway,
            30
        )

    value = maxspeed.lower().strip()

    try:

        if "mph" in value:

            number = float(
                value
                .replace("mph", "")
                .strip()
            )

            return number * 1.60934

        number = ""

        for character in value:

            if (
                character.isdigit()
                or character == "."
            ):
                number += character
            else:
                break

        if number:

            return float(number)

    except ValueError:
        pass

    return DEFAULT_SPEEDS.get(
        highway,
        30
    )


# ---------------------------------------------------------
# Extract OSM nodes
# ---------------------------------------------------------

def parse_nodes():

    print("Reading OSM nodes...")

    nodes = {}

    context = ET.iterparse(
        INPUT_FILE,
        events=("end",)
    )

    for event, element in context:

        if element.tag != "node":
            continue

        node_id = element.attrib.get("id")

        latitude = element.attrib.get("lat")
        longitude = element.attrib.get("lon")

        if (
            node_id is not None
            and latitude is not None
            and longitude is not None
        ):

            nodes[node_id] = {
                "latitude": float(latitude),
                "longitude": float(longitude)
            }

        element.clear()

    print(
        f"Loaded {len(nodes)} OSM nodes."
    )

    return nodes


# ---------------------------------------------------------
# Extract road ways
# ---------------------------------------------------------

def parse_ways():

    print("Reading road ways...")

    ways = []

    context = ET.iterparse(
        INPUT_FILE,
        events=("end",)
    )

    for event, element in context:

        if element.tag != "way":
            continue

        highway = None
        maxspeed = None
        oneway = False

        node_refs = []

        for child in element:

            if child.tag == "nd":

                reference = child.attrib.get("ref")

                if reference:

                    node_refs.append(reference)

            elif child.tag == "tag":

                key = child.attrib.get("k")
                value = child.attrib.get("v")

                if key == "highway":

                    highway = value

                elif key == "maxspeed":

                    maxspeed = value

                elif key == "oneway":

                    if value in {
                        "yes",
                        "true",
                        "1"
                    }:

                        oneway = True

        if (
            highway in ALLOWED_HIGHWAYS
            and len(node_refs) >= 2
        ):

            speed = parse_speed(
                maxspeed,
                highway
            )

            ways.append({
                "nodes": node_refs,
                "highway": highway,
                "speedKmh": speed,
                "oneway": oneway
            })

        element.clear()

    print(
        f"Loaded {len(ways)} road ways."
    )

    return ways


# ---------------------------------------------------------
# Build graph
# ---------------------------------------------------------

def build_graph(
    nodes,
    ways
):

    print("Building graph...")

    graph_nodes = {}

    edges = []

    used_node_ids = set()

    for way in ways:

        node_refs = way["nodes"]

        speed = way["speedKmh"]

        oneway = way["oneway"]

        for i in range(
            len(node_refs) - 1
        ):

            source_id = node_refs[i]

            destination_id = node_refs[i + 1]

            if (
                source_id not in nodes
                or destination_id not in nodes
            ):

                continue

            source = nodes[source_id]

            destination = nodes[
                destination_id
            ]

            distance = haversine(
                source["latitude"],
                source["longitude"],
                destination["latitude"],
                destination["longitude"]
            )

            if distance <= 0:
                continue

            travel_time = (
                distance / speed
            ) * 60

            edges.append({

                "from": source_id,

                "to": destination_id,

                "distanceKm": round(
                    distance,
                    5
                ),

                "speedKmh": round(
                    speed,
                    2
                ),

                "travelTimeMinutes": round(
                    travel_time,
                    4
                )

            })

            used_node_ids.add(
                source_id
            )

            used_node_ids.add(
                destination_id
            )

            # -------------------------------------------------
            # Add reverse edge unless the road is one-way.
            # -------------------------------------------------

            if not oneway:

                reverse_travel_time = (
                    distance / speed
                ) * 60

                edges.append({

                    "from": destination_id,

                    "to": source_id,

                    "distanceKm": round(
                        distance,
                        5
                    ),

                    "speedKmh": round(
                        speed,
                        2
                    ),

                    "travelTimeMinutes": round(
                        reverse_travel_time,
                        4
                    )

                })

    # Keep only nodes that actually belong to our road graph.

    for node_id in used_node_ids:

        graph_nodes[node_id] = nodes[
            node_id
        ]

    print(
        f"Graph nodes: {len(graph_nodes)}"
    )

    print(
        f"Graph edges: {len(edges)}"
    )

    return graph_nodes, edges


# ---------------------------------------------------------
# Save graph as JSON
# ---------------------------------------------------------

def save_graph(
    graph_nodes,
    edges
):

    print("Writing graph JSON...")

    graph = {

        "nodes": graph_nodes,

        "edges": edges

    }

    os.makedirs(
        os.path.dirname(
            OUTPUT_FILE
        ),
        exist_ok=True
    )

    with open(
        OUTPUT_FILE,
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            graph,
            file,
            separators=(",", ":")
        )

    print(
        f"Graph saved to: {OUTPUT_FILE}"
    )


# ---------------------------------------------------------
# Main
# ---------------------------------------------------------

def main():

    print()
    print(
        "======================================"
    )
    print(
        "   Gurgaon OSM → Graph Converter"
    )
    print(
        "======================================"
    )
    print()

    nodes = parse_nodes()

    ways = parse_ways()

    graph_nodes, edges = build_graph(
        nodes,
        ways
    )

    save_graph(
        graph_nodes,
        edges
    )

    print()
    print(
        "Graph generation completed successfully."
    )
    print()


if __name__ == "__main__":

    main()
