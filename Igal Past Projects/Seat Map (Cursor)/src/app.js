require('dotenv').config();
const express = require('express');
const path = require('path');
const Amadeus = require('amadeus');
const flightRoutes = require('./routes/flight-routes');
const { getSeatMapHandler } = require('./controllers/seat-map-controller');

const app = express();
const port = process.env.PORT || 3000;

// Initialize Amadeus SDK
const amadeus = new Amadeus({
  clientId: process.env.AMADEUS_CLIENT_ID,
  clientSecret: process.env.AMADEUS_CLIENT_SECRET
});

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, '../public')));

// CORS for development
if (process.env.NODE_ENV !== 'production') {
  app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
    next();
  });
}

// API healthcheck
app.get('/api/health', (req, res) => {
  res.json({ message: 'API funcionando' });
});

// Autocomplete endpoint
app.get('/api/autocomplete', async (req, res) => {
  try {
    const { keyword } = req.query;
    if (!keyword) {
      return res.status(400).json({ error: 'Keyword is required' });
    }
    const { data } = await amadeus.referenceData.locations.get({
      keyword: keyword,
      subType: Amadeus.location.city
    });
    res.json(data);
  } catch (error) {
    console.error('Error in autocomplete:', error.response || error);
    res.json([]);
  }
});

// Flight search endpoint
app.get('/api/search', async (req, res) => {
  try {
    const { 
      originLocationCode,
      destinationLocationCode,
      departureDate,
      returnDate,
      adults,
      children,
      infants,
      travelClass,
      carrierCode,
      flightNumber
    } = req.query;

        console.log('Search params:', req.query);

    // Validate required parameters
    if (!originLocationCode || !destinationLocationCode || !departureDate || !adults) {
        return res.status(400).json({
            error: 'Missing required parameters',
            required: ['originLocationCode', 'destinationLocationCode', 'departureDate', 'adults']
        });
    }

    console.log('Calling Amadeus API with params:', {
        originLocationCode,
        destinationLocationCode,
        departureDate,
        adults,
        children,
        infants,
        travelClass,
        returnDate,
        carrierCode,
        flightNumber
    });

    // Log the exact parameters being sent to Amadeus
    const amadeusParams = {
        originLocationCode,
        destinationLocationCode,
        departureDate,
        adults,
        children,
        infants,
        travelClass,
        ...(returnDate ? { returnDate } : {})
    };
    console.log('Exact Amadeus parameters:', amadeusParams);

    const { data } = await amadeus.shopping.flightOffersSearch.get({
      originLocationCode,
      destinationLocationCode,
      departureDate,
      adults,
      children,
      infants,
      travelClass,
      ...(returnDate ? { returnDate } : {})
    });

        console.log('Amadeus API response:', data);

    if (!data || data.length === 0) {
        console.log('No flights found');
        return res.json([]);
    }

    console.log(`Found ${data.length} flights`);
    
    // Log the first few flights to see their routes
    console.log('Sample flights:');
    data.slice(0, 3).forEach((flight, index) => {
        const firstSegment = flight.itineraries[0]?.segments[0];
        if (firstSegment) {
            console.log(`Flight ${index + 1}: ${firstSegment.departure.iataCode} -> ${firstSegment.arrival.iataCode}`);
        }
    });
    
    // Filter flights to ensure they match the exact origin and destination
    const filteredData = data.filter(flight => {
        const firstSegment = flight.itineraries[0]?.segments[0];
        if (!firstSegment) return false;
        
        const flightOrigin = firstSegment.departure.iataCode;
        const flightDestination = firstSegment.arrival.iataCode;
        
        const matchesOrigin = flightOrigin === originLocationCode;
        const matchesDestination = flightDestination === destinationLocationCode;
        
        if (!matchesOrigin || !matchesDestination) {
            console.log(`Filtering out flight: ${flightOrigin} -> ${flightDestination} (requested: ${originLocationCode} -> ${destinationLocationCode})`);
        }
        
        return matchesOrigin && matchesDestination;
    });
    
    console.log(`After filtering: ${filteredData.length} flights match the exact route`);
    
    // Additional filtering by carrier code and flight number if provided
    let finalFilteredData = filteredData;
    
    if (carrierCode) {
        console.log(`Filtering by carrier code: ${carrierCode}`);
        finalFilteredData = finalFilteredData.filter(flight => {
            const firstSegment = flight.itineraries[0]?.segments[0];
            if (!firstSegment) return false;
            
            const flightCarrier = firstSegment.carrierCode;
            const matches = flightCarrier === carrierCode;
            
            if (!matches) {
                console.log(`Filtering out flight with carrier ${flightCarrier} (requested: ${carrierCode})`);
            }
            
            return matches;
        });
        console.log(`After carrier filtering: ${finalFilteredData.length} flights match`);
    }
    
    if (flightNumber) {
        console.log(`Filtering by flight number: ${flightNumber}`);
        finalFilteredData = finalFilteredData.filter(flight => {
            const firstSegment = flight.itineraries[0]?.segments[0];
            if (!firstSegment) return false;
            
            const flightNumberStr = firstSegment.number;
            const matches = flightNumberStr === flightNumber;
            
            if (!matches) {
                console.log(`Filtering out flight with number ${flightNumberStr} (requested: ${flightNumber})`);
            }
            
            return matches;
        });
        console.log(`After flight number filtering: ${finalFilteredData.length} flights match`);
    }
    
    res.json(finalFilteredData);
  } catch (error) {
    console.error('Error in flight search:', error.response || error);
    
    if (error.response && error.response.result && error.response.result.errors) {
      const amadeusError = error.response.result.errors[0];
      console.error('Amadeus API error:', amadeusError);
      return res.status(error.response.statusCode).json({
        error: amadeusError.title,
        detail: amadeusError.detail
      });
    }

    res.status(500).json({ 
      error: 'Failed to search flights',
      message: error.message 
    });
  }
});

// Seat map endpoint
app.post('/api/seat-map', getSeatMapHandler);

// API routes
app.use('/api/flights', flightRoutes);

// Serve index.html for root
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something broke!' });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not Found' });
});

if (require.main === module) {
  app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
    console.log(`Ambiente: ${process.env.NODE_ENV || 'development'}`);
  });
}

module.exports = app; 