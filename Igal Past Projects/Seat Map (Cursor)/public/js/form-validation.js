// Airport validation and autocomplete
class AirportValidator {
    constructor() {
        // Element references
        this.originInput = document.getElementById('origin-input');
        this.originOptions = document.getElementById('origin-options');
        this.destinationInput = document.getElementById('destination-input');
        this.destinationOptions = document.getElementById('destination-options');
        this.flightTypeSelect = document.getElementById('flight-type-select');
        this.departureDateInput = document.getElementById('departureDate');
        this.returnDate = document.getElementById('return-date');
        this.returnDateInput = document.getElementById('return-date-input');
        this.travelClassSelect = document.getElementById('travel-class-select');
        this.carrierCodeInput = document.getElementById('carrierCode');
        this.flightNumberInput = document.getElementById('flightNumber');
        this.searchButton = document.getElementById('search-button');
        this.form = document.getElementById('flightSearchForm');
        this.debounceTimeout = null;

        // Store IATA codes for cities
        this.originCityCodes = {};
        this.destinationCityCodes = {};

        this.setupEventListeners();
        this.reset();
    }

    setupEventListeners() {
        // Autocomplete and validation
        this.originInput.addEventListener('input', () => {
            clearTimeout(this.debounceTimeout);
            this.debounceTimeout = setTimeout(() => {
                this.validateAirportCode(this.originInput, true);
                this.searchAirports(this.originInput.value, this.originOptions);
                this.updateSearchButtonState();
            }, 300);
        });
        this.destinationInput.addEventListener('input', () => {
            clearTimeout(this.debounceTimeout);
            this.debounceTimeout = setTimeout(() => {
                this.validateAirportCode(this.destinationInput, false);
                this.searchAirports(this.destinationInput.value, this.destinationOptions);
                this.updateSearchButtonState();
            }, 300);
        });

        // Flight type: show/hide return date
        this.flightTypeSelect.addEventListener('change', () => {
            if (this.flightTypeSelect.value === 'one-way') {
                this.returnDate.classList.add('d-none');
                this.returnDateInput.value = '';
            } else {
                this.returnDate.classList.remove('d-none');
                // Set default return date to today if empty
                if (!this.returnDateInput.value) {
                    this.returnDateInput.valueAsDate = new Date();
                }
            }
            this.updateSearchButtonState();
        });

        // Date validation
        this.departureDateInput.addEventListener('change', () => {
            this.validateDepartureDate();
            this.updateSearchButtonState();
        });
        this.returnDateInput.addEventListener('change', () => {
            this.updateSearchButtonState();
        });



        // Travel class
        this.travelClassSelect.addEventListener('change', () => {
            this.updateSearchButtonState();
        });

        // Optional flight details validation
        this.carrierCodeInput.addEventListener('input', () => {
            this.validateCarrierCode();
            this.updateSearchButtonState();
        });
        this.flightNumberInput.addEventListener('input', () => {
            this.validateFlightNumber();
            this.updateSearchButtonState();
        });

        // Enable/disable search button on any input
        document.body.addEventListener('input', () => {
            this.updateSearchButtonState();
        });

        // Form submission
        this.form.addEventListener('submit', (e) => this.handleSubmit(e));
    }

    reset() {
        if (this.originInput) this.originInput.value = '';
        if (this.destinationInput) this.destinationInput.value = '';
        if (this.flightTypeSelect) this.flightTypeSelect.value = 'one-way';
        if (this.departureDateInput) this.departureDateInput.valueAsDate = new Date();
        if (this.returnDateInput) this.returnDateInput.value = '';
        if (this.returnDate) this.returnDate.classList.add('d-none');
        if (this.travelClassSelect) this.travelClassSelect.value = 'ECONOMY';
        if (this.carrierCodeInput) this.carrierCodeInput.value = '';
        if (this.flightNumberInput) this.flightNumberInput.value = '';
        this.updateSearchButtonState();
        if (this.searchResultsSeparator) this.searchResultsSeparator.classList.add("d-none");
        if (this.searchResultsLoader) this.searchResultsLoader.classList.add("d-none");
        if (this.searchResults) this.searchResults.classList.add("d-none");
    }

    updateSearchButtonState() {
        // Habilita el botón solo si los campos requeridos están completos y válidos
        const requiredFilled = (this.originInput && this.originInput.value) && 
                              (this.destinationInput && this.destinationInput.value) && 
                              (this.departureDateInput && this.departureDateInput.value);
        let returnDateValid = true;
        if (this.flightTypeSelect && this.flightTypeSelect.value === 'round-trip') {
            returnDateValid = !!(this.returnDateInput && this.returnDateInput.value);
        }
        if (this.searchButton) {
            this.searchButton.disabled = !(requiredFilled && returnDateValid && this.isFormValid());
        }
    }

    setMinDate() {
        const today = new Date().toISOString().split('T')[0];
        if (this.departureDateInput) this.departureDateInput.min = today;
        if (this.returnDateInput) this.returnDateInput.min = today;
    }

    validateAirportCode(input, isOrigin) {
        if (!input) return false;
        
        const value = input.value.trim().toLowerCase();
        const cityCodes = isOrigin ? this.originCityCodes : this.destinationCityCodes;
        
        if (!value) {
            input.setCustomValidity('Please enter a city');
            return false;
        }

        if (!cityCodes[value]) {
            input.setCustomValidity('Please select a valid city from the list');
            return false;
        }

        input.setCustomValidity('');
        return true;
    }

    validateDepartureDate() {
        if (!this.departureDateInput) return false;
        
        const selectedDate = new Date(this.departureDateInput.value);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (selectedDate < today) {
            this.showValidationMessage(this.departureDateInput, 'Please select a future date');
            return false;
        }
        this.clearValidationMessage(this.departureDateInput);
        return true;
    }



    validateDifferentAirports() {
        if (!this.originInput || !this.destinationInput) return false;
        
        const origin = this.originInput.value.toUpperCase();
        const destination = this.destinationInput.value.toUpperCase();
        if (origin === destination) {
            this.showValidationMessage(this.destinationInput, 'Origin and destination cannot be the same');
            return false;
        }
        this.clearValidationMessage(this.destinationInput);
        return true;
    }

    validateCarrierCode() {
        if (!this.carrierCodeInput) return true;
        
        const value = this.carrierCodeInput.value.trim().toUpperCase();
        if (value && !/^[A-Z]{2,3}$/.test(value)) {
            this.showValidationMessage(this.carrierCodeInput, 'Please enter a valid airline code (2-3 letters)');
            return false;
        }
        this.clearValidationMessage(this.carrierCodeInput);
        return true;
    }

    validateFlightNumber() {
        if (!this.flightNumberInput) return true;
        
        const value = this.flightNumberInput.value.trim();
        if (value && !/^[0-9]{1,6}$/.test(value)) {
            this.showValidationMessage(this.flightNumberInput, 'Please enter a valid flight number (1-6 digits)');
            return false;
        }
        this.clearValidationMessage(this.flightNumberInput);
        return true;
    }

    showValidationMessage(input, message) {
        const formGroup = input.closest('.mb-2');
        let feedback = formGroup.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            formGroup.appendChild(feedback);
        }
        feedback.textContent = message;
        input.classList.add('is-invalid');
    }

    clearValidationMessage(input) {
        const formGroup = input.closest('.mb-2');
        const feedback = formGroup.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.remove();
        }
        input.classList.remove('is-invalid');
    }

    async searchAirports(keyword, datalist) {
        if (keyword.length < 2) {
            return;
        }

        try {
            const response = await fetch(`/api/autocomplete?keyword=${encodeURIComponent(keyword)}`);
            if (!response.ok) {
                throw new Error('Failed to fetch airport suggestions');
            }

            const data = await response.json();
            datalist.innerHTML = '';

            data.forEach(entry => {
                const option = document.createElement('option');
                option.value = entry.name;
                option.textContent = `${entry.name} (${entry.iataCode})`;
                datalist.appendChild(option);

                // Store IATA code
                const isOrigin = datalist.id === 'origin-options';
                if (isOrigin) {
                    this.originCityCodes[entry.name.toLowerCase()] = entry.iataCode;
                } else {
                    this.destinationCityCodes[entry.name.toLowerCase()] = entry.iataCode;
                }
            });

            console.log('Stored codes:', {
                origin: this.originCityCodes,
                destination: this.destinationCityCodes
            });
        } catch (error) {
            console.error('Error fetching airport suggestions:', error);
        }
    }

    isFormValid() {
        const isOriginValid = this.validateAirportCode(this.originInput, true);
        const isDestinationValid = this.validateAirportCode(this.destinationInput, false);
        const isDateValid = this.validateDepartureDate();
        const areAirportsDifferent = this.validateDifferentAirports();
        const isCarrierCodeValid = this.validateCarrierCode();
        const isFlightNumberValid = this.validateFlightNumber();
        return isOriginValid && isDestinationValid && isDateValid && areAirportsDifferent && isCarrierCodeValid && isFlightNumberValid;
    }

    showLoader() {
        if (!this.searchButton) return;
        
        const spinner = this.searchButton.querySelector('.spinner-border');
        if (spinner) {
            spinner.classList.remove('d-none');
        }
        this.searchButton.disabled = true;
    }

    hideLoader() {
        if (!this.searchButton) return;
        
        const spinner = this.searchButton.querySelector('.spinner-border');
        if (spinner) {
            spinner.classList.add('d-none');
        }
        this.searchButton.disabled = false;
    }

    async handleSubmit(event) {
        event.preventDefault();
        
        if (!this.isFormValid()) {
            return;
        }

        try {
            this.showLoader();
            const flights = await this.search();
            window.flightResults = new FlightResults();
            window.flightResults.displayResults(flights);
        } catch (error) {
            console.error('Error in form submission:', error);
            // Show error message to user
            const resultsContainer = document.getElementById('searchResults');
            if (resultsContainer) {
                resultsContainer.innerHTML = `
                    <div class="alert alert-danger">
                        ${error.message || 'Error searching for flights. Please try again.'}
                    </div>
                `;
            }
        } finally {
            this.hideLoader();
        }
    }

    async search() {
        const origin = this.originInput.value.trim().toLowerCase();
        const destination = this.destinationInput.value.trim().toLowerCase();
        
        // Get IATA codes from stored mappings
        const originCode = this.originCityCodes[origin];
        const destinationCode = this.destinationCityCodes[destination];
        
        console.log('Search input values:', {
            origin,
            destination,
            originCode,
            destinationCode,
            storedCodes: {
                origin: this.originCityCodes,
                destination: this.destinationCityCodes
            }
        });

        if (!originCode || !destinationCode) {
            console.error('Missing IATA codes:', { origin, destination, originCode, destinationCode });
            throw new Error('Please select valid origin and destination cities');
        }

        const formData = {
            originLocationCode: originCode,
            destinationLocationCode: destinationCode,
            departureDate: this.departureDateInput.value,
            adults: 1, // Default to 1 adult for seat map viewing
            children: 0,
            infants: 0,
            travelClass: this.travelClassSelect.value
        };

        // Add optional carrier code and flight number if provided
        if (this.carrierCodeInput && this.carrierCodeInput.value.trim()) {
            formData.carrierCode = this.carrierCodeInput.value.trim().toUpperCase();
        }
        if (this.flightNumberInput && this.flightNumberInput.value.trim()) {
            formData.flightNumber = this.flightNumberInput.value.trim();
        }

        // Add return date only for round trips
        if (this.flightTypeSelect.value === 'round-trip' && this.returnDateInput.value) {
            formData.returnDate = this.returnDateInput.value;
        }

        // Remove undefined or empty values
        Object.keys(formData).forEach(key => {
            if (formData[key] === undefined || formData[key] === '') {
                delete formData[key];
            }
        });

        console.log('Searching with params:', formData);

        try {
            const response = await fetch('/api/search?' + new URLSearchParams(formData));
            console.log('Search response status:', response.status);
            
            if (!response.ok) {
                const errorData = await response.json();
                console.error('Search error:', errorData);
                throw new Error(errorData.error || 'Search failed');
            }
            
            const data = await response.json();
            console.log('Search results:', data);
            return data;
        } catch (error) {
            console.error('Error in flight search:', error);
            throw error;
        }
    }
}

// Initialize the validator when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.airportValidator = new AirportValidator();
});

const searchResultsSeparator = document.getElementById("search-results-separator");
const searchResultsLoader = document.getElementById("search-results-loader");
const searchResults = document.getElementById("search-results");

const formatDate = (date) => {
    const [formattedDate] = date.toISOString().split("T");
    return formattedDate;
};

const formatNumber = (number) => {
    return `${Math.abs(parseInt(number))}`;
};

const search = async () => {
    try {
        const returns = this.flightTypeSelect.value === "round-trip";
        const params = new URLSearchParams({
            originLocationCode: this.originCityCodes[this.originInput.value.toLowerCase()],
            destinationLocationCode: this.destinationCityCodes[this.destinationInput.value.toLowerCase()],
            departureDate: formatDate(this.departureDateInput.valueAsDate),
            adults: formatNumber(this.adultsInput.value),
            children: formatNumber(this.childrenInput.value),
            infants: formatNumber(this.infantsInput.value),
            travelClass: this.travelClassSelect.value,
            ...(returns ? { returnDate: formatDate(this.returnDateInput.valueAsDate) } : {})
        });
        const response = await fetch(`/api/search?${params}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error(error);
    }
};

class FormValidation {
    constructor() {
        // Esperar a que el DOM esté cargado
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.initialize());
        } else {
            this.initialize();
        }
    }

    initialize() {
        // Obtener referencias a los elementos del formulario
        this.form = document.getElementById('flightSearchForm');
        this.originInput = document.getElementById('origin');
        this.destinationInput = document.getElementById('destination');
        this.departureDateInput = document.getElementById('departureDate');
        this.returnDateInput = document.getElementById('returnDate');
        this.adultsInput = document.getElementById('adults');
        this.childrenInput = document.getElementById('children');
        this.infantsInput = document.getElementById('infants');
        this.travelClassSelect = document.getElementById('travelClass');
        this.flightTypeSelect = document.getElementById('flightType');

        // Verificar que todos los elementos existan
        if (!this.form || !this.originInput || !this.destinationInput || 
            !this.departureDateInput || !this.returnDateInput || !this.adultsInput || 
            !this.childrenInput || !this.infantsInput || !this.travelClassSelect || 
            !this.flightTypeSelect) {
            console.error('No se pudieron encontrar todos los elementos del formulario');
            return;
        }
        
        this.originCityCodes = {};
        this.destinationCityCodes = {};
        
        // Agregar event listeners
        this.form.addEventListener('submit', this.handleSubmit.bind(this));
        this.originInput.addEventListener('input', () => this.searchAirports('origin'));
        this.destinationInput.addEventListener('input', () => this.searchAirports('destination'));
        this.flightTypeSelect.addEventListener('change', this.toggleReturnDate.bind(this));

        // Inicializar el campo de fecha de regreso
        this.toggleReturnDate();
    }

    validateForm() {
        if (!this.originInput || !this.destinationInput || !this.departureDateInput) {
            console.error('Elementos del formulario no encontrados');
            return false;
        }

        if (!this.originInput.value || !this.destinationInput.value) {
            alert('Por favor, seleccione origen y destino');
            return false;
        }

        if (!this.departureDateInput.value) {
            alert('Por favor, seleccione una fecha de salida');
            return false;
        }

        if (this.flightTypeSelect.value === 'round-trip' && !this.returnDateInput.value) {
            alert('Por favor, seleccione una fecha de regreso');
            return false;
        }

        return true;
    }

    showLoader() {
        const loader = document.getElementById('loader');
        if (loader) {
            loader.style.display = 'block';
        }
    }

    hideLoader() {
        const loader = document.getElementById('loader');
        if (loader) {
            loader.style.display = 'none';
        }
    }

    async handleSubmit(event) {
        event.preventDefault();
        
        if (!this.validateForm()) {
            return;
        }

        try {
            this.showLoader();
            const flights = await this.search();
            window.flightResults = new FlightResults();
            window.flightResults.displayResults(flights);
        } catch (error) {
            console.error('Error in form submission:', error);
            this.hideLoader();
            const resultsContainer = document.getElementById('searchResults');
            if (resultsContainer) {
                resultsContainer.innerHTML = `
                    <div class="alert alert-danger">
                        ${error.message || 'Error al buscar vuelos. Por favor, intente nuevamente.'}
                    </div>
                `;
            }
        }
    }

    toggleReturnDate() {
        if (!this.returnDateInput) return;
        
        if (this.flightTypeSelect.value === 'round-trip') {
            this.returnDateInput.disabled = false;
            this.returnDateInput.required = true;
        } else {
            this.returnDateInput.disabled = true;
            this.returnDateInput.required = false;
            this.returnDateInput.value = '';
        }
    }

    async searchAirports(type) {
        const input = type === 'origin' ? this.originInput : this.destinationInput;
        const keyword = input.value.trim();
        
        if (keyword.length < 2) return;

        try {
            const response = await fetch(`/api/airports?keyword=${encodeURIComponent(keyword)}`);
            if (!response.ok) throw new Error('Error al buscar aeropuertos');
            
            const airports = await response.json();
            const datalist = document.getElementById(`${type}List`);
            
            if (!datalist) return;
            
            datalist.innerHTML = '';
            airports.forEach(airport => {
                const option = document.createElement('option');
                option.value = `${airport.name} (${airport.iataCode})`;
                option.dataset.iataCode = airport.iataCode;
                datalist.appendChild(option);
            });

            // Guardar el código IATA cuando se selecciona una ciudad
            input.addEventListener('change', () => {
                const selectedOption = Array.from(datalist.options).find(
                    option => option.value === input.value
                );
                if (selectedOption) {
                    if (type === 'origin') {
                        this.originCityCodes[input.value.toLowerCase()] = selectedOption.dataset.iataCode;
                    } else {
                        this.destinationCityCodes[input.value.toLowerCase()] = selectedOption.dataset.iataCode;
                    }
                }
            });
        } catch (error) {
            console.error('Error searching airports:', error);
        }
    }

    async search() {
        const origin = this.originInput.value.trim().toLowerCase();
        const destination = this.destinationInput.value.trim().toLowerCase();
        
        // Get IATA codes from stored mappings
        const originCode = this.originCityCodes[origin];
        const destinationCode = this.destinationCityCodes[destination];
        
        console.log('Search input values:', {
            origin,
            destination,
            originCode,
            destinationCode,
            storedCodes: {
                origin: this.originCityCodes,
                destination: this.destinationCityCodes
            }
        });

        if (!originCode || !destinationCode) {
            console.error('Missing IATA codes:', { origin, destination, originCode, destinationCode });
            throw new Error('Please select valid origin and destination cities');
        }

        const formData = {
            originLocationCode: originCode,
            destinationLocationCode: destinationCode,
            departureDate: this.departureDateInput.value,
            adults: parseInt(this.adultsInput.value) || 1,
            children: parseInt(this.childrenInput.value) || 0,
            infants: parseInt(this.infantsInput.value) || 0,
            travelClass: this.travelClassSelect.value
        };

        // Add return date only for round trips
        if (this.flightTypeSelect.value === 'round-trip' && this.returnDateInput.value) {
            formData.returnDate = this.returnDateInput.value;
        }

        // Remove undefined or empty values
        Object.keys(formData).forEach(key => {
            if (formData[key] === undefined || formData[key] === '') {
                delete formData[key];
            }
        });

        console.log('Searching with params:', formData);

        try {
            const response = await fetch('/api/search?' + new URLSearchParams(formData));
            console.log('Search response status:', response.status);
            
            if (!response.ok) {
                const errorData = await response.json();
                console.error('Search error:', errorData);
                throw new Error(errorData.error || 'Search failed');
            }
            
            const data = await response.json();
            console.log('Search results:', data);
            return data;
        } catch (error) {
            console.error('Error in flight search:', error);
            throw error;
        }
    }
}

// Initialize the form validation
window.formValidation = new FormValidation(); 