import { useState, useEffect, useCallback, useRef } from "react";
import { useAction } from "convex/react";
import { api } from "../../convex/_generated/api";

interface SearchFormProps {
  onSearch: (params: any) => void;
}

export function SearchForm({ onSearch }: SearchFormProps) {
  const [formData, setFormData] = useState({
    origin: "",
    destination: "",
    departureDate: "",
    returnDate: "",
    flightType: "one-way",
    travelClass: "ECONOMY",
    carrierCode: "",
    flightNumber: "",
  });

  const [originQuery, setOriginQuery] = useState("");
  const [destinationQuery, setDestinationQuery] = useState("");
  const [showOriginSuggestions, setShowOriginSuggestions] = useState(false);
  const [showDestinationSuggestions, setShowDestinationSuggestions] = useState(false);
  const [originSuggestions, setOriginSuggestions] = useState<any[]>([]);
  const [destinationSuggestions, setDestinationSuggestions] = useState<any[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isLoadingOrigin, setIsLoadingOrigin] = useState(false);
  const [isLoadingDestination, setIsLoadingDestination] = useState(false);

  const searchAirports = useAction(api.amadeus.searchAirports);
  
  // Refs for debounce timeouts
  const originTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const destinationTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Common airline codes for suggestions
  const commonAirlines = [
    { code: "IB", name: "Iberia" },
    { code: "VY", name: "Vueling" },
    { code: "BA", name: "British Airways" },
    { code: "AF", name: "Air France" },
    { code: "LH", name: "Lufthansa" },
    { code: "KL", name: "KLM" },
    { code: "AZ", name: "Alitalia" },
    { code: "OS", name: "Austrian Airlines" },
    { code: "LX", name: "Swiss International Air Lines" },
    { code: "TP", name: "TAP Air Portugal" },
    { code: "UX", name: "Air Europa" },
    { code: "FR", name: "Ryanair" },
    { code: "U2", name: "easyJet" },
  ];

  // Debounced search function for origin
  const debouncedOriginSearch = useCallback(async (query: string) => {
    if (query.length < 2) {
      setOriginSuggestions([]);
      setIsLoadingOrigin(false);
      return;
    }

    try {
      setIsLoadingOrigin(true);
      const results = await searchAirports({ keyword: query });
      setOriginSuggestions(results);
    } catch (error) {
      console.error('Error searching airports:', error);
      setOriginSuggestions([]);
    } finally {
      setIsLoadingOrigin(false);
    }
  }, [searchAirports]);

  // Debounced search function for destination
  const debouncedDestinationSearch = useCallback(async (query: string) => {
    if (query.length < 2) {
      setDestinationSuggestions([]);
      setIsLoadingDestination(false);
      return;
    }

    try {
      setIsLoadingDestination(true);
      const results = await searchAirports({ keyword: query });
      setDestinationSuggestions(results);
    } catch (error) {
      console.error('Error searching airports:', error);
      setDestinationSuggestions([]);
    } finally {
      setIsLoadingDestination(false);
    }
  }, [searchAirports]);

  // Effect for origin search with improved debouncing
  useEffect(() => {
    // Clear previous timeout
    if (originTimeoutRef.current) {
      clearTimeout(originTimeoutRef.current);
    }

    // Set new timeout with longer delay to reduce API calls
    originTimeoutRef.current = setTimeout(() => {
      debouncedOriginSearch(originQuery);
    }, 500); // Increased from 300ms to 500ms

    return () => {
      if (originTimeoutRef.current) {
        clearTimeout(originTimeoutRef.current);
      }
    };
  }, [originQuery, debouncedOriginSearch]);

  // Effect for destination search with improved debouncing
  useEffect(() => {
    // Clear previous timeout
    if (destinationTimeoutRef.current) {
      clearTimeout(destinationTimeoutRef.current);
    }

    // Set new timeout with longer delay to reduce API calls
    destinationTimeoutRef.current = setTimeout(() => {
      debouncedDestinationSearch(destinationQuery);
    }, 500); // Increased from 300ms to 500ms

    return () => {
      if (destinationTimeoutRef.current) {
        clearTimeout(destinationTimeoutRef.current);
      }
    };
  }, [destinationQuery, debouncedDestinationSearch]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.origin && formData.destination && formData.departureDate) {
      setIsSearching(true);
      try {
        await onSearch(formData);
      } finally {
        setIsSearching(false);
      }
    }
  };

  const handleOriginSelect = (airport: any) => {
    setFormData({ ...formData, origin: airport.code });
    setOriginQuery(`${airport.code} - ${airport.name}`);
    setShowOriginSuggestions(false);
    setOriginSuggestions([]); // Clear suggestions to reduce memory
  };

  const handleDestinationSelect = (airport: any) => {
    setFormData({ ...formData, destination: airport.code });
    setDestinationQuery(`${airport.code} - ${airport.name}`);
    setShowDestinationSuggestions(false);
    setDestinationSuggestions([]); // Clear suggestions to reduce memory
  };

  // Close suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = () => {
      setShowOriginSuggestions(false);
      setShowDestinationSuggestions(false);
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="bg-white rounded-2xl shadow-xl p-8 max-w-4xl mx-auto">
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Flight type */}
        <div className="flex space-x-4">
          <label className="flex items-center space-x-2 cursor-pointer">
            <input
              type="radio"
              name="flightType"
              value="one-way"
              checked={formData.flightType === "one-way"}
              onChange={(e) => setFormData({ ...formData, flightType: e.target.value })}
              className="w-4 h-4 text-blue-600"
            />
            <span className="text-gray-700">One way</span>
          </label>
          <label className="flex items-center space-x-2 cursor-pointer">
            <input
              type="radio"
              name="flightType"
              value="round-trip"
              checked={formData.flightType === "round-trip"}
              onChange={(e) => setFormData({ ...formData, flightType: e.target.value })}
              className="w-4 h-4 text-blue-600"
            />
            <span className="text-gray-700">Round trip</span>
          </label>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Origin */}
          <div className="relative">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              From
            </label>
            <input
              type="text"
              value={originQuery}
              onChange={(e) => {
                setOriginQuery(e.target.value);
                setShowOriginSuggestions(true);
              }}
              onFocus={() => setShowOriginSuggestions(true)}
              onClick={(e) => e.stopPropagation()}
              placeholder="City or airport"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
            {isLoadingOrigin && (
              <div className="absolute right-3 top-11 transform -translate-y-1/2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
              </div>
            )}
            {showOriginSuggestions && originSuggestions && originSuggestions.length > 0 && (
              <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                {originSuggestions.map((airport) => (
                  <button
                    key={airport.code}
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOriginSelect(airport);
                    }}
                    className="w-full px-4 py-3 text-left hover:bg-gray-50 border-b border-gray-100 last:border-b-0"
                  >
                    <div className="font-medium">{airport.code} - {airport.name}</div>
                    <div className="text-sm text-gray-500">{airport.city}, {airport.country}</div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Destination */}
          <div className="relative">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              To
            </label>
            <input
              type="text"
              value={destinationQuery}
              onChange={(e) => {
                setDestinationQuery(e.target.value);
                setShowDestinationSuggestions(true);
              }}
              onFocus={() => setShowDestinationSuggestions(true)}
              onClick={(e) => e.stopPropagation()}
              placeholder="City or airport"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
            {isLoadingDestination && (
              <div className="absolute right-3 top-11 transform -translate-y-1/2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
              </div>
            )}
            {showDestinationSuggestions && destinationSuggestions && destinationSuggestions.length > 0 && (
              <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                {destinationSuggestions.map((airport) => (
                  <button
                    key={airport.code}
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDestinationSelect(airport);
                    }}
                    className="w-full px-4 py-3 text-left hover:bg-gray-50 border-b border-gray-100 last:border-b-0"
                  >
                    <div className="font-medium">{airport.code} - {airport.name}</div>
                    <div className="text-sm text-gray-500">{airport.city}, {airport.country}</div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Departure date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Departure
            </label>
            <input
              type="date"
              value={formData.departureDate}
              onChange={(e) => setFormData({ ...formData, departureDate: e.target.value })}
              min={today}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
          </div>

          {/* Return date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Return
            </label>
            <input
              type="date"
              value={formData.returnDate}
              onChange={(e) => setFormData({ ...formData, returnDate: e.target.value })}
              min={formData.departureDate || today}
              disabled={formData.flightType === "one-way"}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
            />
          </div>
        </div>

        {/* Advanced filters section */}
        <div className="border-t pt-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Advanced Filters (Optional)</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Airline */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Preferred Airline (Optional)
              </label>
              <input
                type="text"
                value={formData.carrierCode}
                onChange={(e) => setFormData({ ...formData, carrierCode: e.target.value.toUpperCase() })}
                placeholder="e.g., IB, BA, AF"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">
                Enter 2-letter airline code (e.g., IB for Iberia, BA for British Airways)
              </p>
              <div className="mt-2 flex flex-wrap gap-1">
                {commonAirlines.slice(0, 6).map(airline => (
                  <button
                    key={airline.code}
                    type="button"
                    onClick={() => setFormData({ ...formData, carrierCode: airline.code })}
                    className="text-xs px-2 py-1 bg-gray-100 hover:bg-gray-200 rounded text-gray-700 transition-colors"
                    title={airline.name}
                  >
                    {airline.code}
                  </button>
                ))}
              </div>
            </div>

            {/* Flight number */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Flight Number (Optional)
              </label>
              <input
                type="text"
                value={formData.flightNumber}
                onChange={(e) => setFormData({ ...formData, flightNumber: e.target.value.toUpperCase() })}
                placeholder="e.g., 1234"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">
                Enter only the number (e.g., 1234 for IB1234)
              </p>
            </div>
          </div>
        </div>

        {/* Travel class */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Travel class
          </label>
          <select
            value={formData.travelClass}
            onChange={(e) => setFormData({ ...formData, travelClass: e.target.value })}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="ECONOMY">Economy</option>
            <option value="PREMIUM_ECONOMY">Premium Economy</option>
            <option value="BUSINESS">Business</option>
            <option value="FIRST">First Class</option>
          </select>
        </div>

        {/* Search button */}
        <button
          type="submit"
          disabled={!formData.origin || !formData.destination || !formData.departureDate || isSearching}
          className="w-full bg-blue-600 text-white py-4 px-6 rounded-lg font-semibold text-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center"
        >
          {isSearching ? (
            <>
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
              Searching flights...
            </>
          ) : (
            'Search flights'
          )}
        </button>
      </form>
    </div>
  );
}
