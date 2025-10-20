import { useAction } from "convex/react";
import { api } from "../../convex/_generated/api";
import { useState, useEffect } from "react";
import { toast } from "sonner";

interface FlightResultsProps {
  searchParams: any;
  onFlightSelect: (flight: any) => void;
  onBackToSearch: () => void;
}

export function FlightResults({ searchParams, onFlightSelect, onBackToSearch }: FlightResultsProps) {
  const [flightResults, setFlightResults] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const searchFlights = useAction(api.amadeus.searchFlights);

  useEffect(() => {
    const performSearch = async () => {
      setIsLoading(true);
      try {
        console.log('Searching flights with params:', searchParams);
        const results = await searchFlights(searchParams);
        console.log('Flight search results:', results);
        setFlightResults(results);
        
        if (results.fallback) {
          toast.info(results.error || 'Using sample data');
        } else if (results.cached) {
          toast.success(`Using cached results (${results.cacheAge}min old)`);
        }

        if (results.filtered) {
          toast.info('Results filtered by airline/flight number');
        }
      } catch (error) {
        console.error('Error searching flights:', error);
        toast.error('Error searching flights');
        setFlightResults({ success: false, error: 'Error searching flights' });
      } finally {
        setIsLoading(false);
      }
    };

    performSearch();
  }, [searchParams, searchFlights]);

  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'short',
      day: 'numeric',
      month: 'short'
    });
  };

  const calculateDuration = (departure: string, arrival: string) => {
    const dep = new Date(departure);
    const arr = new Date(arrival);
    const diff = arr.getTime() - dep.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  // Enhanced flight selection to include full flight offer data
  const handleFlightSelect = (flight: any) => {
    // Create a complete flight offer object for Amadeus Seat Map API
    const flightOffer = {
      id: flight.amadeusOfferId || `${flight.flightNumber}-${Date.now()}`,
      source: "GDS",
      instantTicketingRequired: false,
      nonHomogeneous: false,
      oneWay: searchParams.flightType === "one-way",
      lastTicketingDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      lastTicketingDateTime: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
      numberOfBookableSeats: 1,
      itineraries: [
        {
          duration: flight.duration || "PT2H15M",
          segments: [
            {
              departure: {
                iataCode: flight.origin,
                terminal: "1",
                at: flight.departureTime
              },
              arrival: {
                iataCode: flight.destination,
                terminal: "1", 
                at: flight.arrivalTime
              },
              carrierCode: flight.flightNumber.substring(0, 2),
              number: flight.flightNumber.substring(2),
              aircraft: {
                code: "320"
              },
              operating: {
                carrierCode: flight.flightNumber.substring(0, 2)
              },
              duration: flight.duration || "PT2H15M",
              id: "1",
              numberOfStops: 0,
              blacklistedInEU: false
            }
          ]
        }
      ],
      price: {
        currency: "EUR",
        total: flight.price.toString(),
        base: (flight.price * 0.8).toString(),
        fees: [
          {
            amount: (flight.price * 0.2).toString(),
            type: "SUPPLIER"
          }
        ]
      },
      pricingOptions: {
        fareType: ["PUBLISHED"],
        includedCheckedBagsOnly: true
      },
      validatingAirlineCodes: [flight.flightNumber.substring(0, 2)],
      travelerPricings: [
        {
          travelerId: "1",
          fareOption: "STANDARD",
          travelerType: "ADULT",
          price: {
            currency: "EUR",
            total: flight.price.toString(),
            base: (flight.price * 0.8).toString()
          },
          fareDetailsBySegment: [
            {
              segmentId: "1",
              cabin: searchParams.travelClass || "ECONOMY",
              fareBasis: "Y",
              class: "Y",
              includedCheckedBags: {
                quantity: 1
              }
            }
          ]
        }
      ]
    };

    // Pass both the original flight data and the flight offer
    onFlightSelect({
      ...flight,
      flightOffer
    });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Searching flights with Amadeus...</p>
          <p className="text-sm text-gray-500 mt-2">Checking cache first, then API if needed</p>
        </div>
      </div>
    );
  }

  if (!flightResults || !flightResults.success) {
    return (
      <div className="text-center py-12">
        <div className="text-red-500 text-6xl mb-4">⚠️</div>
        <h3 className="text-xl font-medium text-gray-900 mb-2">
          Error searching flights
        </h3>
        <p className="text-gray-600 mb-6">
          {flightResults?.error || 'Could not retrieve results'}
        </p>
        <button
          onClick={onBackToSearch}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          Search again
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={onBackToSearch}
            className="flex items-center space-x-2 text-blue-600 hover:text-blue-700 mb-4"
          >
            <span>←</span>
            <span>New search</span>
          </button>
          <h2 className="text-2xl font-bold text-gray-900">
            Flights from {searchParams.origin} to {searchParams.destination}
          </h2>
          <div className="flex items-center space-x-4 text-gray-600">
            <span>{formatDate(searchParams.departureDate)}</span>
            <span>•</span>
            <span>{flightResults.data?.length || 0} flights found</span>
            {flightResults.source && (
              <>
                <span>•</span>
                <span className={`text-xs px-2 py-1 rounded-full ${
                  flightResults.source === 'amadeus' 
                    ? 'bg-green-100 text-green-700' 
                    : 'bg-yellow-100 text-yellow-700'
                }`}>
                  {flightResults.source === 'amadeus' ? '🌐 Amadeus API' : '📋 Sample data'}
                </span>
              </>
            )}
            {flightResults.cached && (
              <>
                <span>•</span>
                <span className="text-xs px-2 py-1 rounded-full bg-blue-100 text-blue-700">
                  💾 Cached ({flightResults.cacheAge}min)
                </span>
              </>
            )}
            {flightResults.filtered && (
              <>
                <span>•</span>
                <span className="text-xs px-2 py-1 rounded-full bg-purple-100 text-purple-700">
                  🔍 Filtered
                </span>
              </>
            )}
          </div>
          
          {/* Active filters */}
          {(searchParams.carrierCode || searchParams.flightNumber) && (
            <div className="mt-3 flex flex-wrap gap-2">
              {searchParams.carrierCode && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-blue-100 text-blue-800">
                  Airline: {searchParams.carrierCode}
                </span>
              )}
              {searchParams.flightNumber && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-blue-100 text-blue-800">
                  Flight: {searchParams.flightNumber}
                </span>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex flex-wrap gap-4">
          <button className="px-4 py-2 bg-blue-100 text-blue-700 rounded-lg text-sm font-medium">
            Best price
          </button>
          <button className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200">
            Fastest
          </button>
          <button className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200">
            Best schedule
          </button>
        </div>
      </div>

      {/* Flight list */}
      <div className="space-y-4">
        {flightResults.data?.map((flight: any, index: number) => (
          <div key={flight.flightNumber + index} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-8">
                {/* Airline */}
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                    <span className="text-white font-bold text-sm">
                      {flight.airline.substring(0, 2)}
                    </span>
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">{flight.airline}</div>
                    <div className="text-sm text-gray-500">{flight.flightNumber}</div>
                  </div>
                </div>

                {/* Schedule */}
                <div className="flex items-center space-x-6">
                  <div className="text-center">
                    <div className="text-xl font-bold text-gray-900">
                      {formatTime(flight.departureTime)}
                    </div>
                    <div className="text-sm text-gray-500">{flight.origin}</div>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <div className="w-16 h-px bg-gray-300"></div>
                    <div className="text-xs text-gray-500">
                      {flight.duration || calculateDuration(flight.departureTime, flight.arrivalTime)}
                    </div>
                    <div className="w-16 h-px bg-gray-300"></div>
                  </div>
                  
                  <div className="text-center">
                    <div className="text-xl font-bold text-gray-900">
                      {formatTime(flight.arrivalTime)}
                    </div>
                    <div className="text-sm text-gray-500">{flight.destination}</div>
                  </div>
                </div>
              </div>

              {/* Price and selection */}
              <div className="text-right space-y-2">
                <div className="text-2xl font-bold text-gray-900">
                  ${flight.price}
                </div>
                <div className="text-sm text-gray-500">
                  {flight.availableSeats} seats available
                </div>
                <button
                  onClick={() => handleFlightSelect(flight)}
                  className="bg-blue-600 text-white px-6 py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors"
                >
                  Select seats
                </button>
              </div>
            </div>

            {/* Additional info */}
            <div className="mt-4 pt-4 border-t border-gray-100">
              <div className="flex items-center justify-between text-sm text-gray-600">
                <span>Aircraft: {flight.aircraft}</span>
                <span>Class: {flight.travelClass}</span>
                {flight.amadeusOfferId && (
                  <span className="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded">
                    ID: {flight.amadeusOfferId.substring(0, 8)}...
                  </span>
                )}
                <span className="text-xs bg-green-50 text-green-600 px-2 py-1 rounded">
                  ✈️ Amadeus Seat Map Ready
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {flightResults.data?.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-400 text-6xl mb-4">✈</div>
          <h3 className="text-xl font-medium text-gray-900 mb-2">
            No flights found
          </h3>
          <p className="text-gray-600 mb-6">
            {(searchParams.carrierCode || searchParams.flightNumber) 
              ? 'Try removing some filters or changing your search criteria.'
              : 'Try changing your dates or destinations to find more options.'
            }
          </p>
          <button
            onClick={onBackToSearch}
            className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
          >
            Modify search
          </button>
        </div>
      )}
    </div>
  );
}
