async function handleSeatMapRequest(flightOffer) {
    try {
        console.log('=== SEAT MAP REQUEST START ===');
        console.log('Flight offer being sent:', JSON.stringify(flightOffer, null, 2));

        const response = await fetch('/api/seat-map', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ flightOffer })
        });

        console.log('Response status:', response.status);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));

        if (!response.ok) {
            const errorData = await response.json();
            console.error('Error response:', errorData);
            throw new Error(errorData.error || 'Failed to get seat map');
        }

        const data = await response.json();
        console.log('Seat map response data:', JSON.stringify(data, null, 2));

        if (!data) {
            throw new Error('No data received from seat map request');
        }

        console.log('=== SEAT MAP REQUEST END ===');
        return data;
    } catch (error) {
        console.error('=== SEAT MAP REQUEST ERROR ===');
        console.error('Error:', error);
        console.error('Error stack:', error.stack);
        console.error('=== END ERROR LOG ===');
        throw error;
    }
}

// En el manejador del botón de selección de asientos
document.querySelectorAll('.select-seats-btn').forEach(button => {
    button.addEventListener('click', async (event) => {
        try {
            console.log('=== SEAT SELECTION START ===');
            const flightOffer = JSON.parse(event.target.dataset.flightOffer);
            console.log('Flight offer from button:', JSON.stringify(flightOffer, null, 2));

            const seatMapData = await handleSeatMapRequest(flightOffer);
            console.log('Seat map data received:', JSON.stringify(seatMapData, null, 2));

            const seatMap = new SeatMap('seat-map-container');
            seatMap.displaySeatMap(seatMapData);
            
            console.log('=== SEAT SELECTION END ===');
        } catch (error) {
            console.error('=== SEAT SELECTION ERROR ===');
            console.error('Error:', error);
            console.error('Error stack:', error.stack);
            console.error('=== END ERROR LOG ===');
            alert('Error getting seat map: ' + error.message);
        }
    });
}); 