jest.mock('../../src/services/amadeus-auth', () => ({
  getAccessToken: jest.fn().mockResolvedValue('mocked_token')
}));

const axios = require('axios');
const { searchAirports } = require('../../src/services/airport-search');

// Mock axios
jest.mock('axios');

describe('Airport Search Service', () => {
    beforeEach(() => {
        // Clear all mocks before each test
        jest.clearAllMocks();
    });

    it('should return empty array when keyword is empty', async () => {
        const result = await searchAirports('');
        expect(result).toEqual([]);
    });

    it('should return empty array when keyword is less than 2 characters', async () => {
        const result = await searchAirports('a');
        expect(result).toEqual([]);
    });

    it('should return formatted airport results', async () => {
        // Mock successful API response
        axios.get.mockResolvedValueOnce({
            data: {
                data: [{
                    iataCode: 'JFK',
                    name: 'John F. Kennedy International Airport',
                    address: {
                        cityName: 'New York',
                        countryName: 'United States'
                    }
                }]
            }
        });

        const result = await searchAirports('new york');
        expect(result).toEqual([
            {
                code: 'JFK',
                name: 'John F. Kennedy International Airport',
                city: 'New York',
                country: 'United States'
            }
        ]);
    });

    it('should handle API errors gracefully', async () => {
        // Mock API error
        axios.get.mockRejectedValueOnce(new Error('API Error'));

        const result = await searchAirports('new york');
        expect(result).toEqual([]);
    });

    it('should handle empty API response', async () => {
        // Mock empty API response
        axios.get.mockResolvedValueOnce({
            data: {
                data: []
            }
        });

        const result = await searchAirports('nonexistent');
        expect(result).toEqual([]);
    });
}); 