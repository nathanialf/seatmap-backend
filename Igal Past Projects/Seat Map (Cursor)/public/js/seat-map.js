class SeatMap {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.seatMapData = null;
        this.selectedSeats = new Set();
        console.log('SeatMap initialized with container:', containerId);
        if (!this.container) {
            console.error(`Container with id "${containerId}" not found`);
            return;
        }
    }

    /**
     * Process the seat map data from the API response
     * @param {Object} seatMapResponse - The response from the seat map API
     * @returns {Object} Processed seat map data
     */
    processSeatMapData(seatMapResponse) {
        console.log('=== SEAT MAP PROCESSING START ===');
        console.log('Raw seat map response:', JSON.stringify(seatMapResponse, null, 2));
        console.log('Response keys:', Object.keys(seatMapResponse));

        // Verify if we have the expected data structure
        if (!seatMapResponse || !seatMapResponse.decks || !Array.isArray(seatMapResponse.decks)) {
            console.error('Invalid seat map response structure:', JSON.stringify(seatMapResponse, null, 2));
            throw new Error('Invalid seat map response structure');
        }

        // Get the first deck (main deck)
        const mainDeck = seatMapResponse.decks[0];
        if (!mainDeck || !mainDeck.facilities || !Array.isArray(mainDeck.facilities)) {
            console.error('Invalid deck structure:', JSON.stringify(mainDeck, null, 2));
            throw new Error('Invalid deck structure');
        }

        // Process seats - check multiple possible locations
        const seatsMap = new Map();
        const facilitiesMap = new Map();
        let seatsArray = null;
        
        console.log('=== SEARCHING FOR SEATS DATA ===');
        
        // Try different possible locations for seats data, prioritizing data.seats
        if (seatMapResponse.data && seatMapResponse.data.seats && Array.isArray(seatMapResponse.data.seats)) {
            console.log('✓ Found seats in seatMapResponse.data.seats');
            console.log(`✓ Found ${seatMapResponse.data.seats.length} seats`);
            seatsArray = seatMapResponse.data.seats;
        } else if (seatMapResponse.seats && Array.isArray(seatMapResponse.seats)) {
            console.log('✓ Found seats in seatMapResponse.seats');
            console.log(`✓ Found ${seatMapResponse.seats.length} seats`);
            seatsArray = seatMapResponse.seats;
        } else if (mainDeck.seats && Array.isArray(mainDeck.seats)) {
            console.log('✓ Found seats in mainDeck.seats');
            console.log(`✓ Found ${mainDeck.seats.length} seats`);
            seatsArray = mainDeck.seats;        
        } else {
            // Debug information to see what's actually in the response
            console.log('❌ No seats found in expected locations');
            console.log('Available properties in seatMapResponse:', Object.keys(seatMapResponse));
            
            if (seatMapResponse.data) {
                console.log('Available properties in seatMapResponse.data:', Object.keys(seatMapResponse.data));
                if (seatMapResponse.data.seats) {
                    console.log('seatMapResponse.data.seats type:', typeof seatMapResponse.data.seats);
                    console.log('seatMapResponse.data.seats isArray:', Array.isArray(seatMapResponse.data.seats));
                    console.log('seatMapResponse.data.seats length:', seatMapResponse.data.seats?.length);
                    console.log('seatMapResponse.data.seats sample:', seatMapResponse.data.seats?.[0]);
                }
            } else {
                console.log('No "data" property found in seatMapResponse');
            }
            
            if (mainDeck) {
                console.log('Available properties in mainDeck:', Object.keys(mainDeck));
            }
            
            // Look for seats in any property that might contain them
            console.log('🔍 Searching for seats in all properties...');
            const searchForSeats = (obj, path = '') => {
                if (!obj || typeof obj !== 'object') return null;
                
                for (const [key, value] of Object.entries(obj)) {
                    const currentPath = path ? `${path}.${key}` : key;
                    
                    if (key.toLowerCase().includes('seat') && Array.isArray(value) && value.length > 0) {
                        // Check if this looks like seat data
                        const firstItem = value[0];
                        if (firstItem && (firstItem.number || firstItem.seatNumber || firstItem.position)) {
                            console.log(`🎯 Found potential seats at: ${currentPath}`);
                            console.log('Sample seat data:', firstItem);
                            return value;
                        }
                    }
                    
                    if (typeof value === 'object' && value !== null) {
                        const result = searchForSeats(value, currentPath);
                        if (result) return result;
                    }
                }
                return null;
            };
            
            seatsArray = searchForSeats(seatMapResponse);
            if (seatsArray) {
                console.log(`✅ Found seats via deep search: ${seatsArray.length} seats`);
            } else {
                console.log('❌ No seats found anywhere in the response');
            }
        }
        
        // Process facilities from mainDeck.facilities
        console.log('=== PROCESSING FACILITIES ===');
        console.log(`Processing ${mainDeck.facilities.length} facilities`);
        
        mainDeck.facilities.forEach(facility => {
            console.log('Processing facility:', JSON.stringify(facility, null, 2));
            
            // Filter out unwanted facilities: BK and Galley (G)
            if (facility.code === 'BK' || facility.code === 'G') {
                console.log(`    ❌ Skipping excluded facility: ${facility.code}`);
                return;
            }
            
            // Determine position for facility
            let position = null;
            
            if (facility.row && facility.column) {
                position = `${facility.row}${facility.column}`;
            } else if (facility.coordinates && facility.coordinates.x !== undefined && facility.coordinates.y !== undefined) {
                // Convert coordinates to row/column using the same system as seats
                // For facilities, we need to map the coordinates to the actual seat row/column system
                // The coordinates.x represents the row number, coordinates.y represents the column index
                const rowNumber = facility.coordinates.x;
                const columnIndex = facility.coordinates.y;
                const col = String.fromCharCode(65 + columnIndex); // A, B, C...
                position = `${rowNumber}${col}`;
            }
            
            if (position) {
                facilitiesMap.set(position, {
                    code: facility.code,
                    position: position,
                    row: facility.row || (facility.coordinates ? facility.coordinates.x : null),
                    column: facility.column || (facility.coordinates ? String.fromCharCode(65 + facility.coordinates.y) : null),
                    coordinates: facility.coordinates,
                    type: 'facility'
                });
                console.log(`    ✅ Mapped facility ${facility.code} to position ${position}`);
            } else {
                console.warn('    ❌ Could not determine position for facility:', facility);
            }
        });
        
        console.log(`🏁 FACILITIES PROCESSING COMPLETE: ${facilitiesMap.size} facilities mapped`);
        
        // Process seats if we found them
        if (seatsArray && seatsArray.length > 0) {
            console.log('🏁 PROCESSING SEATS DATA');
            console.log(`Processing ${seatsArray.length} seats`);
            
            seatsArray.forEach(seat => {
                console.log('Processing seat:', JSON.stringify(seat, null, 2));
                
                // Determine the seat position
                let seatPosition = null;
                
                if (seat.number) {
                    seatPosition = seat.number;
                } else if (seat.seatNumber) {
                    seatPosition = seat.seatNumber;
                } else if (seat.row && seat.column) {
                    seatPosition = `${seat.row}${seat.column}`;
                } else if (seat.coordinates && seat.coordinates.x !== undefined && seat.coordinates.y !== undefined) {
                    // Convert coordinates to seat position using the same system as facilities
                    // The coordinates.x represents the row number, coordinates.y represents the column index
                    const rowNumber = seat.coordinates.x;
                    const columnIndex = seat.coordinates.y;
                    const col = String.fromCharCode(65 + columnIndex); // A, B, C...
                    seatPosition = `${rowNumber}${col}`;
                }
                
                if (seatPosition) {
                    // Determine availability status
                    let availability = 'AVAILABLE'; // default
                    
                    if (seat.travelerPricing && seat.travelerPricing[0]) {
                        const pricing = seat.travelerPricing[0];
                        if (pricing.seatAvailabilityStatus) {
                            availability = pricing.seatAvailabilityStatus;
                        }
                    }
                    
                    seatsMap.set(seatPosition, {
                        number: seatPosition,
                        availability: availability,
                        coordinates: seat.coordinates,
                        price: seat.travelerPricing?.[0]?.price,
                        travelerPricing: seat.travelerPricing,
                        cabin: seat.cabin,
                        characteristicsCodes: seat.characteristicsCodes
                    });
                    
                    console.log(`    ✅ Mapped seat ${seatPosition} with status: ${availability}`);
                } else {
                    console.warn('    ❌ Could not determine position for seat:', seat);
                }
            });
            
            console.log(`🏁 SEATS PROCESSING COMPLETE: ${seatsMap.size} seats mapped`);
        } else {
            console.warn('❌ No seats array found in response');
            console.log('Available properties in response:', Object.keys(seatMapResponse));
            if (mainDeck) {
                console.log('Available properties in mainDeck:', Object.keys(mainDeck));
            }
        }

        // Debug: Check what's in mainDeck.deckConfiguration
        console.log('🔍 MAIN DECK CONFIGURATION DEBUG:');
        console.log('  - mainDeck.deckConfiguration:', mainDeck.deckConfiguration);
        console.log('  - mainDeck.deckConfiguration properties:', Object.keys(mainDeck.deckConfiguration || {}));
        console.log('  - mainDeck.deckConfiguration full JSON:', JSON.stringify(mainDeck.deckConfiguration, null, 2));

        // Create a processed seat map object
        const processedData = {
            flight: seatMapResponse.flight || {},
            aircraft: seatMapResponse.aircraft || {},
            deck: {
                type: mainDeck.deckType,
                configuration: mainDeck.deckConfiguration,
                facilities: Array.from(facilitiesMap.values())
            },
            seats: seatsMap, // Add the seats map
            facilitiesMap: facilitiesMap // Add the facilities map
        };

        console.log('Processed seat map data (summary):');
        console.log('- Flight:', processedData.flight);
        console.log('- Aircraft:', processedData.aircraft);
        console.log('- Deck configuration:', processedData.deck.configuration);
        console.log('- Seat positions:', Array.from(seatsMap.keys()));
        console.log('- Facility positions:', Array.from(facilitiesMap.keys()));
        console.log('=== SEAT MAP PROCESSING END ===');
        return processedData;
    }

    /**
     * Create a grid representation of the cabin
     * @param {Object} dimensions - Cabin dimensions
     * @param {Array} seats - Array of seat objects
     * @param {Array} facilities - Array of facility objects
     * @returns {Array} 2D array representing the cabin layout
     */
    createCabinGrid(dimensions, seats, facilities) {
        // Crear matriz vacía basada en las dimensiones
        const grid = Array(dimensions.length).fill().map(() => 
            Array(dimensions.width).fill(null)
        );
        
        // Colocar asientos en sus coordenadas (x,y)
        seats.forEach(seat => {
            const { x, y } = seat.coordinates;
            if (grid[y] && grid[y][x] !== undefined) {
                grid[y][x] = {
                    type: 'seat',
                    data: seat
                };
            }
        });
        
        // Colocar facilities (baños, cocinas, etc.)
        facilities.forEach(facility => {
            const { x, y } = facility.coordinates;
            if (grid[y] && grid[y][x] !== undefined) {
                grid[y][x] = {
                    type: 'facility',
                    data: facility
                };
            }
        });
        
        return grid;
    }

    /**
     * Render a seat element
     * @param {Object} seat - Seat data
     * @returns {string} HTML string for the seat
     */
    renderSeat(seat) {
        const availability = seat.travelerPricing?.[0]?.seatAvailabilityStatus || 'AVAILABLE';
        const price = seat.travelerPricing?.[0]?.price;
        const seatNumber = seat.number;
        
        const priceText = price ? `€${price.total}` : 'Gratis';
        
        return `
            <div class="seat ${availability.toLowerCase()}" data-seat="${seatNumber}">
                <div class="seat-number">${seatNumber}</div>
                <div class="seat-price">${priceText}</div>
            </div>
        `;
    }

    /**
     * Render a facility element
     * @param {Object} facility - Facility data
     * @returns {string} HTML string for the facility
     */
    renderFacility(facility) {
        return `
            <div class="facility" data-type="${facility.code}">
                <div class="facility-icon">${facility.code}</div>
            </div>
        `;
    }



    /**
     * Display the seat map in the container
     * @param {Object} data - The processed seat map data
     */
    displaySeatMap(data) {
        try {
            console.log('=== DISPLAYING SEAT MAP ===');
            this.seatMapData = this.processSeatMapData(data);
            
            // Clear the container
            this.container.innerHTML = '';
            
            // Create the seat map container
            const seatMapContainer = document.createElement('div');
            seatMapContainer.className = 'seat-map-container';
            
            // Add flight information
            const flightInfo = this.createFlightInfo();
            seatMapContainer.appendChild(flightInfo);
            
            // Debug information removed for cleaner interface
            
            // Add the legend
            const legend = this.createLegend();
            seatMapContainer.appendChild(legend);
            
            // Create the seat grid
            const grid = this.createSeatGrid();
            seatMapContainer.appendChild(grid);
            
            // Add the container to the DOM
            this.container.appendChild(seatMapContainer);
            
            console.log('Seat map displayed successfully');
        } catch (error) {
            console.error('Error displaying seat map:', error);
            this.container.innerHTML = `<div class="alert alert-danger">Error displaying seat map: ${error.message}</div>`;
        }
    }

    createFlightInfo() {
        const flightInfo = document.createElement('div');
        flightInfo.className = 'flight-info mb-4';
        flightInfo.innerHTML = `
            <h3 class="h5">Flight Information</h3>
            <div class="row">
                <div class="col-md-6">
                    <p><strong>Flight:</strong> ${this.seatMapData.flight.number || 'N/A'}</p>
                    <p><strong>From:</strong> ${this.seatMapData.flight.departure?.iataCode || 'N/A'}</p>
                    <p><strong>To:</strong> ${this.seatMapData.flight.arrival?.iataCode || 'N/A'}</p>
                </div>
                <div class="col-md-6">
                    <p><strong>Aircraft:</strong> ${this.seatMapData.aircraft.code || 'N/A'}</p>
                    <p><strong>Class:</strong> ${this.seatMapData.class || 'N/A'}</p>
                </div>
            </div>
        `;
        return flightInfo;
    }

    createLegend() {
        const legend = document.createElement('div');
        legend.className = 'seat-map-legend d-flex flex-wrap justify-content-center gap-3 mt-3';
        
        legend.innerHTML = `
            <div class="legend-item d-flex align-items-center gap-2">
                <div class="seat available" style="width: 20px; height: 20px;">
                    <span style="font-size: 0.6rem;">A</span>
                </div>
                <span>Available</span>
            </div>
            <div class="legend-item d-flex align-items-center gap-2">
                <div class="seat selected" style="width: 20px; height: 20px;">
                    <span style="font-size: 0.6rem;">S</span>
                </div>
                <span>Selected</span>
            </div>
            <div class="legend-item d-flex align-items-center gap-2">
                <div class="seat occupied" style="width: 20px; height: 20px;">
                    <span style="font-size: 0.6rem;">O</span>
                </div>
                <span>Occupied</span>
            </div>
            <div class="legend-item d-flex align-items-center gap-2">
                <div class="seat blocked" style="width: 20px; height: 20px;">
                    <span style="font-size: 0.6rem;">B</span>
                </div>
                <span>Blocked</span>
            </div>

            <div class="legend-item d-flex align-items-center gap-2">
                <div class="seat exit-row-seat" style="width: 20px; height: 20px; border: 2px dashed #ffc107;">
                    <span style="font-size: 0.6rem;">E</span>
                </div>
                <span>Emergency Exit Row</span>
            </div>
            <div class="legend-item d-flex align-items-center gap-2">
                <div class="facility lavatory" style="width: 20px; height: 20px;">
                    <span style="font-size: 0.8rem;">🚻</span>
                </div>
                <span>Lavatory</span>
            </div>
        `;
        return legend;
    }

    /**
     * Analyze seat characteristics to determine cabin layout
     * @param {Map} seats - Map of seat data
     * @param {Object} config - Deck configuration
     * @returns {Object} Layout information with aisles and groupings
     */
    analyzeCabinLayout(seats, config) {
        console.log('=== ANALYZING CABIN LAYOUT (Row-by-Row Processing) ===');
        
        // Collect all rows and columns first
        const allRows = new Set();
        const allColumns = new Set();
        let maxColumn = 0;
        
        for (const [seatNumber, seatData] of seats) {
            const row = this.extractRowFromSeatNumber(seatNumber);
            const column = this.extractColumnFromSeatNumber(seatNumber);
            const columnNum = this.columnLetterToNumber(column);
            
            if (row && column) {
                allRows.add(row);
                allColumns.add(column);
                maxColumn = Math.max(maxColumn, columnNum);
            }
        }
        
        const sortedRows = Array.from(allRows).sort((a, b) => a - b);
        const sortedColumns = Array.from(allColumns).sort();
        
        console.log(`Processing ${sortedRows.length} rows with columns:`, sortedColumns);
        
        const layoutInfo = {
            rowAnalysis: new Map(),      // Analysis for each row
            leftSide: new Map(),         // Left side seats by row
            centerSide: new Map(),       // Center section seats by row
            rightSide: new Map(),        // Right side seats by row
            aislePositions: new Set(),   // Global aisle positions
            columnsByRow: new Map(),     // Columns used in each row
            maxColumn: maxColumn
        };
        
        // Process each row individually
        sortedRows.forEach(row => {
            console.log(`\n=== ANALYZING ROW ${row} ===`);
            
            // Get all seats in this row
            const rowSeats = new Map();
            for (const [seatNumber, seatData] of seats) {
                const seatRow = this.extractRowFromSeatNumber(seatNumber);
                if (seatRow === row) {
                    const column = this.extractColumnFromSeatNumber(seatNumber);
                    rowSeats.set(column, { seatNumber, ...seatData });
                }
            }
            
            const rowColumns = Array.from(rowSeats.keys()).sort();
            console.log(`Row ${row} seats:`, rowColumns.map(col => `${row}${col}`));
            
            // Store columns by row for rendering
            layoutInfo.columnsByRow.set(row, new Set(rowColumns));
            
            const rowLeftSide = [];
            const rowCenterSide = [];
            const rowRightSide = [];
            const rowAisles = new Set();
            
            // First pass: Identify aisle positions
            const aisleColumns = [];
            rowColumns.forEach((column, index) => {
                const seatData = rowSeats.get(column);
                const codes = seatData.characteristicsCodes || [];
                
                if (codes.includes('A')) {
                    aisleColumns.push(column);
                    console.log(`  Found aisle seat: ${column}`);
                }
            });
            
            console.log(`  Aisle seats in row ${row}: [${aisleColumns.join(',')}]`);
            
            // Second pass: Divide seats into sections based on aisles
            let currentSection = 'left';
            let aisleIndex = 0;
            
            rowColumns.forEach((column, index) => {
                const seatData = rowSeats.get(column);
                const codes = seatData.characteristicsCodes || [];
                const seatNumber = `${row}${column}`;
                
                console.log(`  Processing seat ${seatNumber}: codes=[${codes.join(',')}], section=${currentSection}`);
                
                // Add seat to current section
                if (currentSection === 'left') {
                    rowLeftSide.push(column);
                } else if (currentSection === 'center') {
                    rowCenterSide.push(column);
                } else {
                    rowRightSide.push(column);
                }
                
                // Check if this seat creates an aisle (has 'A' code)
                if (codes.includes('A')) {
                    // Create aisle after this seat
                    const nextColumn = rowColumns[index + 1];
                    if (nextColumn) {
                        const aislePosition = `${column}-${nextColumn}`;
                        rowAisles.add(aislePosition);
                        layoutInfo.aislePositions.add(aislePosition);
                        console.log(`    → AISLE after ${column}`);
                        
                        // Move to next section
                        if (currentSection === 'left') {
                            currentSection = 'center';
                            console.log(`    → Switching to CENTER section`);
                        } else if (currentSection === 'center') {
                            currentSection = 'right';
                            console.log(`    → Switching to RIGHT section`);
                        }
                    }
                }
            });
            
            // Store row analysis
            layoutInfo.rowAnalysis.set(row, {
                seats: rowSeats,
                leftSide: rowLeftSide,
                centerSide: rowCenterSide,
                rightSide: rowRightSide,
                aisles: rowAisles
            });
            
            // Store in layout for rendering
            if (rowLeftSide.length > 0) {
                layoutInfo.leftSide.set(row, rowLeftSide);
            }
            if (rowCenterSide.length > 0) {
                layoutInfo.centerSide.set(row, rowCenterSide);
            }
            if (rowRightSide.length > 0) {
                layoutInfo.rightSide.set(row, rowRightSide);
            }
            
            console.log(`  Row ${row} final:`);
            console.log(`    LEFT=[${rowLeftSide.join(',')}]`);
            console.log(`    CENTER=[${rowCenterSide.join(',')}]`);
            console.log(`    RIGHT=[${rowRightSide.join(',')}]`);
            console.log(`    AISLES=[${Array.from(rowAisles).join(',')}]`);
        });
        
        // Create compatible structure for existing rendering code
        const compatibleLayout = {
            leftSide: layoutInfo.leftSide,
            centerSide: layoutInfo.centerSide,
            rightSide: layoutInfo.rightSide,
            aisleColumns: new Set(),
            windowColumns: new Set(),
            centerColumns: new Set(),
            columnsByRow: layoutInfo.columnsByRow,
            maxColumn: layoutInfo.maxColumn,
            columnOrder: sortedColumns,
            aislePositions: layoutInfo.aislePositions,
            rowAnalysis: layoutInfo.rowAnalysis
        };
        
        // Track column types from all seats
        for (const [seatNumber, seatData] of seats) {
            const column = this.extractColumnFromSeatNumber(seatNumber);
            const codes = seatData.characteristicsCodes || [];
            
            if (codes.includes('W')) compatibleLayout.windowColumns.add(column);
            if (codes.includes('9')) compatibleLayout.centerColumns.add(column);
            if (codes.includes('A')) compatibleLayout.aisleColumns.add(column);
        }
        
        console.log('\n=== FINAL LAYOUT SUMMARY ===');
        console.log('Total rows processed:', sortedRows.length);
        console.log('Global aisles:', Array.from(layoutInfo.aislePositions));
        console.log('Sample left side:', Array.from(layoutInfo.leftSide.entries()).slice(0, 3));
        console.log('Sample center side:', Array.from(layoutInfo.centerSide.entries()).slice(0, 3));
        console.log('Sample right side:', Array.from(layoutInfo.rightSide.entries()).slice(0, 3));
        console.log('Aisle columns:', Array.from(compatibleLayout.aisleColumns));
        console.log('Window columns:', Array.from(compatibleLayout.windowColumns));
        console.log('Center columns:', Array.from(compatibleLayout.centerColumns));
        
        return compatibleLayout;
    }

    /**
     * Validate and correct seat positioning based on column position
     * Handles cases like row 50 where seat H should always be on the right side
     */
    validateAndCorrectSeatPositioning(layoutInfo, seats) {
        console.log('=== VALIDATING SEAT POSITIONING ===');
        
        // Process all rows that have any seats (left or right)
        const allRowsWithSeats = new Set([
            ...layoutInfo.leftSide.keys(),
            ...layoutInfo.rightSide.keys()
        ]);
        
        for (const row of allRowsWithSeats) {
            const leftColumns = layoutInfo.leftSide.get(row) || [];
            const rightColumns = layoutInfo.rightSide.get(row) || [];
            
            // Check for seats that should be moved from left to right
            const toMoveToRight = [];
            leftColumns.forEach((column, index) => {
                const columnNum = this.columnLetterToNumber(column);
                
                // If column is E or higher (5+), it should be on the right side
                if (columnNum >= 5) {
                    console.log(`⚠️ Moving seat ${row}${column} from LEFT to RIGHT (column ${columnNum} >= 5)`);
                    toMoveToRight.push(column);
                }
            });
            
            // Remove from left and add to right
            toMoveToRight.forEach(column => {
                const leftIndex = leftColumns.indexOf(column);
                if (leftIndex > -1) {
                    leftColumns.splice(leftIndex, 1);
                    rightColumns.push(column);
                }
            });
            
            // Check for seats that should be moved from right to left
            const toMoveToLeft = [];
            rightColumns.forEach((column, index) => {
                const columnNum = this.columnLetterToNumber(column);
                
                // If column is D or lower (4-), it should be on the left side
                if (columnNum <= 4) {
                    console.log(`⚠️ Moving seat ${row}${column} from RIGHT to LEFT (column ${columnNum} <= 4)`);
                    toMoveToLeft.push(column);
                }
            });
            
            // Remove from right and add to left
            toMoveToLeft.forEach(column => {
                const rightIndex = rightColumns.indexOf(column);
                if (rightIndex > -1) {
                    rightColumns.splice(rightIndex, 1);
                    leftColumns.push(column);
                }
            });
            
            // Update the layout info
            if (leftColumns.length > 0) {
                layoutInfo.leftSide.set(row, leftColumns.sort());
            } else {
                layoutInfo.leftSide.delete(row);
            }
            
            if (rightColumns.length > 0) {
                layoutInfo.rightSide.set(row, rightColumns.sort());
            } else {
                layoutInfo.rightSide.delete(row);
            }
            
            // Log the final configuration for this row
            if (leftColumns.length > 0 || rightColumns.length > 0) {
                console.log(`✅ Row ${row}: Left=[${leftColumns.join(',')}] Right=[${rightColumns.join(',')}]`);
            }
        }
        
        console.log('=== SEAT POSITIONING VALIDATION COMPLETE ===');
    }

    /**
     * Helper function to extract row number from seat number (e.g., "51F" -> 51)
     */
    extractRowFromSeatNumber(seatNumber) {
        const match = seatNumber.match(/^(\d+)[A-Z]$/);
        return match ? parseInt(match[1]) : null;
    }
    
    /**
     * Helper function to extract column letter from seat number (e.g., "51F" -> "F")
     */
    extractColumnFromSeatNumber(seatNumber) {
        const match = seatNumber.match(/^\d+([A-Z])$/);
        return match ? match[1] : null;
    }
    
    /**
     * Helper function to convert column letter to number (A=1, B=2, etc.)
     */
    columnLetterToNumber(letter) {
        return letter.charCodeAt(0) - 64; // A=1, B=2, etc.
    }

    createSeatGrid() {
        const grid = document.createElement('div');
        grid.className = 'seat-grid';
        
        const config = this.seatMapData.deck.configuration;
        const seats = this.seatMapData.seats; // Get the seats map
        const facilities = this.seatMapData.facilitiesMap; // Get the facilities map
        
        console.log('Creating seat grid using coordinates-based approach');
        console.log('Available seats data:', seats);
        console.log('Available facilities data:', facilities);
        
        // Extract coordinates from all seats and facilities
        const coordinatesMap = new Map(); // Map of "x,y" -> seat/facility data
        const rowNumberMap = new Map(); // Map of x coordinate -> real row number
        let minX = Infinity, maxX = -Infinity;
        let minY = Infinity, maxY = -Infinity;
        
        // Process seats using coordinates
        for (const [seatPosition, seatData] of seats) {
            if (seatData.coordinates && seatData.coordinates.x !== undefined && seatData.coordinates.y !== undefined) {
                const x = seatData.coordinates.x;
                const y = seatData.coordinates.y;
                const coordKey = `${x},${y}`;
                
                // Extract real row number from seat position (e.g., "15A" -> 15)
                const realRowNumber = this.extractRowFromSeatNumber(seatPosition);
                if (realRowNumber) {
                    rowNumberMap.set(x, realRowNumber);
                }
                
                coordinatesMap.set(coordKey, {
                    type: 'seat',
                    data: seatData,
                    position: seatPosition,
                    x: x,
                    y: y
                });
                
                // Track bounds
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                
                console.log(`Seat ${seatPosition} at coordinates (${x}, ${y}) - real row ${realRowNumber}`);
            }
        }
        
        // Process facilities using coordinates
        for (const [facilityPosition, facilityData] of facilities) {
            if (facilityData.coordinates && facilityData.coordinates.x !== undefined && facilityData.coordinates.y !== undefined) {
                const x = facilityData.coordinates.x;
                const y = facilityData.coordinates.y;
                const coordKey = `${x},${y}`;
                
                coordinatesMap.set(coordKey, {
                    type: 'facility',
                    data: facilityData,
                    position: facilityPosition,
                    x: x,
                    y: y
                });
                
                // Track bounds
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                
                console.log(`Facility ${facilityData.code} at coordinates (${x}, ${y})`);
            }
        }
        
        console.log(`Grid bounds: X(${minX} to ${maxX}), Y(${minY} to ${maxY})`);
        console.log(`Grid dimensions: ${maxX - minX + 1} rows × ${maxY - minY + 1} columns`);
        
        // Create grid based on actual coordinates
        for (let x = minX; x <= maxX; x++) {
            const rowElement = document.createElement('div');
            rowElement.className = 'seat-row coordinates-layout';
            rowElement.dataset.row = x;
            
            // Check row properties
            const isExitRow = config?.exitRowsX?.includes(x) || false;
            
            if (isExitRow) {
                rowElement.classList.add('exit-row');
            }
            
            // Add row label (actual deck row number)
            const rowLabel = document.createElement('div');
            rowLabel.className = 'row-label';
            
            // Get the real row number for this x coordinate
            const realRowNumber = rowNumberMap.get(x) || x;
            
            if (isExitRow) {
                rowLabel.classList.add('exit-row-label');
                rowLabel.innerHTML = `${realRowNumber}<br><span class="exit-text">EXIT</span>`;
            } else {
                rowLabel.textContent = realRowNumber;
            }
            rowElement.appendChild(rowLabel);
            
            // Check if this row has any seats or facilities
            let hasContent = false;
            for (let y = minY; y <= maxY; y++) {
                const coordKey = `${x},${y}`;
                if (coordinatesMap.has(coordKey)) {
                    hasContent = true;
                    break;
                }
            }
            
            if (!hasContent) {
                console.log(`Skipping empty row ${x} (real row ${realRowNumber})`);
                continue;
            }
            
            // Detect aisles by finding gaps in seat arrangement
            const occupiedColumns = [];
            const aislePositions = new Set();
            
            for (let y = minY; y <= maxY; y++) {
                const coordKey = `${x},${y}`;
                if (coordinatesMap.has(coordKey)) {
                    occupiedColumns.push(y);
                }
            }
            
            // Find gaps between occupied columns (these are aisles)
            for (let i = 0; i < occupiedColumns.length - 1; i++) {
                const currentCol = occupiedColumns[i];
                const nextCol = occupiedColumns[i + 1];
                
                // If there's a gap larger than 1 between columns, it's an aisle
                if (nextCol - currentCol > 1) {
                    // Mark aisle positions between the columns
                    for (let aisleY = currentCol + 1; aisleY < nextCol; aisleY++) {
                        aislePositions.add(aisleY);
                    }
                    console.log(`Row ${x} (real row ${realRowNumber}): Found aisle between columns ${currentCol} and ${nextCol}`);
                }
            }
            
            // Create columns for this row
            for (let y = minY; y <= maxY; y++) {
                const coordKey = `${x},${y}`;
                
                // Check if this position is an aisle
                if (aislePositions.has(y)) {
                    const aisleElement = document.createElement('div');
                    aisleElement.className = 'aisle center-aisle';
                    aisleElement.innerHTML = '<div class="aisle-space"></div>';
                    rowElement.appendChild(aisleElement);
                    continue;
                }
                
                // Check if there's a seat or facility at this position
                if (coordinatesMap.has(coordKey)) {
                    const item = coordinatesMap.get(coordKey);
                    const cellElement = this.createCoordinateBasedElement(item, isExitRow);
                    rowElement.appendChild(cellElement);
                } else {
                    // Empty position - add for all positions in the global range
                    const emptyElement = document.createElement('div');
                    emptyElement.className = 'seat-cell empty';
                    emptyElement.innerHTML = '<div class="empty-seat"></div>';
                    rowElement.appendChild(emptyElement);
                }
            }
            
            grid.appendChild(rowElement);
        }
        
        console.log('Coordinates-based seat grid created successfully');
        return grid;
    }

    /**
     * Create a seat/facility element based on coordinate data
     */
    createCoordinateBasedElement(item, isExitRow) {
        const cellElement = document.createElement('div');
        cellElement.className = 'seat-cell';
        cellElement.dataset.coordinates = `${item.x},${item.y}`;
        
        if (isExitRow) {
            cellElement.classList.add('exit-row-cell');
        }
        
        if (item.type === 'facility') {
            const facilityData = item.data;
            const facilityType = this.getFacilityType(facilityData.code);
            const facilityIcon = this.getFacilityIcon(facilityData.code);
            const facilityName = this.getFacilityName(facilityData.code);
            
            cellElement.innerHTML = `
                <div class="facility ${facilityType}" 
                     data-facility-code="${facilityData.code}"
                     data-coordinates="${item.x},${item.y}"
                     title="${facilityName}">
                    <div class="facility-icon">${facilityIcon}</div>
                    <div class="facility-code">${facilityData.code}</div>
                </div>
            `;
        } else if (item.type === 'seat') {
            const seatData = item.data;
            const availability = seatData.availability.toLowerCase();
            const price = seatData.price;
            const priceText = price ? `€${price.total}` : '';
            const characteristics = seatData.characteristicsCodes || [];
            
            // Use the actual seat position letter instead of converting from Y coordinate
            const columnLetter = this.extractColumnFromSeatNumber(item.position);
            
            cellElement.innerHTML = `
                <div class="seat ${availability} ${isExitRow ? 'exit-row-seat' : ''}" 
                     data-seat-id="${item.position}" 
                     data-coordinates="${item.x},${item.y}"
                     data-availability="${seatData.availability}"
                     data-characteristics="${characteristics.join(',')}"
                     title="Seat ${item.position} (${item.x},${item.y})">
                    <div class="seat-number">${columnLetter}</div>
                </div>
            `;
            
            // Add event listener for available seats
            if (seatData.availability === 'AVAILABLE') {
                const seatElement = cellElement.querySelector('.seat');
                seatElement.addEventListener('click', () => this.toggleSeatSelection(item.position));
            }
        }
        
        return cellElement;
    }

    /**
     * Get facility type CSS class based on facility code
     * @param {string} code - Facility code (G, LA, etc.)
     * @returns {string} CSS class name
     */
    getFacilityType(code) {
        switch (code) {
            case 'LA':
                return 'lavatory';
            case 'G':
                return 'galley';
            case 'E':
                return 'exit';
            case 'CL':
                return 'closet';
            case 'ST':
                return 'stairs';
            default:
                return 'facility-other';
        }
    }

    /**
     * Get facility icon based on facility code
     * @param {string} code - Facility code (G, LA, etc.)
     * @returns {string} Icon/emoji
     */
    getFacilityIcon(code) {
        switch (code) {
            case 'LA':
                return '🚻';
            case 'G':
                return '🍽️';
            case 'E':
                return '🚪';
            case 'CL':
                return '🗄️';
            case 'ST':
                return '🪜';
            default:
                return '⚪';
        }
    }

    /**
     * Get facility name based on facility code
     * @param {string} code - Facility code (G, LA, etc.)
     * @returns {string} Facility name
     */
    getFacilityName(code) {
        switch (code) {
            case 'LA':
                return 'Lavatory (Baño)';
            case 'G':
                return 'Galley (Cocina)';
            case 'E':
                return 'Emergency Exit (Salida de Emergencia)';
            case 'CL':
                return 'Closet (Armario)';
            case 'ST':
                return 'Stairs (Escaleras)';
            default:
                return 'Facility (Instalación)';
        }
    }

    toggleSeatSelection(seatId) {
        const seatElement = document.querySelector(`[data-seat-id="${seatId}"]`);
        if (!seatElement || seatElement.classList.contains('lavatory') || seatElement.classList.contains('galley')) {
            return;
        }
        
        if (this.selectedSeats.has(seatId)) {
            this.selectedSeats.delete(seatId);
            seatElement.classList.remove('selected');
            seatElement.classList.add('available');
        } else {
            this.selectedSeats.add(seatId);
            seatElement.classList.remove('available');
            seatElement.classList.add('selected');
        }
        
        // Dispatch seat selection changed event
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

// Export the class for global use
window.SeatMap = SeatMap; 