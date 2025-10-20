import { Authenticated, Unauthenticated, useQuery } from "convex/react";
import { api } from "../convex/_generated/api";
import { SignInForm } from "./SignInForm";
import { SignOutButton } from "./SignOutButton";
import { Toaster } from "sonner";
import { useState } from "react";
import { SearchForm } from "./components/SearchForm";
import { FlightResults } from "./components/FlightResults";
import { AmadeusSeatMap } from "./components/AmadeusSeatMap";
import { TestCredentials } from "./components/TestCredentials";

export default function App() {
  const [currentView, setCurrentView] = useState<'search' | 'results' | 'seatmap'>('search');
  const [searchParams, setSearchParams] = useState<any>(null);
  const [selectedFlight, setSelectedFlight] = useState<any>(null);

  const handleSearch = (params: any) => {
    setSearchParams(params);
    setCurrentView('results');
  };

  const handleFlightSelect = (flight: any) => {
    setSelectedFlight(flight);
    setCurrentView('seatmap');
  };

  const handleBackToSearch = () => {
    setCurrentView('search');
    setSearchParams(null);
    setSelectedFlight(null);
  };

  const handleBackToResults = () => {
    setCurrentView('results');
    setSelectedFlight(null);
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-blue-50 to-indigo-100">
      <header className="sticky top-0 z-10 bg-white/90 backdrop-blur-sm border-b shadow-sm">
        <div className="max-w-7xl mx-auto px-4 h-16 flex justify-between items-center">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">✈</span>
            </div>
            <h1 className="text-xl font-bold text-gray-900">SeatMap Pro</h1>
            <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">
              Amadeus Powered
            </span>
          </div>
          <SignOutButton />
        </div>
      </header>

      <main className="flex-1">
        <Content 
          currentView={currentView}
          searchParams={searchParams}
          selectedFlight={selectedFlight}
          onSearch={handleSearch}
          onFlightSelect={handleFlightSelect}
          onBackToSearch={handleBackToSearch}
          onBackToResults={handleBackToResults}
        />
      </main>

      <Toaster position="top-right" />
    </div>
  );
}

function Content({ 
  currentView, 
  searchParams, 
  selectedFlight, 
  onSearch, 
  onFlightSelect, 
  onBackToSearch, 
  onBackToResults 
}: any) {
  const loggedInUser = useQuery(api.auth.loggedInUser);

  if (loggedInUser === undefined) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <Authenticated>
        {currentView === 'search' && (
          <div className="space-y-12">
            <div className="text-center space-y-4">
              <h2 className="text-4xl font-bold text-gray-900">
                Find Your Perfect Flight
              </h2>
              <p className="text-xl text-gray-600 max-w-2xl mx-auto">
                Search flights and select your ideal seat with real-time Amadeus seat maps.
              </p>
              <div className="flex justify-center space-x-8 text-sm text-gray-500">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  <span>✈️ Amadeus Integration</span>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                  <span>🗺️ Real Seat Maps</span>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-purple-500 rounded-full"></div>
                  <span>🚪 Exit Row Detection</span>
                </div>
              </div>
            </div>
            <SearchForm onSearch={onSearch} />
            
            {/* Temporary credentials test */}
            <div className="mt-8">
              <TestCredentials />
            </div>
          </div>
        )}

        {currentView === 'results' && searchParams && (
          <FlightResults 
            searchParams={searchParams}
            onFlightSelect={onFlightSelect}
            onBackToSearch={onBackToSearch}
          />
        )}

        {currentView === 'seatmap' && selectedFlight && (
          <AmadeusSeatMap 
            flightOffer={selectedFlight.flightOffer}
            onBackToResults={onBackToResults}
            userId={loggedInUser?._id}
          />
        )}
      </Authenticated>

      <Unauthenticated>
        <div className="max-w-md mx-auto space-y-8">
          <div className="text-center space-y-4">
            <h2 className="text-3xl font-bold text-gray-900">
              Welcome to SeatMap Pro
            </h2>
            <p className="text-gray-600">
              Sign in to start searching flights and selecting your favorite seats with Amadeus integration.
            </p>
          </div>
          <SignInForm />
        </div>
      </Unauthenticated>
    </div>
  );
}
