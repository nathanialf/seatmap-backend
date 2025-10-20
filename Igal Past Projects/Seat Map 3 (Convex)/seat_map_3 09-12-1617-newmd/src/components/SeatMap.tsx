import { useQuery, useMutation } from "convex/react";
import { api } from "../../convex/_generated/api";
import { useState, useEffect } from "react";
import { toast } from "sonner";

interface SeatMapProps {
  flight: any;
  onBackToResults: () => void;
  userId: string | undefined;
}

export function SeatMap({ flight, onBackToResults, userId }: SeatMapProps) {
  const [selectedSeat, setSelectedSeat] = useState<any>(null);
  const seatMapData = useQuery(api.seatMap.getSeatMap, { flightNumber: flight.flightNumber });
  const selectSeat = useMutation(api.seatMap.selectSeat);
  const generateSeats = useMutation(api.seatMap.generateSeatsForFlight);

  // Generate seats if they don't exist
  useEffect(() => {
    if (seatMapData?.success && seatMapData.data?.needsGeneration) {
      generateSeats({ flightNumber: flight.flightNumber });
    }
  }, [seatMapData, generateSeats, flight.flightNumber]);

  const handleSeatSelect = (seat: any) => {
    if (!seat.available) return;
    setSelectedSeat(seat);
  };

  const handleConfirmSelection = async () => {
    if (!selectedSeat || !userId) return;

    try {
      if (!userId) {
        toast.error("User not authenticated");
        return;
      }

      const result = await selectSeat({
        seatId: selectedSeat.id,
        userId: userId as any,
      });

      if (result.success) {
        toast.success(`Seat ${selectedSeat.seatNumber} selected successfully`);
        setSelectedSeat(null);
      } else {
        toast.error(result.error || "Error selecting seat");
      }
    } catch (error) {
      toast.error("Error processing selection");
    }
  };

  const getSeatColor = (seat: any) => {
    if (!seat.available) return "bg-red-200 cursor-not-allowed";
    if (selectedSeat?.id === seat.id) return "bg-blue-600 text-white";
    if (seat.type === "WINDOW") return "bg-green-100 hover:bg-green-200 cursor-pointer";
    if (seat.type === "AISLE") return "bg-yellow-100 hover:bg-yellow-200 cursor-pointer";
    return "bg-gray-100 hover:bg-gray-200 cursor-pointer";
  };

  const getSeatIcon = (seat: any) => {
    if (seat.type === "WINDOW") return "🪟";
    if (seat.type === "AISLE") return "🚶";
    return "💺";
  };

  if (!seatMapData) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!seatMapData.success) {
    return (
      <div className="text-center py-12">
        <div className="text-red-500 text-6xl mb-4">⚠️</div>
        <h3 className="text-xl font-medium text-gray-900 mb-2">
          Error loading seat map
        </h3>
        <p className="text-gray-600 mb-6">{seatMapData.error}</p>
        <button
          onClick={onBackToResults}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          Back to results
        </button>
      </div>
    );
  }

  const { seats, needsGeneration } = seatMapData.data || { seats: [], needsGeneration: false };

  // Show loading if seats are being generated
  if (needsGeneration || seats.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Generating seat map...</p>
        </div>
      </div>
    );
  }

  const rows = Array.from(new Set(seats.map((seat: any) => seat.row))).sort((a: number, b: number) => a - b);
  const columns = ["A", "B", "C", "D", "E", "F"];

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
            Select your seat - {flight.airline} {flight.flightNumber}
          </h2>
          <p className="text-gray-600">
            {flight.origin} → {flight.destination} • {flight.aircraft}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Seat map */}
        <div className="lg:col-span-3">
          <div className="bg-white rounded-lg shadow-sm p-6">
            {/* Legend */}
            <div className="flex flex-wrap gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-green-100 rounded"></div>
                <span className="text-sm">🪟 Window</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-yellow-100 rounded"></div>
                <span className="text-sm">🚶 Aisle</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-gray-100 rounded"></div>
                <span className="text-sm">💺 Middle</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 bg-red-200 rounded"></div>
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

              {/* Seats */}
              <div className="space-y-2">
                {rows.map((row: number) => (
                  <div key={row} className="flex items-center justify-center space-x-1">
                    <div className="w-6 text-xs text-gray-500 text-center">{row}</div>
                    
                    {columns.map((column, colIndex) => {
                      const seat = seats.find((s: any) => s.row === row && s.column === column);
                      
                      return (
                        <div key={column} className="flex items-center">
                          {seat ? (
                            <button
                              onClick={() => handleSeatSelect(seat)}
                              disabled={!seat.available}
                              className={`w-8 h-8 rounded text-xs font-medium transition-colors ${getSeatColor(seat)}`}
                              title={`${seat.seatNumber} - ${seat.type} - ${seat.class} ${seat.price > 0 ? `(+$${seat.price})` : ''}`}
                            >
                              {getSeatIcon(seat)}
                            </button>
                          ) : (
                            <div className="w-8 h-8"></div>
                          )}
                          
                          {/* Aisle after column C */}
                          {colIndex === 2 && <div className="w-4"></div>}
                        </div>
                      );
                    })}
                  </div>
                ))}
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
                <span className="font-medium">{flight.flightNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Airline:</span>
                <span className="font-medium">{flight.airline}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Base price:</span>
                <span className="font-medium">${flight.price}</span>
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
                  <span className="font-medium text-blue-900">{selectedSeat.seatNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-blue-700">Type:</span>
                  <span className="font-medium text-blue-900">
                    {selectedSeat.type === "WINDOW" ? "Window" : 
                     selectedSeat.type === "AISLE" ? "Aisle" : "Middle"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-blue-700">Class:</span>
                  <span className="font-medium text-blue-900">{selectedSeat.class}</span>
                </div>
                {selectedSeat.price > 0 && (
                  <div className="flex justify-between">
                    <span className="text-blue-700">Extra fee:</span>
                    <span className="font-medium text-blue-900">+${selectedSeat.price}</span>
                  </div>
                )}
                <div className="border-t border-blue-200 pt-3 mt-3">
                  <div className="flex justify-between text-base">
                    <span className="font-medium text-blue-900">Total:</span>
                    <span className="font-bold text-blue-900">
                      ${flight.price + (selectedSeat.price || 0)}
                    </span>
                  </div>
                </div>
              </div>
              
              <button
                onClick={handleConfirmSelection}
                className="w-full mt-4 bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition-colors"
              >
                Confirm selection
              </button>
            </div>
          )}

          {/* Tips */}
          <div className="bg-green-50 rounded-lg shadow-sm p-6 border border-green-200">
            <h3 className="font-semibold text-green-900 mb-3">💡 Tips</h3>
            <ul className="text-sm text-green-800 space-y-2">
              <li>• Window seats offer better views</li>
              <li>• Aisle seats provide easier movement</li>
              <li>• Avoid middle seats for more comfort</li>
              <li>• Front rows usually board first</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
