import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";
import { authTables } from "@convex-dev/auth/server";

const applicationTables = {
  airports: defineTable({
    code: v.string(),
    name: v.string(),
    city: v.string(),
    country: v.string(),
  }).index("by_code", ["code"]),

  flights: defineTable({
    flightNumber: v.string(),
    airline: v.string(),
    origin: v.string(),
    destination: v.string(),
    departureTime: v.string(),
    arrivalTime: v.string(),
    price: v.number(),
    availableSeats: v.number(),
    aircraft: v.string(),
    travelClass: v.string(),
  }).index("by_route", ["origin", "destination"]),

  seats: defineTable({
    flightId: v.id("flights"),
    seatNumber: v.string(),
    row: v.number(),
    column: v.string(),
    type: v.string(), // WINDOW, AISLE, MIDDLE
    class: v.string(), // ECONOMY, BUSINESS, FIRST
    available: v.boolean(),
    price: v.optional(v.number()),
  }).index("by_flight", ["flightId"]),

  bookings: defineTable({
    userId: v.id("users"),
    flightId: v.id("flights"),
    seatId: v.id("seats"),
    status: v.string(), // PENDING, CONFIRMED, CANCELLED
    totalPrice: v.number(),
    bookingDate: v.string(),
  }).index("by_user", ["userId"]),

  // Cache tables
  airportSearchCache: defineTable({
    keyword: v.string(),
    results: v.array(v.object({
      code: v.string(),
      name: v.string(),
      city: v.string(),
      country: v.string(),
    })),
  }).index("by_keyword", ["keyword"]),

  flightSearchCache: defineTable({
    cacheKey: v.string(),
    origin: v.string(),
    destination: v.string(),
    departureDate: v.string(),
    travelClass: v.string(),
    results: v.array(v.any()),
    source: v.string(),
  }).index("by_cache_key", ["cacheKey"]),
};

export default defineSchema({
  ...authTables,
  ...applicationTables,
});
