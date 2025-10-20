"use node";

import { action } from "./_generated/server";
import { v } from "convex/values";
import { api } from "./_generated/api";

// Sample data as fallback
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

// Helper function to get airline names
function getAirlineName(carrierCode: string): string {
  const airlines: Record<string, string> = {
    'IB': 'Iberia',
    'VY': 'Vueling',
    'BA': 'British Airways',
    'AF': 'Air France',
    'LH': 'Lufthansa',
    'KL': 'KLM',
    'AZ': 'Alitalia',
    'OS': 'Austrian Airlines',
    'LX': 'Swiss International Air Lines',
    'TP': 'TAP Air Portugal',
    'UX': 'Air Europa',
    'FR': 'Ryanair',
    'U2': 'easyJet'
  };
  
  return airlines[carrierCode] || carrierCode;
}

// Action to search airports using Amadeus API with caching
export const searchAirports = action({
  args: { keyword: v.string() },
  handler: async (ctx, args): Promise<any[]> => {
    if (args.keyword.length < 2) {
      return [];
    }

    // Check cache first
    const cachedResult: any = await ctx.runQuery(api.cache.getCachedAirports, {
      keyword: args.keyword
    });

    if (cachedResult.cached) {
      console.log(`Using cached airport results for: ${args.keyword}`);
      return cachedResult.data;
    }

    console.log(`Cache miss for airports: ${args.keyword}, calling API`);

    try {
      const Amadeus = require('amadeus');
      const amadeus = new Amadeus({
        clientId: process.env.AMADEUS_CLIENT_ID,
        clientSecret: process.env.AMADEUS_CLIENT_SECRET
      });

      console.log('Searching airports with keyword:', args.keyword);

      const response = await amadeus.referenceData.locations.get({
        keyword: args.keyword,
        subType: 'AIRPORT,CITY',
        'page[limit]': 10
      });

      console.log(`Found ${response.data.length} airports from Amadeus`);

      const results = response.data.map((location: any) => ({
        code: location.iataCode,
        name: location.name,
        city: location.address?.cityName || '',
        country: location.address?.countryName || ''
      }));

      // Cache the results
      await ctx.runMutation(api.cache.cacheAirportResults, {
        keyword: args.keyword,
        results
      });

      return results;
    } catch (error) {
      console.error('Amadeus API error:', error);
      
      // Fallback to sample data if API fails
      const query = args.keyword.toLowerCase();
      const fallbackResults = SAMPLE_AIRPORTS.filter(airport => 
        airport.code.toLowerCase().includes(query) ||
        airport.name.toLowerCase().includes(query) ||
        airport.city.toLowerCase().includes(query)
      );

      // Cache fallback results too (with shorter TTL)
      await ctx.runMutation(api.cache.cacheAirportResults, {
        keyword: args.keyword,
        results: fallbackResults
      });

      return fallbackResults;
    }
  },
});

// Action to search flights using Amadeus API with caching
export const searchFlights = action({
  args: {
    origin: v.string(),
    destination: v.string(),
    departureDate: v.string(),
    returnDate: v.optional(v.string()),
    flightType: v.string(),
    travelClass: v.string(),
    carrierCode: v.optional(v.string()),
    flightNumber: v.optional(v.string()),
  },
  handler: async (ctx, args): Promise<any> => {
    // Check cache first (include carrier and flight number in cache key)
    const cacheKey = `${args.origin}-${args.destination}-${args.departureDate}-${args.travelClass}-${args.carrierCode || ''}-${args.flightNumber || ''}`;
    
    const cachedResult: any = await ctx.runQuery(api.cache.getCachedFlights, {
      origin: args.origin,
      destination: args.destination,
      departureDate: args.departureDate,
      travelClass: args.travelClass,
    });

    if (cachedResult.cached) {
      console.log(`Using cached flight results (${cachedResult.cacheAge}min old)`);
      
      // Apply additional filters to cached results if needed
      let filteredData = cachedResult.data;
      
      if (args.carrierCode) {
        filteredData = filteredData.filter((flight: any) => 
          flight.flightNumber.startsWith(args.carrierCode!)
        );
      }
      
      if (args.flightNumber) {
        filteredData = filteredData.filter((flight: any) => 
          flight.flightNumber.includes(args.flightNumber!)
        );
      }
      
      return {
        success: true,
        data: filteredData,
        source: cachedResult.source,
        cached: true,
        cacheAge: cachedResult.cacheAge,
        filtered: args.carrierCode || args.flightNumber ? true : false
      };
    }

    console.log('Cache miss for flights, calling API');

    try {
      const Amadeus = require('amadeus');
      const amadeus = new Amadeus({
        clientId: process.env.AMADEUS_CLIENT_ID,
        clientSecret: process.env.AMADEUS_CLIENT_SECRET
      });

      // Validations
      const departureDateObj = new Date(args.departureDate + 'T00:00:00');
      const now = new Date();
      if (departureDateObj <= now) {
        return {
          success: false,
          error: 'Departure date must be in the future'
        };
      }

      if (args.returnDate) {
        const returnDateObj = new Date(args.returnDate + 'T00:00:00');
        if (returnDateObj <= departureDateObj) {
          return {
            success: false,
            error: 'Return date must be after departure date'
          };
        }
      }

      // Parameters for Amadeus - always use 1 adult
      const searchParams: any = {
        originLocationCode: args.origin,
        destinationLocationCode: args.destination,
        departureDate: args.departureDate,
        adults: 1, // Always 1 adult
        travelClass: args.travelClass,
        max: 50
      };

      if (args.returnDate && args.flightType === 'round-trip') {
        searchParams.returnDate = args.returnDate;
      }

      // Add airline filter if specified
      if (args.carrierCode) {
        searchParams.includedAirlineCodes = args.carrierCode;
      }

      console.log('Calling Amadeus API with params:', searchParams);

      const response = await amadeus.shopping.flightOffersSearch.get(searchParams);

      console.log(`Raw Amadeus response: ${response.data.length} offers`);

      // Transform Amadeus data to expected frontend format
      let transformedFlights = response.data.map((offer: any) => {
        const firstSegment = offer.itineraries[0]?.segments[0];
        const lastSegment = offer.itineraries[0]?.segments[offer.itineraries[0].segments.length - 1];
        
        return {
          flightNumber: `${firstSegment.carrierCode}${firstSegment.number}`,
          airline: getAirlineName(firstSegment.carrierCode),
          origin: firstSegment.departure.iataCode,
          destination: lastSegment.arrival.iataCode,
          departureTime: firstSegment.departure.at,
          arrivalTime: lastSegment.arrival.at,
          price: parseFloat(offer.price.total),
          availableSeats: Math.floor(Math.random() * 200) + 50, // Amadeus doesn't always provide this
          aircraft: firstSegment.aircraft?.code || "Airbus A320",
          travelClass: args.travelClass,
          duration: offer.itineraries[0].duration,
          amadeusOfferId: offer.id
        };
      });

      // Filter by exact route
      transformedFlights = transformedFlights.filter((flight: any) => 
        flight.origin === args.origin && flight.destination === args.destination
      );

      // Apply flight number filter if specified
      if (args.flightNumber) {
        transformedFlights = transformedFlights.filter((flight: any) => 
          flight.flightNumber.includes(args.flightNumber!)
        );
      }

      console.log(`Found ${response.data.length} flights from Amadeus, ${transformedFlights.length} match filters`);

      // Cache the results
      await ctx.runMutation(api.cache.cacheFlightResults, {
        origin: args.origin,
        destination: args.destination,
        departureDate: args.departureDate,
        travelClass: args.travelClass,
        results: transformedFlights,
        source: 'amadeus'
      });

      return {
        success: true,
        data: transformedFlights,
        source: 'amadeus',
        filtered: args.carrierCode || args.flightNumber ? true : false
      };

    } catch (error: any) {
      console.error('Amadeus API error:', error);
      
      // If Amadeus API fails, use sample data as fallback
      let matchingFlights = SAMPLE_FLIGHTS.filter(flight => 
        flight.origin === args.origin && 
        flight.destination === args.destination &&
        flight.travelClass === args.travelClass
      );

      // Apply filters to sample data
      if (args.carrierCode) {
        matchingFlights = matchingFlights.filter(flight => 
          flight.flightNumber.startsWith(args.carrierCode!)
        );
      }

      if (args.flightNumber) {
        matchingFlights = matchingFlights.filter(flight => 
          flight.flightNumber.includes(args.flightNumber!)
        );
      }

      console.log(`Using fallback data: ${matchingFlights.length} flights`);

      // Cache fallback results
      await ctx.runMutation(api.cache.cacheFlightResults, {
        origin: args.origin,
        destination: args.destination,
        departureDate: args.departureDate,
        travelClass: args.travelClass,
        results: matchingFlights,
        source: 'sample'
      });

      return {
        success: true,
        data: matchingFlights,
        fallback: true,
        source: 'sample',
        error: 'Using sample data - Amadeus API not available',
        filtered: args.carrierCode || args.flightNumber ? true : false
      };
    }
  },
});
