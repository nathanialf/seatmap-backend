const { getAmadeusClient } = require('./amadeus-auth');
const fs = require('fs');
const path = require('path');

/**
 * Process seat map data from Amadeus API
 * @param {Object} seatMapResponse - Raw seat map data from Amadeus
 * @returns {Object} Processed seat map data
 */
function processSeatMapData(seatMapResponse) {
    writeLog('\n=== PROCESSING SEAT MAP DATA ===');
    writeLog('seatMapResponse type: ' + typeof seatMapResponse);
    writeLog('seatMapResponse: ' + JSON.stringify(seatMapResponse, null, 2));

    // Verificar si tenemos datos válidos
    if (!seatMapResponse) {
        writeLog('Invalid seat map response - response is null or undefined');
        throw new Error('Invalid seat map data structure');
    }

    // Si seatMapResponse ya es el objeto de datos que necesitamos
    if (seatMapResponse.id && seatMapResponse.type === 'seatmap') {
        writeLog('Processing direct seat map data');
        return processSeatMapObject(seatMapResponse);
    }

    // Si seatMapResponse tiene una propiedad data
    if (seatMapResponse.data) {
        writeLog('Processing seat map data from response.data');
        if (Array.isArray(seatMapResponse.data)) {
            if (seatMapResponse.data.length === 0) {
                writeLog('No seat map data found in array');
                throw new Error('No seat map data available');
            }
            return processSeatMapObject(seatMapResponse.data[0]);
        } else {
            return processSeatMapObject(seatMapResponse.data);
        }
    }

    writeLog('Invalid seat map response structure');
    throw new Error('Invalid seat map data structure');
}

function processSeatMapObject(seatMap) {
    writeLog('Processing seat map object: ' + JSON.stringify(seatMap, null, 2));

    if (!seatMap.decks || !Array.isArray(seatMap.decks)) {
        writeLog('Invalid seat map structure - missing decks array');
        throw new Error('Invalid seat map structure');
    }

    // Procesar cada deck y extraer asientos
    let allSeats = []; // Array para recopilar todos los asientos de todos los decks
    
    const processedDecks = seatMap.decks.map(deck => {
        writeLog('Processing deck: ' + JSON.stringify(deck, null, 2));
        
        if (!deck.facilities || !Array.isArray(deck.facilities)) {
            writeLog('Invalid deck structure - missing facilities array');
            return null;
        }

        // Procesar las facilidades (asientos)
        const processedFacilities = deck.facilities.map(facility => {
            writeLog('Processing facility: ' + JSON.stringify(facility, null, 2));
            
            if (!facility.coordinates || !facility.coordinates.x || !facility.coordinates.y) {
                writeLog('Invalid facility coordinates');
                return null;
            }

            return {
                code: facility.code,
                column: facility.column,
                row: facility.row,
                position: facility.position,
                coordinates: {
                    x: facility.coordinates.x,
                    y: facility.coordinates.y
                }
            };
        }).filter(Boolean);

        // Extraer asientos de este deck si están disponibles
        if (deck.seats && Array.isArray(deck.seats)) {
            writeLog('Found seats in deck: ' + deck.seats.length + ' seats');
            deck.seats.forEach(seat => {
                writeLog('Processing seat from deck: ' + JSON.stringify(seat, null, 2));
                allSeats.push({
                    cabin: seat.cabin,
                    number: seat.number,
                    characteristicsCodes: seat.characteristicsCodes,
                    travelerPricing: seat.travelerPricing,
                    coordinates: seat.coordinates
                });
            });
        } else {
            writeLog('No seats found in this deck');
        }

        return {
            deckType: deck.deckType,
            deckConfiguration: deck.deckConfiguration,
            facilities: processedFacilities
        };
    }).filter(Boolean);

    writeLog('Total seats collected from all decks: ' + allSeats.length);

    const processedData = {
        flight: {
            number: seatMap.number,
            departure: {
                iataCode: seatMap.departure.iataCode,
                terminal: seatMap.departure.terminal,
                at: seatMap.departure.at
            },
            arrival: {
                iataCode: seatMap.arrival.iataCode,
                terminal: seatMap.arrival.terminal,
                at: seatMap.arrival.at
            },
            carrierCode: seatMap.carrierCode,
            operating: seatMap.operating
        },
        aircraft: {
            code: seatMap.aircraft.code
        },
        class: seatMap.class,
        flightOfferId: seatMap.flightOfferId,
        segmentId: seatMap.segmentId,
        decks: processedDecks,
        seats: allSeats // ← USAR LOS ASIENTOS EXTRAÍDOS DE LOS DECKS
    };

    writeLog('Processed seat map data: ' + JSON.stringify(processedData, null, 2));
    writeLog('Total seats in processed data: ' + allSeats.length);
    writeLog('=== END PROCESSING SEAT MAP DATA ===\n');

    return processedData;
}

// Función para escribir logs en un archivo
function writeLog(message) {
    const logDir = path.join(__dirname, '../logs');
    const logFile = path.join(logDir, 'seat-map.log');
    
    // Crear el directorio de logs si no existe
    if (!fs.existsSync(logDir)) {
        fs.mkdirSync(logDir, { recursive: true });
    }
    
    // Agregar timestamp al mensaje
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] ${message}\n`;
    
    // Escribir en el archivo
    fs.appendFileSync(logFile, logMessage);
    
    // También mostrar en consola
    console.log(message);
}

/**
 * Get seat map information for a flight offer
 * @param {Object} flightOffer - The flight offer object
 * @returns {Promise<Object>} Processed seat map data
 */
async function getSeatMap(flightOffer) {
    try {
        writeLog('\n=== SEAT MAP SERVICE START ===');
        writeLog('Flight offer received: ' + JSON.stringify(flightOffer, null, 2));
        
        const amadeus = getAmadeusClient();
        writeLog('Amadeus client initialized');
        
        // Asegurarnos de que el flightOffer tenga el formato correcto
        const formattedFlightOffer = {
            type: flightOffer.type,
            id: flightOffer.id,
            source: flightOffer.source,
            instantTicketingRequired: flightOffer.instantTicketingRequired,
            nonHomogeneous: flightOffer.nonHomogeneous,
            oneWay: flightOffer.oneWay,
            lastTicketingDate: flightOffer.lastTicketingDate,
            lastTicketingDateTime: flightOffer.lastTicketingDateTime,
            numberOfBookableSeats: flightOffer.numberOfBookableSeats,
            itineraries: flightOffer.itineraries,
            price: flightOffer.price,
            travelerPricings: flightOffer.travelerPricings
        };

        writeLog('Formatted flight offer: ' + JSON.stringify(formattedFlightOffer, null, 2));
        writeLog('Sending request to Amadeus API...');

        // Convertir el objeto a JSON string antes de enviarlo
        const requestBody = JSON.stringify({
            data: [formattedFlightOffer]
        });

        writeLog('Request body: ' + requestBody);

        try {
            // Enviar la petición con el cuerpo como string
            writeLog('Making request to Amadeus API...');
            const response = await amadeus.shopping.seatmaps.post(requestBody);
            writeLog('Received response from Amadeus API');

            writeLog('=== AMADEUS API RESPONSE ===');
            writeLog('Response status: ' + response.statusCode);
            writeLog('Response headers: ' + JSON.stringify(response.headers, null, 2));
            writeLog('Response data type: ' + typeof response.data);
            writeLog('Response data: ' + JSON.stringify(response.data, null, 2));

            if (!response || !response.data) {
                writeLog('Invalid response from Amadeus API - no data received');
                throw new Error('Invalid response from Amadeus API');
            }

            // Verificar la estructura de la respuesta
            let seatMapData;
            writeLog('Checking response structure...');
            
            if (Array.isArray(response.data)) {
                writeLog('Response is an array');
                seatMapData = response.data;
            } else if (response.data.data && Array.isArray(response.data.data)) {
                writeLog('Response has data array');
                seatMapData = response.data.data;
            } else if (response.data.seatmaps && Array.isArray(response.data.seatmaps)) {
                writeLog('Response has seatmaps array');
                seatMapData = response.data.seatmaps;
            } else {
                writeLog('Unexpected response structure: ' + JSON.stringify(response.data, null, 2));
                throw new Error('Invalid response structure from Amadeus API');
            }

            // Procesar los datos del mapa de asientos
            writeLog('Processing seat map data...');
            const processedData = processSeatMapData({ data: seatMapData });
            writeLog('Processed seat map data: ' + JSON.stringify(processedData, null, 2));
            writeLog('=== SEAT MAP SERVICE END ===\n');
            
            return processedData;
        } catch (apiError) {
            writeLog('\n=== AMADEUS API ERROR ===');
            writeLog('Error type: ' + apiError.constructor.name);
            writeLog('Error message: ' + apiError.message);
            writeLog('Error stack: ' + apiError.stack);
            if (apiError.response) {
                writeLog('Amadeus API Error Response: ' + JSON.stringify({
                    status: apiError.response.status,
                    statusText: apiError.response.statusText,
                    data: apiError.response.data,
                    headers: apiError.response.headers
                }, null, 2));
            }
            writeLog('=== END AMADEUS API ERROR ===\n');
            throw apiError;
        }
    } catch (error) {
        writeLog('\n=== SEAT MAP SERVICE ERROR ===');
        writeLog('Error type: ' + error.constructor.name);
        writeLog('Error message: ' + error.message);
        writeLog('Error stack: ' + error.stack);
        if (error.response) {
            writeLog('Amadeus API Error Response: ' + JSON.stringify({
                status: error.response.status,
                statusText: error.response.statusText,
                data: error.response.data,
                headers: error.response.headers
            }, null, 2));
        }
        writeLog('=== END ERROR LOG ===\n');
        throw error;
    }
}

module.exports = {
    getSeatMap
}; 