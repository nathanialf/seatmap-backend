import { query, mutation } from "./_generated/server";
import { v } from "convex/values";

// Sample data for airports
const SAMPLE_AIRPORTS = [
  { code: "MAD", name: "Adolfo Suárez Madrid-Barajas", city: "Madrid", country: "Spain" },
  { code: "BCN", name: "Barcelona-El Prat", city: "Barcelona", country: "Spain" },
  { code: "LHR", name: "London Heathrow", city: "London", country: "United Kingdom" },
  { code: "CDG", name: "Charles de Gaulle", city: "Paris", country: "France" },
  { code: "FCO", name: "Leonardo da Vinci-Fiumicino", city: "Rome", country: "Italy" },
  { code: "AMS", name: "Amsterdam Schiphol", city: "Amsterdam", country: "Netherlands" },
  { code: "FRA", name: "Frankfurt am Main", city: "Frankfurt", country: "Germany" },
  { code: "MUC", name: "Munich", city: "Munich", country: "Germany" },
  { code: "ZUR", name: "Zurich", city: "Zurich", country: "Switzerland" },
  { code: "VIE", name: "Vienna International", city: "Vienna", country: "Austria" },
];

// Sample data for flights
const SAMPLE_FLIGHTS = [
  {
    flightNumber: "IB6301",
    airline: "Iberia",
    origin: "MAD",
    destination: "BCN",
    departureTime: "2024-02-15T08:30:00Z",
    arrivalTime: "2024-02-15T09:45:00Z",
    price: 89,
    availableSeats: 156,
    aircraft: "Airbus A320",
    travelClass: "ECONOMY"
  },
  {
    flightNumber: "VY1004",
    airline: "Vueling",
    origin: "MAD",
    destination: "BCN",
    departureTime: "2024-02-15T14:20:00Z",
    arrivalTime: "2024-02-15T15:35:00Z",
    price: 75,
    availableSeats: 142,
    aircraft: "Airbus A320",
    travelClass: "ECONOMY"
  },
  {
    flightNumber: "BA458",
    airline: "British Airways",
    origin: "MAD",
    destination: "LHR",
    departureTime: "2024-02-15T11:15:00Z",
    arrivalTime: "2024-02-15T12:30:00Z",
    price: 245,
    availableSeats: 89,
    aircraft: "Boeing 737-800",
    travelClass: "ECONOMY"
  },
  {
    flightNumber: "AF1801",
    airline: "Air France",
    origin: "MAD",
    destination: "CDG",
    departureTime: "2024-02-15T16:45:00Z",
    arrivalTime: "2024-02-15T18:50:00Z",
    price: 198,
    availableSeats: 124,
    aircraft: "Airbus A321",
    travelClass: "ECONOMY"
  },
];

export const searchAirports = query({
  args: { query: v.string() },
  handler: async (ctx, args) => {
    if (args.query.length < 2) {
      return [];
    }

    const query = args.query.toLowerCase();
    return SAMPLE_AIRPORTS.filter(airport => 
      airport.code.toLowerCase().includes(query) ||
      airport.name.toLowerCase().includes(query) ||
      airport.city.toLowerCase().includes(query)
    );
  },
});

export const searchFlights = query({
  args: {
    origin: v.string(),
    destination: v.string(),
    departureDate: v.string(),
    returnDate: v.optional(v.string()),
    flightType: v.string(),
    travelClass: v.string(),
  },
  handler: async (ctx, args) => {
    // Simulate flight search with sample data
    const matchingFlights = SAMPLE_FLIGHTS.filter(flight => 
      flight.origin === args.origin && 
      flight.destination === args.destination &&
      flight.travelClass === args.travelClass
    );

    return {
      success: true,
      data: matchingFlights,
    };
  },
});

export const getFlightById = query({
  args: { flightId: v.string() },
  handler: async (ctx, args) => {
    const flight = SAMPLE_FLIGHTS.find(f => f.flightNumber === args.flightId);
    return flight || null;
  },
});

export const initializeData = mutation({
  args: {},
  handler: async (ctx) => {
    // Initialize airports
    for (const airport of SAMPLE_AIRPORTS) {
      const existing = await ctx.db
        .query("airports")
        .withIndex("by_code", (q) => q.eq("code", airport.code))
        .first();
      
      if (!existing) {
        await ctx.db.insert("airports", airport);
      }
    }

    // Initialize flights
    for (const flight of SAMPLE_FLIGHTS) {
      const existing = await ctx.db
        .query("flights")
        .filter((q) => q.eq(q.field("flightNumber"), flight.flightNumber))
        .first();
      
      if (!existing) {
        await ctx.db.insert("flights", flight);
      }
    }

    return { success: true };
  },
});
