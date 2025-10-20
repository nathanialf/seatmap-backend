document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('flightSearchForm');
    const submitButton = form.querySelector('button[type="submit"]');
    const spinner = submitButton.querySelector('.spinner-border');

    // Set minimum date for departure date input
    const departureDateInput = document.getElementById('departureDate');
    if (departureDateInput) {
        const today = new Date().toISOString().split('T')[0];
        departureDateInput.min = today;
    }

    // Initialize the form validation when the DOM is loaded
    window.airportValidator = new AirportValidator();

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!window.airportValidator || !window.airportValidator.isFormValid()) {
            return;
        }

        // Show loading state
        submitButton.disabled = true;
        spinner.classList.remove('d-none');

        try {
            const flights = await window.airportValidator.search();
            window.flightResults = new FlightResults();
            window.flightResults.displayResults(flights);
        } catch (error) {
            console.error('Error:', error);
            const resultsContainer = document.getElementById('searchResults');
            if (resultsContainer) {
                resultsContainer.innerHTML = `
                    <div class="alert alert-danger">
                        ${error.message || 'An error occurred while searching for flights. Please try again.'}
                    </div>
                `;
            }
        } finally {
            submitButton.disabled = false;
            spinner.classList.add('d-none');
        }
    });
});

// Simulate API call with mock data
async function simulateApiCall(formData) {
    return new Promise((resolve) => {
        setTimeout(() => {
            // Mock flight data
            const flights = [
                {
                    id: 'FL001',
                    airline: 'American Airlines',
                    flightNumber: 'AA123',
                    origin: formData.origin,
                    destination: formData.destination,
                    departureTime: new Date(`${formData.departureDate}T08:00:00`).toISOString(),
                    arrivalTime: new Date(`${formData.departureDate}T10:30:00`).toISOString(),
                    price: 299.99
                },
                {
                    id: 'FL002',
                    airline: 'Delta Airlines',
                    flightNumber: 'DL456',
                    origin: formData.origin,
                    destination: formData.destination,
                    departureTime: new Date(`${formData.departureDate}T12:15:00`).toISOString(),
                    arrivalTime: new Date(`${formData.departureDate}T14:45:00`).toISOString(),
                    price: 349.99
                },
                {
                    id: 'FL003',
                    airline: 'United Airlines',
                    flightNumber: 'UA789',
                    origin: formData.origin,
                    destination: formData.destination,
                    departureTime: new Date(`${formData.departureDate}T16:30:00`).toISOString(),
                    arrivalTime: new Date(`${formData.departureDate}T19:00:00`).toISOString(),
                    price: 279.99
                }
            ];
            resolve(flights);
        }, 1500); // Simulate network delay
    });
} 