class FlightResults {
    constructor() {
        this.resultsContainer = document.getElementById('searchResults');
        this.loader = document.getElementById('loader');
    }

    formatDuration(duration) {
        // Convert ISO 8601 duration to human readable format
        const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?/);
        const hours = match[1] ? `${match[1]}h ` : '';
        const minutes = match[2] ? `${match[2]}m` : '';
        return `${hours}${minutes}`;
    }

    showLoader() {
        this.loader.style.display = 'block';
        this.resultsContainer.innerHTML = '';
    }

    hideLoader() {
        this.loader.style.display = 'none';
    }

    displayResults(flights) {
        console.log('Displaying flight results:', flights);
        this.hideLoader();
        
        if (!flights || flights.length === 0) {
            this.resultsContainer.innerHTML = `
                <div class="alert alert-info">
                    No se encontraron vuelos disponibles para los criterios de búsqueda.
                </div>
            `;
            return;
        }

        const resultsHTML = flights.map(flight => {
            console.log('Processing flight:', flight);
            
            const offer = flight.itineraries[0];
            const segment = offer.segments[0];
            const price = flight.price;
            
            console.log('Flight details:', {
                offer,
                segment,
                price
            });

            // Obtener el código de la aerolínea y número de vuelo
            const carrierCode = segment.carrierCode || '';
            const flightNumber = segment.number || '';
            const flightCode = `${carrierCode}${flightNumber}`;

            return `
                <div class="flight-result card mb-3">
                    <div class="card-body">
                        <div class="row align-items-center">
                            <div class="col-md-8">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div class="departure">
                                        <h5 class="mb-0">${segment.departure.at.split('T')[1].substring(0, 5)}</h5>
                                        <small class="text-muted">${segment.departure.iataCode}</small>
                                    </div>
                                    <div class="flight-duration text-center mx-3">
                                        <small class="text-muted">${this.formatDuration(offer.duration)}</small>
                                        <div class="flight-line"></div>
                                        <small class="text-muted">${flightCode}</small>
                                    </div>
                                    <div class="arrival text-end">
                                        <h5 class="mb-0">${segment.arrival.at.split('T')[1].substring(0, 5)}</h5>
                                        <small class="text-muted">${segment.arrival.iataCode}</small>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4 text-end">
                                <h4 class="text-primary mb-2">${price.total} ${price.currency}</h4>
                                <button class="btn btn-primary select-flight" 
                                        data-flight-id="${flight.id}"
                                        data-flight-offer='${JSON.stringify(flight)}'>
                                    Seleccionar
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        this.resultsContainer.innerHTML = resultsHTML;
        
        // Add click event listeners to all select buttons
        document.querySelectorAll('.select-flight').forEach(button => {
            button.addEventListener('click', async (event) => {
                // Store original text before any changes
                const originalText = event.target.innerHTML;
                
                try {
                    const flightOffer = JSON.parse(event.target.dataset.flightOffer);
                    console.log('Selected flight offer:', flightOffer);
                    
                    // Store the selected flight offer in a global variable for later use
                    window.selectedFlightOffer = flightOffer;

                    // Show loading state
                    event.target.disabled = true;
                    event.target.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Cargando...';

                    // Make the seat map request
                    const response = await fetch('/api/seat-map', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ flightOffer })
                    });

                    if (!response.ok) {
                        const errorData = await response.json();
                        throw new Error(errorData.error || 'Failed to get seat map');
                    }

                    const seatMapData = await response.json();
                    console.log('Seat map data received:', seatMapData);

                    // Initialize and display the seat map
                    const seatMap = new SeatMap('seatMap');
                    seatMap.displaySeatMap(seatMapData);

                    // Show the seat map interface (hide search and results)
                    if (typeof showSeatMapInterface === 'function') {
                        showSeatMapInterface();
                    }

                } catch (error) {
                    console.error('Error getting seat map:', error);
                    alert('Error al obtener el mapa de asientos: ' + error.message);
                } finally {
                    // Restore button state
                    event.target.disabled = false;
                    event.target.innerHTML = originalText;
                }
            });
        });
    }
}

// Exportar la clase para uso global
window.FlightResults = FlightResults;

// Function to handle flight selection
function selectFlight(flightId) {
    // TODO: Implement flight selection logic
    console.log('Selected flight:', flightId);
} 