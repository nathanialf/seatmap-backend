class SeatMap {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.seatMapData = null;
        this.selectedSeats = new Set();
        console.log('SeatMap initialized with container:', containerId);
    }

    processSeatMapData(seatMapResponse) {
        console.log('=== SEAT MAP PROCESSING START ===');
        console.log('Raw seat map response:', JSON.stringify(seatMapResponse, null, 2));
        console.log('Response type:', typeof seatMapResponse);
        console.log('Response keys:', Object.keys(seatMapResponse));
        
        // Verificar si la respuesta tiene la estructura esperada
        if (!seatMapResponse || !seatMapResponse.decks || !Array.isArray(seatMapResponse.decks)) {
            console.error('Invalid seat map response structure:', JSON.stringify(seatMapResponse, null, 2));
            throw new Error('Invalid seat map response structure');
        }

        // Procesar el primer deck (asumimos que es el principal)
        const mainDeck = seatMapResponse.decks[0];
        console.log('Main deck data:', JSON.stringify(mainDeck, null, 2));
        
        if (!mainDeck || !mainDeck.facilities || !Array.isArray(mainDeck.facilities)) {
            console.error('Invalid deck structure:', JSON.stringify(mainDeck, null, 2));
            throw new Error('Invalid deck structure');
        }

        // Crear un mapa de asientos basado en las instalaciones
        const seatMap = {
            flight: seatMapResponse.flight || {},
            aircraft: seatMapResponse.aircraft || {},
            seats: new Map(),
            rows: new Set(),
            columns: new Set()
        };

        console.log('Processing facilities...');
        // Procesar las instalaciones para crear el mapa de asientos
        mainDeck.facilities.forEach(facility => {
            console.log('Processing facility:', JSON.stringify(facility, null, 2));
            if (facility.type === 'SEAT') {
                const seatId = `${facility.row}${facility.column}`;
                seatMap.seats.set(seatId, {
                    id: seatId,
                    row: facility.row,
                    column: facility.column,
                    position: facility.position,
                    coordinates: facility.coordinates,
                    type: facility.type,
                    status: 'AVAILABLE' // Por defecto, todos los asientos están disponibles
                });
                seatMap.rows.add(facility.row);
                seatMap.columns.add(facility.column);
            }
        });

        // Convertir Sets a Arrays ordenados
        seatMap.rows = Array.from(seatMap.rows).sort();
        seatMap.columns = Array.from(seatMap.columns).sort();

        console.log('Final processed seat map:', JSON.stringify(seatMap, null, 2));
        console.log('=== SEAT MAP PROCESSING END ===');
        return seatMap;
    }

    displaySeatMap(data) {
        try {
            console.log('=== DISPLAYING SEAT MAP ===');
            console.log('Input data:', JSON.stringify(data, null, 2));
            this.seatMapData = this.processSeatMapData(data);
            
            // Limpiar el contenedor
            this.container.innerHTML = '';
            
            // Crear el contenedor del mapa de asientos
            const seatMapContainer = document.createElement('div');
            seatMapContainer.className = 'seat-map-container';
            
            // Crear la leyenda
            const legend = this.createLegend();
            seatMapContainer.appendChild(legend);
            
            // Crear la cuadrícula de asientos
            const grid = this.createSeatGrid();
            seatMapContainer.appendChild(grid);
            
            // Agregar el contenedor al DOM
            this.container.appendChild(seatMapContainer);
            
            console.log('Seat map displayed successfully');
            console.log('=== END DISPLAYING SEAT MAP ===');
        } catch (error) {
            console.error('=== ERROR DISPLAYING SEAT MAP ===');
            console.error('Error:', error);
            console.error('Error stack:', error.stack);
            console.error('=== END ERROR LOG ===');
            this.container.innerHTML = `<div class="error-message">Error displaying seat map: ${error.message}</div>`;
        }
    }

    createLegend() {
        const legend = document.createElement('div');
        legend.className = 'seat-map-legend';
        legend.innerHTML = `
            <div class="legend-item">
                <div class="seat available"></div>
                <span>Available</span>
            </div>
            <div class="legend-item">
                <div class="seat selected"></div>
                <span>Selected</span>
            </div>
            <div class="legend-item">
                <div class="seat occupied"></div>
                <span>Occupied</span>
            </div>
        `;
        return legend;
    }

    createSeatGrid() {
        const grid = document.createElement('div');
        grid.className = 'seat-grid';
        
        // Crear las filas de asientos
        this.seatMapData.rows.forEach(row => {
            const rowElement = document.createElement('div');
            rowElement.className = 'seat-row';
            
            // Agregar el número de fila
            const rowLabel = document.createElement('div');
            rowLabel.className = 'row-label';
            rowLabel.textContent = row;
            rowElement.appendChild(rowLabel);
            
            // Agregar los asientos
            this.seatMapData.columns.forEach(column => {
                const seatId = `${row}${column}`;
                const seat = this.seatMapData.seats.get(seatId);
                
                if (seat) {
                    const seatElement = document.createElement('div');
                    seatElement.className = `seat ${seat.status.toLowerCase()}`;
                    seatElement.dataset.seatId = seatId;
                    seatElement.textContent = column;
                    
                    seatElement.addEventListener('click', () => this.toggleSeatSelection(seatId));
                    rowElement.appendChild(seatElement);
                }
            });
            
            grid.appendChild(rowElement);
        });
        
        return grid;
    }

    toggleSeatSelection(seatId) {
        const seat = this.seatMapData.seats.get(seatId);
        if (!seat) return;
        
        const seatElement = document.querySelector(`[data-seat-id="${seatId}"]`);
        if (!seatElement) return;
        
        if (this.selectedSeats.has(seatId)) {
            this.selectedSeats.delete(seatId);
            seatElement.classList.remove('selected');
            seatElement.classList.add('available');
        } else {
            this.selectedSeats.add(seatId);
            seatElement.classList.remove('available');
            seatElement.classList.add('selected');
        }
        
        // Disparar evento de cambio de selección
        const event = new CustomEvent('seatSelectionChanged', {
            detail: {
                selectedSeats: Array.from(this.selectedSeats)
            }
        });
        this.container.dispatchEvent(event);
    }

    getSelectedSeats() {
        return Array.from(this.selectedSeats);
    }
} 