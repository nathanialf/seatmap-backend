import { useAction } from "convex/react";
import { api } from "../../convex/_generated/api";
import { useState, useEffect } from "react";
import { toast } from "sonner";

interface AmadeusSeatMapProps {
  flightOffer: any;
  onBackToResults: () => void;
  userId: string | undefined;
}

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

interface SeatMapData {
  success: boolean;
  flight: any;
  aircraft: any;
  seats: ProcessedSeat[];
  facilities: any[];
  layout: any;
  features: any;
  metadata: any;
  error?: string;
  fallback?: boolean;
}

export function AmadeusSeatMap({ flightOffer, onBackToResults, userId }: AmadeusSeatMapProps) {
  const [selectedSeat, setSelectedSeat] = useState<ProcessedSeat | null>(null);
  const [seatMapData, setSeatMapData] = useState<SeatMapData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const getRealSeatMap = useAction(api.amadeusRealSeatMap.getRealSeatMap);

  useEffect(() => {
    const fetchSeatMap = async () => {
      setIsLoading(true);
      try {
        console.log('Fetching real seat map from Amadeus...');
        const result = await getRealSeatMap({ flightOffer });
        setSeatMapData(result);
        
        if ((result as any).fallback) {
          toast.warning('Using sample seat map - Amadeus API not available');
        } else if (result.success) {
          toast.success('Seat map loaded from Amadeus');
        } else {
          toast.error((result as any).error || 'Failed to load seat map');
        }
      } catch (error) {
        console.error('Error fetching seat map:', error);
        toast.error('Failed to load seat map');
      } finally {
        setIsLoading(false);
      }
    };

    fetchSeatMap();
  }, [flightOffer, getRealSeatMap]);

  const handleSeatSelect = (seat: ProcessedSeat) => {
    if (!seat.availability.selectable) return;
    setSelectedSeat(seat);
  };

  const handleConfirmSelection = async () => {
    if (!selectedSeat || !userId) return;

    try {
      toast.success(`Seat ${selectedSeat.number} selected successfully`);
      setSelectedSeat(null);
    } catch (error) {
      toast.error("Error processing selection");
    }
  };

  const getSeatColor = (seat: ProcessedSeat) => {
    if (!seat.availability.selectable) {
      switch (seat.availability.status) {
        case 'occupied':
          return "bg-red-500 cursor-not-allowed text-white";
        case 'blocked':
          return "bg-gray-500 cursor-not-allowed text-white";
        case 'reserved':
          return "bg-yellow-500 cursor-not-allowed text-black";
        default:
          return "bg-gray-300 cursor-not-allowed text-gray-600";
      }
    }
    
    if (selectedSeat?.number === seat.number) return "bg-blue-600 text-white";
    
    // Color by seat type and characteristics
    if (seat.characteristics.includes('P')) return "bg-purple-500 hover:bg-purple-600 cursor-pointer text-white"; // Premium
    if (seat.type === "window") return "bg-green-500 hover:bg-green-600 cursor-pointer text-white";
    if (seat.type === "aisle") return "bg-blue-500 hover:bg-blue-600 cursor-pointer text-white";
    return "bg-gray-400 hover:bg-gray-500 cursor-pointer text-white";
  };

  const getSeatIcon = (seat: ProcessedSeat) => {
    if (seat.characteristics.includes('E')) return "🚪"; // Exit
    if (seat.type === "window") return "🪟";
    if (seat.type === "aisle") return "🚶";
    if (seat.characteristics.includes('P')) return "⭐"; // Premium
    return seat.column;
  };

  const getCharacteristicDescription = (code: string) => {
    const descriptions: Record<string, string> = {
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
      'R': 'Rear section'
    };
    return descriptions[code] || code;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading seat map from Amadeus...</p>
          <p className="text-sm text-gray-500 mt-2">Processing flight offer and deck configuration</p>
        </div>
      </div>
    );
  }

  if (!seatMapData || (!seatMapData.success && !(seatMapData as any).fallback)) {
    return (
      <div className="text-center py-12">
        <div className="text-red-500 text-6xl mb-4">⚠️</div>
        <h3 className="text-xl font-medium text-gray-900 mb-2">
          Error loading seat map
        </h3>
        <p className="text-gray-600 mb-6">
          {(seatMapData as any)?.error || 'Could not retrieve seat map data'}
        </p>
        <button
          onClick={onBackToResults}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          Back to results
        </button>
      </div>
    );
  }

  const { seats = [], layout = {}, features = {}, flight = {}, aircraft = {} } = seatMapData;
  const rows = Array.from(new Set(seats.map((seat: ProcessedSeat) => seat.row))).sort((a, b) => a - b);
  const columns = ['A', 'B', 'C', 'D', 'E', 'F'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={onBackToResults}
            className="flex items-center space-x-2 text-blue-600 hover:text-blue-700 mb-4"
          >
            <span>←</span>
            <span>Back to results</span>
          </button>
          <h2 className="text-2xl font-bold text-gray-900">
            Select your seat - {flight.carrierCode || 'XX'}{flight.number || '000'}
          </h2>
          <p className="text-gray-600">
            {flight.departure?.iataCode || 'XXX'} → {flight.arrival?.iataCode || 'XXX'} • {aircraft.code || 'Unknown'}
          </p>
          <div className="flex items-center space-x-4 text-sm text-gray-500 mt-2">
            <span>✈️ Amadeus Seat Map</span>
            {(seatMapData as any).fallback && <span className="text-yellow-600">📋 Sample Data</span>}
            <span>💺 {seats.length} seats</span>
            {features.exitRows && features.exitRows.length > 0 && <span>🚪 {features.exitRows.length} exit rows</span>}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Seat map */}
        <div className="lg:col-span-3">
          <div className="bg-white rounded-lg shadow-sm p-6">
            {/* Legend */}
            <div className="flex flex-wrap gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-green-500 rounded"></div>
                <span className="text-sm">🪟 Window</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-blue-500 rounded"></div>
                <span className="text-sm">🚶 Aisle</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-gray-400 rounded"></div>
                <span className="text-sm">💺 Middle</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-purple-500 rounded"></div>
                <span className="text-sm">⭐ Premium</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-red-500 rounded"></div>
                <span className="text-sm">❌ Occupied</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-blue-600 rounded"></div>
                <span className="text-sm">✓ Selected</span>
              </div>
            </div>

            {/* Aircraft */}
            <div className="max-w-md mx-auto">
              {/* Cockpit */}
              <div className="bg-gray-100 rounded-t-full h-8 mb-4 flex items-center justify-center">
                <span className="text-xs text-gray-600">Cockpit</span>
              </div>

              {/* Wing indicators */}
              {features.wings && features.wings.startRow && features.wings.endRow && rows.length > 0 && (
                <div className="relative">
                  <div 
                    className="absolute left-0 w-8 bg-gray-300 opacity-50 rounded-l-lg flex items-center justify-center text-xs text-gray-600"
                    style={{
                      top: `${(features.wings.startRow - rows[0]) * 44}px`,
                      height: `${(features.wings.endRow - features.wings.startRow + 1) * 44}px`
                    }}
                  >
                    ✈️
                  </div>
                  <div 
                    className="absolute right-0 w-8 bg-gray-300 opacity-50 rounded-r-lg flex items-center justify-center text-xs text-gray-600"
                    style={{
                      top: `${(features.wings.startRow - rows[0]) * 44}px`,
                      height: `${(features.wings.endRow - features.wings.startRow + 1) * 44}px`
                    }}
                  >
                    ✈️
                  </div>
                </div>
              )}

              {/* Seats */}
              <div className="space-y-2 relative">
                {rows.map((row: number) => {
                  const isExitRow = features.exitRows && features.exitRows.includes(row);
                  
                  return (
                    <div key={row} className={`flex items-center justify-center space-x-1 ${isExitRow ? 'bg-yellow-50 border border-yellow-200 rounded p-1' : ''}`}>
                      <div className="w-6 text-xs text-gray-500 text-center">
                        {row}
                        {isExitRow && <div className="text-yellow-600 text-xs">EXIT</div>}
                      </div>
                      
                      {columns.map((column, colIndex) => {
                        const seat = seats.find(s => s.row === row && s.column === column);
                        
                        return (
                          <div key={column} className="flex items-center">
                            {seat ? (
                              <button
                                onClick={() => handleSeatSelect(seat)}
                                disabled={!seat.availability.selectable}
                                className={`w-8 h-8 rounded text-xs font-medium transition-all duration-200 ${getSeatColor(seat)} ${
                                  seat.availability.selectable ? 'hover:scale-105' : ''
                                }`}
                                title={`${seat.number} - ${seat.type} - ${seat.cabin} ${
                                  seat.availability.price ? `(+${seat.availability.currency}${seat.availability.price})` : ''
                                } - ${seat.characteristics.map(getCharacteristicDescription).join(', ')}`}
                              >
                                {getSeatIcon(seat)}
                              </button>
                            ) : (
                              <div className="w-8 h-8"></div>
                            )}
                            
                            {/* Aisle after column C */}
                            {colIndex === 2 && <div className="w-4 flex justify-center">
                              <div className="w-1 h-6 bg-gray-200 rounded"></div>
                            </div>}
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>

              {/* Tail */}
              <div className="bg-gray-100 rounded-b-lg h-6 mt-4"></div>
            </div>
          </div>
        </div>

        {/* Selection panel */}
        <div className="space-y-6">
          {/* Flight info */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h3 className="font-semibold text-gray-900 mb-4">Flight information</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Flight:</span>
                <span className="font-medium">{flight.carrierCode || 'XX'}{flight.number || '000'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Aircraft:</span>
                <span className="font-medium">{aircraft.code || 'Unknown'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Route:</span>
                <span className="font-medium">{flight.departure?.iataCode || 'XXX'} → {flight.arrival?.iataCode || 'XXX'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Total seats:</span>
                <span className="font-medium">{seats.length}</span>
              </div>
            </div>
          </div>

          {/* Selected seat */}
          {selectedSeat && (
            <div className="bg-blue-50 rounded-lg shadow-sm p-6 border border-blue-200">
              <h3 className="font-semibold text-blue-900 mb-4">Selected seat</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-blue-700">Seat:</span>
                  <span className="font-medium text-blue-900">{selectedSeat.number}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-blue-700">Type:</span>
                  <span className="font-medium text-blue-900 capitalize">{selectedSeat.type}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-blue-700">Class:</span>
                  <span className="font-medium text-blue-900">{selectedSeat.cabin}</span>
                </div>
                {selectedSeat.characteristics.length > 0 && (
                  <div className="flex justify-between">
                    <span className="text-blue-700">Features:</span>
                    <span className="font-medium text-blue-900 text-right">
                      {selectedSeat.characteristics.map(getCharacteristicDescription).join(', ')}
                    </span>
                  </div>
                )}
                {selectedSeat.availability.price && selectedSeat.availability.price > 0 && (
                  <div className="flex justify-between">
                    <span className="text-blue-700">Extra fee:</span>
                    <span className="font-medium text-blue-900">
                      +{selectedSeat.availability.currency}{selectedSeat.availability.price}
                    </span>
                  </div>
                )}
              </div>
              
              <button
                onClick={handleConfirmSelection}
                className="w-full mt-4 bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition-colors"
              >
                Confirm selection
              </button>
            </div>
          )}

          {/* Amadeus info */}
          <div className="bg-green-50 rounded-lg shadow-sm p-6 border border-green-200">
            <h3 className="font-semibold text-green-900 mb-3">✈️ Amadeus Integration</h3>
            <ul className="text-sm text-green-800 space-y-2">
              <li>• Real-time seat availability</li>
              <li>• Exit row detection</li>
              <li>• Wing position mapping</li>
              <li>• Premium seat identification</li>
              <li>• Aisle configuration analysis</li>
            </ul>
            {seatMapData.metadata && (
              <div className="mt-3 pt-3 border-t border-green-200 text-xs text-green-700">
                <div>Source: {seatMapData.metadata.source || 'unknown'}</div>
                <div>Processed: {seatMapData.metadata.processedAt ? new Date(seatMapData.metadata.processedAt).toLocaleString() : 'unknown'}</div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
