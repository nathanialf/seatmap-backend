import { useAction } from "convex/react";
import { api } from "../../convex/_generated/api";
import { useState } from "react";

export function TestCredentials() {
  const [result, setResult] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const testCredentials = useAction(api.seatMapImproved.testAmadeusCredentials);
  const getRealSeatMap = useAction(api.amadeusRealSeatMap.getRealSeatMap);

  const handleTest = async () => {
    setIsLoading(true);
    try {
      const result = await testCredentials({});
      setResult(result);
    } catch (error) {
      setResult({ success: false, error: error instanceof Error ? error.message : 'Unknown error' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleTestSeatMap = async () => {
    setIsLoading(true);
    try {
      const testOffer = {
        id: "test-123",
        source: "GDS",
        itineraries: [{
          segments: [{
            departure: { iataCode: "MAD", at: new Date(Date.now() + 86400000).toISOString() },
            arrival: { iataCode: "BCN", at: new Date(Date.now() + 94800000).toISOString() },
            carrierCode: "IB",
            number: "6301"
          }]
        }],
        price: { currency: "EUR", total: "89.00" },
        validatingAirlineCodes: ["IB"]
      };

      const result = await getRealSeatMap({ flightOffer: testOffer });
      setResult({ ...result, testType: 'Seat Map Test' });
    } catch (error) {
      setResult({ 
        success: false, 
        error: error instanceof Error ? error.message : 'Unknown error',
        testType: 'Seat Map Test'
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-6 max-w-md mx-auto">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Test Amadeus Credentials
      </h3>
      
      <div className="space-y-3">
        <button
          onClick={handleTest}
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 transition-colors"
        >
          {isLoading ? 'Testing...' : 'Test Basic APIs'}
        </button>
        
        <button
          onClick={handleTestSeatMap}
          disabled={isLoading}
          className="w-full bg-green-600 text-white py-2 px-4 rounded-lg font-medium hover:bg-green-700 disabled:bg-gray-400 transition-colors"
        >
          {isLoading ? 'Testing...' : 'Test Seat Map API'}
        </button>
      </div>

      {result && (
        <div className="mt-4 p-4 rounded-lg bg-gray-50">
          <h4 className="font-medium mb-2">
            Result: {result.success ? '✅ Success' : '❌ Failed'}
          </h4>
          <pre className="text-xs text-gray-600 whitespace-pre-wrap">
            {JSON.stringify(result, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
