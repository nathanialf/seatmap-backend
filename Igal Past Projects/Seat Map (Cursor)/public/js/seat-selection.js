class SeatSelection {
    constructor() {
        this.selectedSeats = new Set();
        this.maxSeats = 1;
        this.currentFlight = null;
    }

    initialize(flight, maxSeats) {
        this.currentFlight = flight;
        this.maxSeats = maxSeats;
        this.selectedSeats.clear();
        this.renderSeatMap();
    }

    renderSeatMap() {
        const seatMapContainer = document.getElementById('seatMap');
        if (!seatMapContainer) return;

        // Clear previous content
        seatMapContainer.innerHTML = '';

        // Create seat map header
        const header = document.createElement('div');
        header.className = 'seat-map-header mb-4';
        header.innerHTML = `
            <h3 class="h5">Select Your Seats</h3>
            <p class="text-muted">Flight ${this.currentFlight.flightNumber} - ${this.currentFlight.airline}</p>
            <div class="seat-legend d-flex gap-3 mt-2">
                <div class="d-flex align-items-center">
                    <div class="seat-legend-item available"></div>
                    <span class="ms-2">Available</span>
                </div>
                <div class="d-flex align-items-center">
                    <div class="seat-legend-item selected"></div>
                    <span class="ms-2">Selected</span>
                </div>
                <div class="d-flex align-items-center">
                    <div class="seat-legend-item occupied"></div>
                    <span class="ms-2">Occupied</span>
                </div>
            </div>
        `;
        seatMapContainer.appendChild(header);

        // Create seat map grid
        const seatMap = document.createElement('div');
        seatMap.className = 'seat-map-grid';
        
        // Generate seats (mock data)
        const rows = ['A', 'B', 'C', 'D', 'E', 'F'];
        const columns = Array.from({length: 20}, (_, i) => i + 1);
        
        // Create aisle
        const aislePosition = 3;
        
        columns.forEach(column => {
            const rowContainer = document.createElement('div');
            rowContainer.className = 'seat-row d-flex justify-content-center gap-4 mb-2';
            
            rows.forEach((row, index) => {
                const seatNumber = `${column}${row}`;
                const isOccupied = Math.random() < 0.3; // 30% chance of being occupied
                
                const seat = document.createElement('div');
                seat.className = `seat ${isOccupied ? 'occupied' : 'available'}`;
                seat.dataset.seat = seatNumber;
                
                if (!isOccupied) {
                    seat.addEventListener('click', () => this.toggleSeat(seat));
                }
                
                seat.innerHTML = `
                    <div class="seat-number">${seatNumber}</div>
                `;
                
                rowContainer.appendChild(seat);
                
                // Add aisle
                if (index === aislePosition - 1) {
                    const aisle = document.createElement('div');
                    aisle.className = 'aisle';
                    rowContainer.appendChild(aisle);
                }
            });
            
            seatMap.appendChild(rowContainer);
        });
        
        seatMapContainer.appendChild(seatMap);

        // Create selection summary
        const summary = document.createElement('div');
        summary.className = 'seat-selection-summary mt-4';
        summary.innerHTML = `
            <div class="card">
                <div class="card-body">
                    <h4 class="h6 mb-3">Selected Seats</h4>
                    <div id="selectedSeatsList" class="mb-3">
                        <p class="text-muted">No seats selected</p>
                    </div>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-muted">Total Seats:</span>
                        <span id="selectedSeatsCount">0</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mt-2">
                        <span class="text-muted">Total Price:</span>
                        <span id="totalPrice" class="h5 mb-0">$0.00</span>
                    </div>
                    <button id="confirmSeats" class="btn btn-primary w-100 mt-3" disabled>
                        Confirm Selection
                    </button>
                </div>
            </div>
        `;
        seatMapContainer.appendChild(summary);

        // Add event listener to confirm button
        const confirmButton = document.getElementById('confirmSeats');
        if (confirmButton) {
            confirmButton.addEventListener('click', () => this.confirmSelection());
        }
    }

    toggleSeat(seatElement) {
        const seatNumber = seatElement.dataset.seat;
        
        if (this.selectedSeats.has(seatNumber)) {
            this.selectedSeats.delete(seatNumber);
            seatElement.classList.remove('selected');
            seatElement.classList.add('available');
        } else {
            if (this.selectedSeats.size >= this.maxSeats) {
                // Show error message
                const errorAlert = document.createElement('div');
                errorAlert.className = 'alert alert-warning alert-dismissible fade show mt-3';
                errorAlert.innerHTML = `
                    You can only select up to ${this.maxSeats} seat(s).
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                `;
                document.getElementById('seatMap').appendChild(errorAlert);
                
                // Auto-dismiss after 3 seconds
                setTimeout(() => {
                    errorAlert.remove();
                }, 3000);
                
                return;
            }
            
            this.selectedSeats.add(seatNumber);
            seatElement.classList.remove('available');
            seatElement.classList.add('selected');
        }
        
        this.updateSelectionSummary();
    }

    updateSelectionSummary() {
        const selectedSeatsList = document.getElementById('selectedSeatsList');
        const selectedSeatsCount = document.getElementById('selectedSeatsCount');
        const totalPrice = document.getElementById('totalPrice');
        const confirmButton = document.getElementById('confirmSeats');
        
        if (!selectedSeatsList || !selectedSeatsCount || !totalPrice || !confirmButton) return;
        
        if (this.selectedSeats.size === 0) {
            selectedSeatsList.innerHTML = '<p class="text-muted">No seats selected</p>';
            selectedSeatsCount.textContent = '0';
            totalPrice.textContent = '$0.00';
            confirmButton.disabled = true;
            return;
        }
        
        // Update selected seats list
        selectedSeatsList.innerHTML = Array.from(this.selectedSeats)
            .map(seat => `<span class="badge bg-primary me-2">${seat}</span>`)
            .join('');
        
        // Update count
        selectedSeatsCount.textContent = this.selectedSeats.size;
        
        // Calculate total price (mock price per seat)
        const pricePerSeat = 50;
        const total = this.selectedSeats.size * pricePerSeat;
        totalPrice.textContent = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(total);
        
        // Enable/disable confirm button
        confirmButton.disabled = this.selectedSeats.size === 0;
    }

    confirmSelection() {
        if (this.selectedSeats.size === 0) return;
        
        // TODO: Implement seat confirmation logic
        console.log('Selected seats:', Array.from(this.selectedSeats));
        
        // Show success message
        const successAlert = document.createElement('div');
        successAlert.className = 'alert alert-success alert-dismissible fade show mt-3';
        successAlert.innerHTML = `
            Seats confirmed successfully!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        document.getElementById('seatMap').appendChild(successAlert);
        
        // Auto-dismiss after 3 seconds
        setTimeout(() => {
            successAlert.remove();
        }, 3000);
    }
}

// Initialize the seat selection handler
window.seatSelection = new SeatSelection(); 