import { query, mutation } from "./_generated/server";
import { v } from "convex/values";

// Seat configuration by aircraft type
const AIRCRAFT_CONFIGS = {
  "Airbus A320": {
    rows: 30,
    seatsPerRow: ["A", "B", "C", "D", "E", "F"],
    aisleAfter: ["C"],
  },
  "Boeing 737-800": {
    rows: 32,
    seatsPerRow: ["A", "B", "C", "D", "E", "F"],
    aisleAfter: ["C"],
  },
  "Airbus A321": {
    rows: 35,
    seatsPerRow: ["A", "B", "C", "D", "E", "F"],
    aisleAfter: ["C"],
  },
};

function generateSeatMap(aircraft: string) {
  const config = AIRCRAFT_CONFIGS[aircraft as keyof typeof AIRCRAFT_CONFIGS];
  if (!config) return [];

  const seats = [];
  
  for (let row = 1; row <= config.rows; row++) {
    for (const column of config.seatsPerRow) {
      const seatNumber = `${row}${column}`;
      let type = "MIDDLE";
      
      if (column === "A" || column === "F") {
        type = "WINDOW";
      } else if (column === "C" || column === "D") {
        type = "AISLE";
      }

      // Service class (first 5 rows business, rest economy)
      const seatClass = row <= 5 ? "BUSINESS" : "ECONOMY";
      
      // Random availability (90% available)
      const available = Math.random() > 0.1;
      
      seats.push({
        seatNumber,
        row,
        column,
        type,
        class: seatClass,
        available,
        price: seatClass === "BUSINESS" ? 50 : type === "WINDOW" ? 15 : 0,
      });
    }
  }
  
  return seats;
}

export const generateSeatsForFlight = mutation({
  args: { flightNumber: v.string() },
  handler: async (ctx, args) => {
    // Search flight by number
    const flight = await ctx.db
      .query("flights")
      .filter((q) => q.eq(q.field("flightNumber"), args.flightNumber))
      .first();

    if (!flight) {
      return { success: false, error: "Flight not found" };
    }

    // Check if seats already exist
    const existingSeats = await ctx.db
      .query("seats")
      .withIndex("by_flight", (q) => q.eq("flightId", flight._id))
      .collect();

    if (existingSeats.length > 0) {
      return { success: true, message: "Seats already exist" };
    }

    // Generate seats
    const generatedSeats = generateSeatMap(flight.aircraft);
    
    for (const seat of generatedSeats) {
      await ctx.db.insert("seats", {
        flightId: flight._id,
        ...seat,
      });
    }

    return { success: true, message: "Seats generated successfully" };
  },
});

export const getSeatMap = query({
  args: { flightNumber: v.string() },
  handler: async (ctx, args) => {
    // Search flight by number
    const flight = await ctx.db
      .query("flights")
      .filter((q) => q.eq(q.field("flightNumber"), args.flightNumber))
      .first();

    if (!flight) {
      return { success: false, error: "Flight not found" };
    }

    // Search existing seats
    const seats = await ctx.db
      .query("seats")
      .withIndex("by_flight", (q) => q.eq("flightId", flight._id))
      .collect();

    return {
      success: true,
      data: {
        flight,
        seats: seats.map(seat => ({
          id: seat._id,
          seatNumber: seat.seatNumber,
          row: seat.row,
          column: seat.column,
          type: seat.type,
          class: seat.class,
          available: seat.available,
          price: seat.price || 0,
        })),
        needsGeneration: seats.length === 0,
      },
    };
  },
});

export const selectSeat = mutation({
  args: {
    seatId: v.id("seats"),
    userId: v.id("users"),
  },
  handler: async (ctx, args) => {
    const seat = await ctx.db.get(args.seatId);
    
    if (!seat) {
      return { success: false, error: "Seat not found" };
    }
    
    if (!seat.available) {
      return { success: false, error: "Seat not available" };
    }

    // Mark seat as unavailable
    await ctx.db.patch(args.seatId, { available: false });

    // Create booking
    const flight = await ctx.db.get(seat.flightId);
    const totalPrice = (flight?.price || 0) + (seat.price || 0);

    await ctx.db.insert("bookings", {
      userId: args.userId,
      flightId: seat.flightId,
      seatId: args.seatId,
      status: "PENDING",
      totalPrice,
      bookingDate: new Date().toISOString(),
    });

    return { success: true, totalPrice };
  },
});
