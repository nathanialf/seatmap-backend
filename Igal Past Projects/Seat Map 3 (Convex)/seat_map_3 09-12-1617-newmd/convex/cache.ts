import { query, mutation } from "./_generated/server";
import { v } from "convex/values";

// Cache for airport searches
export const getCachedAirports = query({
  args: { keyword: v.string() },
  handler: async (ctx, args) => {
    if (args.keyword.length < 2) {
      return { cached: false, data: [] };
    }

    const normalizedKeyword = args.keyword.toLowerCase().trim();
    
    // Check if we have cached results for this keyword
    const cachedResult = await ctx.db
      .query("airportSearchCache")
      .withIndex("by_keyword", (q) => q.eq("keyword", normalizedKeyword))
      .first();

    if (cachedResult) {
      // Check if cache is still valid (24 hours)
      const cacheAge = Date.now() - cachedResult._creationTime;
      const cacheValidityMs = 24 * 60 * 60 * 1000; // 24 hours
      
      if (cacheAge < cacheValidityMs) {
        return { cached: true, data: cachedResult.results };
      }
    }

    return { cached: false, data: [] };
  },
});

export const cacheAirportResults = mutation({
  args: {
    keyword: v.string(),
    results: v.array(v.object({
      code: v.string(),
      name: v.string(),
      city: v.string(),
      country: v.string(),
    })),
  },
  handler: async (ctx, args) => {
    const normalizedKeyword = args.keyword.toLowerCase().trim();
    
    // Remove old cache entry if exists
    const existingCache = await ctx.db
      .query("airportSearchCache")
      .withIndex("by_keyword", (q) => q.eq("keyword", normalizedKeyword))
      .first();
    
    if (existingCache) {
      await ctx.db.delete(existingCache._id);
    }

    // Insert new cache entry
    await ctx.db.insert("airportSearchCache", {
      keyword: normalizedKeyword,
      results: args.results,
    });

    return { success: true };
  },
});

// Cache for flight searches
export const getCachedFlights = query({
  args: {
    origin: v.string(),
    destination: v.string(),
    departureDate: v.string(),
    travelClass: v.string(),
  },
  handler: async (ctx, args) => {
    const cacheKey = `${args.origin}-${args.destination}-${args.departureDate}-${args.travelClass}`;
    
    const cachedResult = await ctx.db
      .query("flightSearchCache")
      .withIndex("by_cache_key", (q) => q.eq("cacheKey", cacheKey))
      .first();

    if (cachedResult) {
      // Check if cache is still valid (30 minutes for flights)
      const cacheAge = Date.now() - cachedResult._creationTime;
      const cacheValidityMs = 30 * 60 * 1000; // 30 minutes
      
      if (cacheAge < cacheValidityMs) {
        return { 
          cached: true, 
          data: cachedResult.results,
          source: cachedResult.source,
          cacheAge: Math.floor(cacheAge / 1000 / 60) // age in minutes
        };
      }
    }

    return { cached: false, data: [] };
  },
});

export const cacheFlightResults = mutation({
  args: {
    origin: v.string(),
    destination: v.string(),
    departureDate: v.string(),
    travelClass: v.string(),
    results: v.array(v.any()),
    source: v.string(),
  },
  handler: async (ctx, args) => {
    const cacheKey = `${args.origin}-${args.destination}-${args.departureDate}-${args.travelClass}`;
    
    // Remove old cache entry if exists
    const existingCache = await ctx.db
      .query("flightSearchCache")
      .withIndex("by_cache_key", (q) => q.eq("cacheKey", cacheKey))
      .first();
    
    if (existingCache) {
      await ctx.db.delete(existingCache._id);
    }

    // Insert new cache entry
    await ctx.db.insert("flightSearchCache", {
      cacheKey,
      origin: args.origin,
      destination: args.destination,
      departureDate: args.departureDate,
      travelClass: args.travelClass,
      results: args.results,
      source: args.source,
    });

    return { success: true };
  },
});

// Cleanup old cache entries
export const cleanupCache = mutation({
  args: {},
  handler: async (ctx) => {
    const now = Date.now();
    const airportCacheLimit = 24 * 60 * 60 * 1000; // 24 hours
    const flightCacheLimit = 30 * 60 * 1000; // 30 minutes

    // Cleanup airport cache
    const oldAirportCaches = await ctx.db
      .query("airportSearchCache")
      .collect();
    
    for (const cache of oldAirportCaches) {
      if (now - cache._creationTime > airportCacheLimit) {
        await ctx.db.delete(cache._id);
      }
    }

    // Cleanup flight cache
    const oldFlightCaches = await ctx.db
      .query("flightSearchCache")
      .collect();
    
    for (const cache of oldFlightCaches) {
      if (now - cache._creationTime > flightCacheLimit) {
        await ctx.db.delete(cache._id);
      }
    }

    return { success: true };
  },
});
