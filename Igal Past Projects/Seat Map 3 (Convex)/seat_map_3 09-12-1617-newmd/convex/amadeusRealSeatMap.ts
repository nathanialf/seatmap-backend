"use node";

import { action } from "./_generated/server";
import { v } from "convex/values";

// Aircraft wing position database
const AIRCRAFT_WING_DATA = {
  '320': { startRow: 12, endRow: 18, position: 'mid-cabin' },
  '321': { startRow: 14, endRow: 20, position: 'mid-cabin' },
  '737': { startRow: 10, endRow: 16, position: 'mid-cabin' },
  '738': { startRow: 11, endRow: 17, position: 'mid-cabin' },
  '777': { startRow: 18, endRow: 26, position: 'mid-cabin' },
  '380': { startRow: 20, endRow: 30, position: 'mid-cabin' }
};

// Seat characteristic descriptions
const CHARACTERISTIC_DESCRIPTIONS = {
  'W': 'Window seat',
  'A': 'Aisle seat',
  '9': 'Middle seat',
  'M': 'Middle seat',
  'E': 'Emergency exit row',
  'B': 'Bulkhead seat',
  'X': 'Extra legroom',
  'P': 'Premium seat',
  'Q': 'Quiet zone',
  'G': 'Near galley',
  'T': 'Near lavatory',
  'F': 'Front section',
  'R': 'Rear section',
  'S': 'Side position',
  'V': 'Limited view',
  'O': 'Outside view'
};

// Facility type mapping
const FACILITY_TYPES = {
  'LA': 'Lavatory',
  'GA': 'Galley',
  'CL': 'Closet',
  'ST': 'Stairs',
  'DO': 'Door',
  'EM': 'Emergency Equipment'
};

interface ProcessedSeat {
  number: string;
  cabin: string;
  coordinates: { x: number; y: number };
  characteristics: string[];
  availability: {
    status: string;
    price: number | null;
    currency: string | null;
    selectable: boolean;
  };
  row: number;
  column: string;
  type: string;
  restrictions: any[];
}

interface ProcessedFacility {
  code: string;
  type: string;
  position: string;
  coordinates: { x: number; y: number };
  row: number;
  column: string;
  description: string;
}

interface LayoutInfo {
  totalRows: number;
  totalColumns: number;
  rowRange: { min: number; max: number };
  columnRange: { min: number; max: number };
  coordinateRange: {
    x: { min: number; max: number };
    y: { min: number; max: number };
  };
  seatsByRow: Map<number, Map<string, ProcessedSeat>>;
  cabinSections: Map<string, any>;
  aisleConfiguration: string;
}

class AmadeusSeatMapProcessor {
  private seats = new Map<string, ProcessedSeat>();
  private facilities = new Map<string, ProcessedFacility>();
  private layoutInfo: LayoutInfo | null = null;
  private exitRows: number[] = [];
  private aisleInfo: any = null;
  private wingInfo: any = null;

  constructor(private seatMapData: any) {}

  processSeatMap() {
    console.log('Starting Amadeus seat map processing...');
    
    try {
      // Extract deck data
      this.extractDeckData();
      
      // Analyze deck configuration
      this.analyzeDeckConfiguration();
      
      // Detect special features
      this.detectSpecialFeatures();
      
      // Build processed seat map
      return this.buildProcessedSeatMap();
      
    } catch (error) {
      console.error('Error processing seat map:', error);
      throw error;
    }
  }

  private extractDeckData() {
    if (!this.seatMapData.decks || this.seatMapData.decks.length === 0) {
      throw new Error('No deck data available');
    }

    const mainDeck = this.seatMapData.decks.find((deck: any) => deck.deckType === 'MAIN') 
                     || this.seatMapData.decks[0];

    // Process seats
    if (mainDeck.seats) {
      mainDeck.seats.forEach((seat: any) => {
        const processedSeat = this.processSeatData(seat);
        this.seats.set(seat.number, processedSeat);
      });
    }

    // Process facilities
    if (mainDeck.facilities) {
      mainDeck.facilities.forEach((facility: any) => {
        const processedFacility = this.processFacilityData(facility);
        this.facilities.set(facility.position || `${facility.row}${facility.column}`, processedFacility);
      });
    }

    console.log(`Processed ${this.seats.size} seats and ${this.facilities.size} facilities`);
  }

  private processSeatData(seat: any): ProcessedSeat {
    const row = this.extractRowFromSeatNumber(seat.number);
    const column = this.extractColumnFromSeatNumber(seat.number);
    
    return {
      number: seat.number,
      cabin: seat.cabin || 'ECONOMY',
      coordinates: seat.coordinates || { x: row, y: this.columnLetterToNumber(column) },
      characteristics: seat.characteristicsCodes || [],
      availability: this.processSeatAvailability(seat),
      row,
      column,
      type: this.determineSeatType(seat.characteristicsCodes || []),
      restrictions: this.extractSeatRestrictions(seat)
    };
  }

  private processFacilityData(facility: any): ProcessedFacility {
    return {
      code: facility.code,
      type: (FACILITY_TYPES as any)[facility.code] || 'Unknown',
      position: facility.position || `${facility.row}${facility.column}`,
      coordinates: facility.coordinates || { x: facility.row, y: this.columnLetterToNumber(facility.column) },
      row: facility.row,
      column: facility.column,
      description: (FACILITY_TYPES as any)[facility.code] || facility.code
    };
  }

  private processSeatAvailability(seat: any) {
    const availability = {
      status: 'unknown',
      price: null as number | null,
      currency: null as string | null,
      selectable: false
    };

    if (seat.travelerPricing && seat.travelerPricing.length > 0) {
      const pricing = seat.travelerPricing[0];
      
      if (pricing.seatAvailabilityStatus) {
        availability.status = this.normalizeSeatStatus(pricing.seatAvailabilityStatus);
      }
      
      if (pricing.price) {
        availability.price = parseFloat(pricing.price.total);
        availability.currency = pricing.price.currency;
      }
    }

    availability.selectable = this.isSeatSelectable(availability.status);
    return availability;
  }

  private normalizeSeatStatus(status: string): string {
    const statusMap: Record<string, string> = {
      'available': 'available',
      'free': 'available',
      'open': 'available',
      'occupied': 'occupied',
      'taken': 'occupied',
      'booked': 'occupied',
      'blocked': 'blocked',
      'closed': 'blocked',
      'reserved': 'reserved',
      'hold': 'reserved',
      'unavailable': 'unavailable',
      'restricted': 'restricted'
    };
    
    return statusMap[status.toLowerCase()] || 'unknown';
  }

  private isSeatSelectable(status: string): boolean {
    return ['available', 'premium'].includes(status);
  }

  private determineSeatType(characteristics: string[]): string {
    if (characteristics.includes('W')) return 'window';
    if (characteristics.includes('A')) return 'aisle';
    if (characteristics.includes('9') || characteristics.includes('M')) return 'middle';
    return 'unknown';
  }

  private extractSeatRestrictions(seat: any): any[] {
    if (seat.travelerPricing && seat.travelerPricing[0] && seat.travelerPricing[0].restrictions) {
      return seat.travelerPricing[0].restrictions;
    }
    return [];
  }

  private analyzeDeckConfiguration() {
    this.layoutInfo = {
      totalRows: 0,
      totalColumns: 0,
      rowRange: { min: Infinity, max: -Infinity },
      columnRange: { min: Infinity, max: -Infinity },
      coordinateRange: {
        x: { min: Infinity, max: -Infinity },
        y: { min: Infinity, max: -Infinity }
      },
      seatsByRow: new Map(),
      cabinSections: new Map(),
      aisleConfiguration: 'unknown'
    };

    // Analyze seat ranges
    this.seats.forEach((seat) => {
      const row = seat.row;
      const column = seat.column;
      const coords = seat.coordinates;

      // Update ranges
      this.layoutInfo!.rowRange.min = Math.min(this.layoutInfo!.rowRange.min, row);
      this.layoutInfo!.rowRange.max = Math.max(this.layoutInfo!.rowRange.max, row);

      const colNum = this.columnLetterToNumber(column);
      this.layoutInfo!.columnRange.min = Math.min(this.layoutInfo!.columnRange.min, colNum);
      this.layoutInfo!.columnRange.max = Math.max(this.layoutInfo!.columnRange.max, colNum);

      if (coords) {
        this.layoutInfo!.coordinateRange.x.min = Math.min(this.layoutInfo!.coordinateRange.x.min, coords.x);
        this.layoutInfo!.coordinateRange.x.max = Math.max(this.layoutInfo!.coordinateRange.x.max, coords.x);
        this.layoutInfo!.coordinateRange.y.min = Math.min(this.layoutInfo!.coordinateRange.y.min, coords.y);
        this.layoutInfo!.coordinateRange.y.max = Math.max(this.layoutInfo!.coordinateRange.y.max, coords.y);
      }

      // Group by row
      if (!this.layoutInfo!.seatsByRow.has(row)) {
        this.layoutInfo!.seatsByRow.set(row, new Map());
      }
      this.layoutInfo!.seatsByRow.get(row)!.set(column, seat);
    });

    // Calculate totals
    this.layoutInfo.totalRows = this.layoutInfo.rowRange.max - this.layoutInfo.rowRange.min + 1;
    this.layoutInfo.totalColumns = this.layoutInfo.columnRange.max - this.layoutInfo.columnRange.min + 1;

    console.log('Deck configuration analyzed:', {
      totalRows: this.layoutInfo.totalRows,
      totalColumns: this.layoutInfo.totalColumns,
      rowRange: this.layoutInfo.rowRange
    });
  }

  private detectSpecialFeatures() {
    // Detect exit rows
    this.exitRows = this.detectExitRows();
    
    // Detect aisles
    this.aisleInfo = this.detectAisles();
    
    // Detect wing position
    this.wingInfo = this.detectWingPosition();

    console.log('Special features detected:', {
      exitRows: this.exitRows,
      aisleConfiguration: this.aisleInfo?.configuration,
      wingPosition: this.wingInfo?.position
    });
  }

  private detectExitRows(): number[] {
    const exitRows = new Set<number>();

    // From deck configuration
    const mainDeck = this.seatMapData.decks.find((deck: any) => deck.deckType === 'MAIN') 
                     || this.seatMapData.decks[0];

    if (mainDeck.deckConfiguration?.exitRows) {
      mainDeck.deckConfiguration.exitRows.forEach((row: string) => {
        exitRows.add(parseInt(row));
      });
    }

    if (mainDeck.deckConfiguration?.exitRowsX) {
      mainDeck.deckConfiguration.exitRowsX.forEach((row: number) => {
        exitRows.add(row);
      });
    }

    // From seat characteristics
    this.seats.forEach((seat) => {
      if (seat.characteristics.includes('E')) {
        exitRows.add(seat.row);
      }
    });

    return Array.from(exitRows).sort((a, b) => a - b);
  }

  private detectAisles() {
    const aisleInfo = {
      positions: new Set<string>(),
      byRow: new Map<number, string[]>(),
      configuration: 'unknown',
      pattern: [] as string[]
    };

    // Analyze each row for aisles
    this.layoutInfo!.seatsByRow.forEach((rowSeats, row) => {
      const columns = Array.from(rowSeats.keys()).sort((a, b) => 
        this.columnLetterToNumber(a) - this.columnLetterToNumber(b)
      );

      const aisleColumns: string[] = [];
      const sectionSizes: number[] = [];
      let currentSectionSize = 0;

      columns.forEach((column) => {
        const seat = rowSeats.get(column)!;
        currentSectionSize++;

        if (seat.characteristics.includes('A')) {
          aisleColumns.push(column);
          sectionSizes.push(currentSectionSize);
          currentSectionSize = 0;
        }
      });

      // Add last section
      if (currentSectionSize > 0) {
        sectionSizes.push(currentSectionSize);
      }

      if (aisleColumns.length > 0) {
        aisleInfo.byRow.set(row, aisleColumns);
        const pattern = sectionSizes.join('-');
        aisleInfo.pattern.push(pattern);
      }
    });

    // Determine most common configuration
    const patternCounts = new Map<string, number>();
    aisleInfo.pattern.forEach(pattern => {
      patternCounts.set(pattern, (patternCounts.get(pattern) || 0) + 1);
    });

    let mostCommonPattern = '';
    let maxCount = 0;
    patternCounts.forEach((count, pattern) => {
      if (count > maxCount) {
        maxCount = count;
        mostCommonPattern = pattern;
      }
    });

    aisleInfo.configuration = mostCommonPattern || 'unknown';
    return aisleInfo;
  }

  private detectWingPosition() {
    const wingInfo = {
      startRow: null as number | null,
      endRow: null as number | null,
      position: 'unknown',
      affectedSeats: new Set<string>(),
      confidence: 'low'
    };

    // Method 1: By aircraft code
    if (this.seatMapData.aircraft?.code) {
      const aircraftWingData = AIRCRAFT_WING_DATA[this.seatMapData.aircraft.code as keyof typeof AIRCRAFT_WING_DATA];
      if (aircraftWingData) {
        wingInfo.startRow = aircraftWingData.startRow;
        wingInfo.endRow = aircraftWingData.endRow;
        wingInfo.position = aircraftWingData.position;
        wingInfo.confidence = 'high';
      }
    }

    // Method 2: By seat characteristics
    this.seats.forEach((seat) => {
      const wingCodes = seat.characteristics.filter(code => 
        ['S', 'V', 'O'].includes(code)
      );

      if (wingCodes.length > 0) {
        if (!wingInfo.startRow || seat.row < wingInfo.startRow) {
          wingInfo.startRow = seat.row;
        }
        if (!wingInfo.endRow || seat.row > wingInfo.endRow) {
          wingInfo.endRow = seat.row;
        }

        wingInfo.affectedSeats.add(seat.number);
        if (wingInfo.confidence === 'low') {
          wingInfo.confidence = 'medium';
        }
      }
    });

    // Method 3: Estimation based on total length
    if (!wingInfo.startRow && this.layoutInfo!.totalRows > 0) {
      const estimatedStart = Math.floor(this.layoutInfo!.totalRows * 0.35) + this.layoutInfo!.rowRange.min;
      const estimatedEnd = Math.floor(this.layoutInfo!.totalRows * 0.65) + this.layoutInfo!.rowRange.min;

      wingInfo.startRow = estimatedStart;
      wingInfo.endRow = estimatedEnd;
      wingInfo.position = 'estimated';
      wingInfo.confidence = 'low';
    }

    return wingInfo;
  }

  private buildProcessedSeatMap() {
    return {
      success: true,
      flight: this.seatMapData.flight,
      aircraft: this.seatMapData.aircraft,
      seats: Array.from(this.seats.values()),
      facilities: Array.from(this.facilities.values()),
      layout: {
        totalRows: this.layoutInfo!.totalRows,
        totalColumns: this.layoutInfo!.totalColumns,
        rowRange: this.layoutInfo!.rowRange,
        columnRange: this.layoutInfo!.columnRange,
        coordinateRange: this.layoutInfo!.coordinateRange
      },
      features: {
        exitRows: this.exitRows,
        aisles: this.aisleInfo,
        wings: this.wingInfo
      },
      metadata: {
        processedAt: new Date().toISOString(),
        source: 'amadeus',
        totalSeats: this.seats.size,
        totalFacilities: this.facilities.size
      }
    };
  }

  // Helper methods
  private extractRowFromSeatNumber(seatNumber: string): number {
    const match = seatNumber.match(/\d+/);
    return match ? parseInt(match[0]) : 0;
  }

  private extractColumnFromSeatNumber(seatNumber: string): string {
    const match = seatNumber.match(/[A-Z]+/);
    return match ? match[0] : '';
  }

  private columnLetterToNumber(letter: string): number {
    return letter.charCodeAt(0) - 64; // A=1, B=2, etc.
  }
}

// Action to get real seat map from Amadeus
export const getRealSeatMap = action({
  args: {
    flightOffer: v.any()
  },
  handler: async (ctx, args) => {
    try {
      console.log('Getting real seat map from Amadeus...');

      // Validate flight offer
      if (!args.flightOffer || !args.flightOffer.id) {
        throw new Error('Invalid flight offer provided');
      }

      // Check if credentials are available
      if (!process.env.AMADEUS_CLIENT_ID || !process.env.AMADEUS_CLIENT_SECRET) {
        throw new Error('Amadeus credentials not configured. Please set AMADEUS_CLIENT_ID and AMADEUS_CLIENT_SECRET environment variables.');
      }

      const Amadeus = require('amadeus');
      const amadeus = new Amadeus({
        clientId: process.env.AMADEUS_CLIENT_ID,
        clientSecret: process.env.AMADEUS_CLIENT_SECRET,
        hostname: 'production' // Use production environment
      });

      console.log('Amadeus client initialized successfully');

      // Format flight offer for Amadeus
      const formattedOffer = {
        type: "flight-offer",
        id: args.flightOffer.id,
        source: args.flightOffer.source || "GDS",
        instantTicketingRequired: args.flightOffer.instantTicketingRequired || false,
        nonHomogeneous: args.flightOffer.nonHomogeneous || false,
        oneWay: args.flightOffer.oneWay || false,
        lastTicketingDate: args.flightOffer.lastTicketingDate,
        lastTicketingDateTime: args.flightOffer.lastTicketingDateTime,
        numberOfBookableSeats: args.flightOffer.numberOfBookableSeats || 1,
        itineraries: args.flightOffer.itineraries,
        price: args.flightOffer.price,
        pricingOptions: args.flightOffer.pricingOptions,
        validatingAirlineCodes: args.flightOffer.validatingAirlineCodes,
        travelerPricings: args.flightOffer.travelerPricings
      };

      console.log('Calling Amadeus Seat Map API...');
      console.log('Flight offer data:', JSON.stringify(formattedOffer, null, 2));

      // Call Amadeus API
      const response = await amadeus.shopping.seatmaps.post(
        JSON.stringify({
          data: [formattedOffer]
        }),
        {
          'Content-Type': 'application/json'
        }
      );

      console.log(`Amadeus API response status: ${response.statusCode}`);
      console.log(`Amadeus API response: ${response.data?.length || 0} seat maps`);
      
      if (response.data && response.data.length > 0) {
        console.log('First seat map data:', JSON.stringify(response.data[0], null, 2));
      }

      if (!response.data || response.data.length === 0) {
        throw new Error('No seat map data returned from Amadeus');
      }

      // Process the seat map data
      const processor = new AmadeusSeatMapProcessor(response.data[0]);
      const processedSeatMap = processor.processSeatMap();

      return processedSeatMap;

    } catch (error: any) {
      console.error('Amadeus seat map error:', error);
      console.error('Error details:', {
        message: error.message,
        status: error.response?.status,
        data: error.response?.data
      });

      // Handle Amadeus API errors with more detail
      if (error.response && error.response.result && error.response.result.errors) {
        const amadeusError = error.response.result.errors[0];
        console.error('Amadeus API Error Details:', amadeusError);
        console.log('API error occurred, using fallback data:', `${amadeusError.title} - ${amadeusError.detail}`);
      } else if (error.message.includes('credentials not configured')) {
        console.log('Credentials not configured, using fallback data');
      }

      // Return fallback data for development
      console.log('Returning fallback seat map data...', error.message);
      return {
        success: true,
        error: error.message,
        fallback: true,
        flight: {
          number: args.flightOffer.itineraries?.[0]?.segments?.[0]?.number || 'TEST123',
          departure: {
            iataCode: args.flightOffer.itineraries?.[0]?.segments?.[0]?.departure?.iataCode || 'MAD',
            at: args.flightOffer.itineraries?.[0]?.segments?.[0]?.departure?.at || new Date().toISOString()
          },
          arrival: {
            iataCode: args.flightOffer.itineraries?.[0]?.segments?.[0]?.arrival?.iataCode || 'BCN',
            at: args.flightOffer.itineraries?.[0]?.segments?.[0]?.arrival?.at || new Date().toISOString()
          },
          carrierCode: args.flightOffer.itineraries?.[0]?.segments?.[0]?.carrierCode || 'IB'
        },
        aircraft: {
          code: '320'
        },
        seats: generateFallbackSeats(),
        facilities: [],
        layout: {
          totalRows: 30,
          totalColumns: 6,
          rowRange: { min: 1, max: 30 },
          columnRange: { min: 1, max: 6 },
          coordinateRange: {
            x: { min: 1, max: 30 },
            y: { min: 1, max: 6 }
          }
        },
        features: {
          exitRows: [15, 16],
          aisles: { configuration: '3-3' },
          wings: { startRow: 12, endRow: 18, position: 'mid-cabin' }
        },
        metadata: {
          processedAt: new Date().toISOString(),
          source: 'fallback',
          totalSeats: 180,
          totalFacilities: 0
        }
      };
    }
  }
});

// Generate fallback seat data for development
function generateFallbackSeats() {
  const seats = [];
  const columns = ['A', 'B', 'C', 'D', 'E', 'F'];
  
  for (let row = 1; row <= 30; row++) {
    for (let colIndex = 0; colIndex < columns.length; colIndex++) {
      const column = columns[colIndex];
      const seatNumber = `${row}${column}`;
      
      // Determine seat type
      let type = 'middle';
      const characteristics = [];
      
      if (column === 'A' || column === 'F') {
        type = 'window';
        characteristics.push('W');
      } else if (column === 'C' || column === 'D') {
        type = 'aisle';
        characteristics.push('A');
      }
      
      // Exit rows
      if (row === 15 || row === 16) {
        characteristics.push('E');
      }
      
      // Premium seats (first 5 rows)
      if (row <= 5) {
        characteristics.push('P');
      }
      
      // Random availability (90% available)
      const isAvailable = Math.random() > 0.1;
      
      seats.push({
        number: seatNumber,
        cabin: row <= 5 ? 'BUSINESS' : 'ECONOMY',
        coordinates: { x: row, y: colIndex + 1 },
        characteristics,
        availability: {
          status: isAvailable ? 'available' : 'occupied',
          price: type === 'window' ? 15 : (row <= 5 ? 50 : 0),
          currency: 'EUR',
          selectable: isAvailable
        },
        row,
        column,
        type,
        restrictions: []
      });
    }
  }
  
  return seats;
}
