package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.model.SeatMapData.SeatCharacteristic;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatCharacteristicMapperTest {

    @Test
    void mapAmadeusCharacteristics_WithKnownCodes_ReturnsCorrectMappings() {
        List<String> codes = Arrays.asList("W", "CH", "1A_AQC_PREMIUM_SEAT");
        
        List<SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapAmadeusCharacteristics(codes, null);
        
        assertEquals(3, characteristics.size());
        
        // Check window seat
        SeatCharacteristic windowSeat = characteristics.get(0);
        assertEquals("W", windowSeat.getCode());
        assertEquals("POSITION", windowSeat.getCategory());
        assertEquals("Window seat", windowSeat.getDescription());
        assertFalse(windowSeat.isRestriction());
        assertFalse(windowSeat.isPremium());
        
        // Check chargeable seat
        SeatCharacteristic chargeableSeat = characteristics.get(1);
        assertEquals("CH", chargeableSeat.getCode());
        assertEquals("PREMIUM", chargeableSeat.getCategory());
        assertEquals("Chargeable seats", chargeableSeat.getDescription());
        assertFalse(chargeableSeat.isRestriction());
        assertTrue(chargeableSeat.isPremium());
        
        // Check premium seat
        SeatCharacteristic premiumSeat = characteristics.get(2);
        assertEquals("1A_AQC_PREMIUM_SEAT", premiumSeat.getCode());
        assertEquals("PREMIUM", premiumSeat.getCategory());
        assertEquals("Premium seat", premiumSeat.getDescription());
        assertFalse(premiumSeat.isRestriction());
        assertTrue(premiumSeat.isPremium());
    }

    @Test
    void mapAmadeusCharacteristics_WithRestrictionCodes_MarksRestrictions() {
        List<String> codes = Arrays.asList("1", "DE", "1A");
        
        List<SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapAmadeusCharacteristics(codes, null);
        
        assertEquals(3, characteristics.size());
        
        // All should be marked as restrictions
        for (SeatCharacteristic characteristic : characteristics) {
            assertTrue(characteristic.isRestriction(), 
                "Characteristic " + characteristic.getCode() + " should be marked as restriction");
            assertEquals("RESTRICTION", characteristic.getCategory());
        }
    }

    @Test
    void mapAmadeusCharacteristics_WithUnknownCode_CreatesGenericMapping() {
        List<String> codes = Arrays.asList("UNKNOWN_CODE");
        
        List<SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapAmadeusCharacteristics(codes, null);
        
        assertEquals(1, characteristics.size());
        
        SeatCharacteristic unknown = characteristics.get(0);
        assertEquals("UNKNOWN_CODE", unknown.getCode());
        assertEquals("UNKNOWN", unknown.getCategory());
        assertTrue(unknown.getDescription().contains("UNKNOWN_CODE"));
        assertFalse(unknown.isRestriction());
        assertFalse(unknown.isPremium());
    }

    @Test
    void mapAmadeusCharacteristics_WithNullOrEmpty_ReturnsEmptyList() {
        assertTrue(SeatCharacteristicMapper.mapAmadeusCharacteristics(null, null).isEmpty());
        assertTrue(SeatCharacteristicMapper.mapAmadeusCharacteristics(Arrays.asList(), null).isEmpty());
    }

    @Test
    void mapSabreCharacteristics_WorksSimilarToAmadeus() {
        List<String> codes = Arrays.asList("W", "A");
        
        List<SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapSabreCharacteristics(codes);
        
        assertEquals(2, characteristics.size());
        assertEquals("W", characteristics.get(0).getCode());
        assertEquals("A", characteristics.get(1).getCode());
    }

    @Test
    void getAmadeusMapping_ReturnsNonEmptyMap() {
        var mappings = SeatCharacteristicMapper.getAmadeusMapping();
        
        assertFalse(mappings.isEmpty());
        assertTrue(mappings.containsKey("W"));
        assertTrue(mappings.containsKey("CH"));
        assertTrue(mappings.containsKey("1"));
    }

    @Test
    void mapAmadeusCharacteristics_WithResponseDictionary_UsesResponseDefinitions() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Create a mock response with dictionaries including 'H' characteristic
        String dictionariesJson = """
        {
            "seatCharacteristics": {
                "H": "High-traffic area seat",
                "W": "Window seat override from response",
                "CUSTOM": "Custom airline-specific seat feature"
            }
        }
        """;
        
        JsonNode dictionaries = objectMapper.readTree(dictionariesJson);
        List<String> codes = Arrays.asList("H", "W", "CUSTOM", "UNKNOWN_CODE");
        
        List<SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapAmadeusCharacteristics(codes, dictionaries);
        
        assertEquals(4, characteristics.size());
        
        // Check 'H' characteristic from response dictionary
        SeatCharacteristic hCharacteristic = characteristics.get(0);
        assertEquals("H", hCharacteristic.getCode());
        assertEquals("GENERAL", hCharacteristic.getCategory());
        assertEquals("High-traffic area seat", hCharacteristic.getDescription());
        assertFalse(hCharacteristic.isRestriction());
        assertFalse(hCharacteristic.isPremium());
        
        // Check that response dictionary overrides hardcoded mapping for 'W'
        SeatCharacteristic wCharacteristic = characteristics.get(1);
        assertEquals("W", wCharacteristic.getCode());
        assertEquals("POSITION", wCharacteristic.getCategory());
        assertEquals("Window seat override from response", wCharacteristic.getDescription());
        
        // Check custom characteristic from response
        SeatCharacteristic customCharacteristic = characteristics.get(2);
        assertEquals("CUSTOM", customCharacteristic.getCode());
        assertEquals("GENERAL", customCharacteristic.getCategory());
        assertEquals("Custom airline-specific seat feature", customCharacteristic.getDescription());
        
        // Check that unmapped codes still create generic mappings
        SeatCharacteristic unknownCharacteristic = characteristics.get(3);
        assertEquals("UNKNOWN_CODE", unknownCharacteristic.getCode());
        assertEquals("UNKNOWN", unknownCharacteristic.getCategory());
        assertTrue(unknownCharacteristic.getDescription().contains("Unmapped characteristic"));
    }
}