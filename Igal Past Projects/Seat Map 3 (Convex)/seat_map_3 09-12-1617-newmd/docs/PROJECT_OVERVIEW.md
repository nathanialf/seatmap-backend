# SeatMap Pro - Project Documentation

## Overview
SeatMap Pro is a flight booking application that integrates with the Amadeus API to provide real-time flight search and seat map visualization. Users can search for flights, view available seats with detailed layouts, and make bookings.

## Tech Stack
- **Frontend**: React + TypeScript + Vite + TailwindCSS
- **Backend**: Convex (reactive database with real-time updates)
- **Authentication**: Convex Auth (username/password)
- **External API**: Amadeus Flight API for real flight data
- **UI Components**: Custom components with Tailwind styling

## Project Structure

### Frontend (`src/`)
```
src/
├── App.tsx                 # Main application component with routing logic
├── components/
│   ├── SearchForm.tsx      # Flight search form
│   ├── FlightResults.tsx   # Display search results
│   ├── AmadeusSeatMap.tsx  # Seat map visualization
│   ├── SeatMap.tsx         # Alternative seat map component
│   └── TestCredentials.tsx # API credentials testing
├── SignInForm.tsx          # Authentication form (locked)
├── SignOutButton.tsx       # Sign out component (locked)
└── lib/utils.ts           # Utility functions
```

### Backend (`convex/`)
```
convex/
├── schema.ts              # Database schema definition
├── auth.ts               # Authentication configuration (locked)
├── flights.ts            # Flight search and booking logic
├── amadeus.ts            # Amadeus API integration
├── amadeusRealSeatMap.ts # Real seat map data fetching
├── seatMap.ts            # Seat map processing
├── cache.ts              # Caching system for API responses
└── router.ts             # HTTP endpoints
```

## Application Flow

### 1. User Authentication
- Users sign in with username/password via Convex Auth
- Authentication state is managed globally
- Unauthenticated users see a landing page with sign-in form

### 2. Flight Search Flow
```
User Input → SearchForm → Amadeus API → Cache → FlightResults
```

**Steps:**
1. User fills search form (origin, destination, date, class)
2. `searchFlights` mutation called with search parameters
3. System checks cache first (`flightSearchCache` table)
4. If not cached, calls Amadeus Flight Offers Search API
5. Results stored in cache and returned to frontend
6. FlightResults component displays available flights

### 3. Seat Map Visualization Flow
```
Flight Selection → Seat Map API → Seat Processing → Visual Display
```

**Steps:**
1. User selects a flight from search results
2. `getSeatMap` action called with flight offer data
3. Amadeus Seat Map API called to get aircraft configuration
4. Seat data processed and organized by deck/cabin/row
5. AmadeusSeatMap component renders interactive seat layout
6. Users can select available seats

### 4. Booking Flow
```
Seat Selection → Booking Creation → Database Storage → Confirmation
```

**Steps:**
1. User selects desired seat from seat map
2. `createBooking` mutation called
3. Booking record created in `bookings` table
4. Seat marked as unavailable in `seats` table
5. User receives booking confirmation

## Database Schema

### Core Tables

**flights**
- Stores flight information (number, airline, route, times, price)
- Indexed by route (origin, destination)

**seats**
- Individual seat records linked to flights
- Contains position (row, column), type (window/aisle/middle), class
- Availability status and pricing

**bookings**
- User booking records
- Links users to specific flights and seats
- Tracks booking status and total price

**airports**
- Airport reference data (code, name, city, country)
- Used for search autocomplete and validation

### Cache Tables

**flightSearchCache**
- Caches Amadeus flight search results
- Reduces API calls and improves performance
- Keyed by search parameters hash

**airportSearchCache**
- Caches airport search results
- Enables fast airport autocomplete

## API Integration

### Amadeus API Endpoints Used

1. **Flight Offers Search**
   - Endpoint: `/v2/shopping/flight-offers`
   - Purpose: Search available flights
   - Cached for 1 hour

2. **Seat Map Display**
   - Endpoint: `/v1/shopping/seatmaps`
   - Purpose: Get aircraft seat configuration
   - Real-time data (not cached)

3. **Airport Search** (if implemented)
   - Endpoint: `/v1/reference-data/locations`
   - Purpose: Airport autocomplete

### API Call Flow
```
Frontend Request → Convex Action → Amadeus API → Response Processing → Database Storage → Frontend Update
```

## Key Features

### 1. Real-time Seat Maps
- Fetches actual aircraft configurations from Amadeus
- Shows seat types (window, aisle, middle)
- Displays cabin classes (economy, business, first)
- Indicates seat availability and pricing

### 2. Intelligent Caching
- Flight searches cached to reduce API costs
- Cache invalidation based on time and search parameters
- Separate caching for different data types

### 3. Responsive Design
- Mobile-first approach with TailwindCSS
- Interactive seat selection interface
- Loading states and error handling

### 4. User Management
- Secure authentication with Convex Auth
- User-specific booking history
- Session management

## Environment Variables Required

```
AMADEUS_API_KEY=your_amadeus_api_key
AMADEUS_API_SECRET=your_amadeus_api_secret
```

## Development Workflow

### 1. Local Development
```bash
npm install
npx convex dev  # Starts Convex backend
npm run dev     # Starts Vite frontend
```

### 2. Database Changes
- Modify `convex/schema.ts`
- Deploy with `npx convex deploy`
- Handle schema migrations if needed

### 3. API Testing
- Use TestCredentials component to verify Amadeus connection
- Check Convex dashboard for function logs
- Monitor API usage and rate limits

## Error Handling

### Frontend
- Loading states for async operations
- Error boundaries for component failures
- User-friendly error messages with Sonner toasts

### Backend
- API rate limit handling
- Graceful degradation when external APIs fail
- Comprehensive logging for debugging

## Performance Optimizations

1. **Caching Strategy**
   - Flight searches cached for 1 hour
   - Airport data cached indefinitely
   - Seat maps fetched fresh (real-time availability)

2. **Database Indexing**
   - Flights indexed by route for fast searches
   - Bookings indexed by user for quick retrieval
   - Seats indexed by flight for efficient seat maps

3. **Frontend Optimization**
   - React Query patterns with Convex
   - Lazy loading of components
   - Optimistic updates for better UX

## Security Considerations

1. **API Keys**
   - Stored as environment variables
   - Never exposed to frontend
   - Used only in Convex actions

2. **User Data**
   - Authentication required for all operations
   - User isolation in database queries
   - Secure session management

3. **Input Validation**
   - Convex validators for all function arguments
   - Frontend form validation
   - Sanitization of user inputs

## Deployment

### Production Deployment
1. Set up Convex production deployment
2. Configure environment variables in Convex dashboard
3. Deploy backend: `npx convex deploy --prod`
4. Build and deploy frontend to hosting platform

### Monitoring
- Convex dashboard for function performance
- API usage monitoring for Amadeus
- Error tracking and logging

## Future Enhancements

1. **Payment Integration**
   - Stripe or similar payment processor
   - Secure payment flow
   - Booking confirmation emails

2. **Advanced Features**
   - Multi-city flights
   - Seat preferences and recommendations
   - Price alerts and notifications

3. **Mobile App**
   - React Native version
   - Push notifications
   - Offline capability

## Troubleshooting

### Common Issues

1. **Amadeus API Errors**
   - Check API credentials in environment variables
   - Verify API quota and rate limits
   - Review request format and parameters

2. **Convex Deployment Failures**
   - Check schema compatibility
   - Verify function syntax and imports
   - Review environment variable configuration

3. **Frontend Build Issues**
   - Ensure all dependencies are installed
   - Check TypeScript compilation errors
   - Verify Convex client configuration

This documentation provides a comprehensive overview of the SeatMap Pro application architecture, functionality, and development workflow.
