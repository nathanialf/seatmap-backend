package com.seatmap.api.service;

import com.seatmap.api.model.SeatMapData.SeatCharacteristic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Maps seat characteristic codes from different APIs (Amadeus, Sabre) to normalized SeatCharacteristic objects
 */
public class SeatCharacteristicMapper {
    
    private static final Map<String, SeatCharacteristic> AMADEUS_MAPPINGS = new HashMap<>();
    private static final Map<String, SeatCharacteristic> SABRE_MAPPINGS = new HashMap<>();
    
    static {
        initializeAmadeusMapping();
        initializeSabreMapping();
    }
    
    private static void initializeAmadeusMapping() {
        // Position characteristics
        AMADEUS_MAPPINGS.put("W", new SeatCharacteristic("W", "POSITION", "Window seat", false, false));
        AMADEUS_MAPPINGS.put("A", new SeatCharacteristic("A", "POSITION", "Aisle seat", false, false));
        AMADEUS_MAPPINGS.put("9", new SeatCharacteristic("9", "POSITION", "Center seat (not window, not aisle)", false, false));
        AMADEUS_MAPPINGS.put("RS", new SeatCharacteristic("RS", "POSITION", "Right side of aircraft", false, false));
        AMADEUS_MAPPINGS.put("LS", new SeatCharacteristic("LS", "POSITION", "Left side of aircraft", false, false));
        
        // Special seats
        AMADEUS_MAPPINGS.put("K", new SeatCharacteristic("K", "SPECIAL", "Bulkhead seat", false, false));
        AMADEUS_MAPPINGS.put("E", new SeatCharacteristic("E", "SPECIAL", "Exit row seat", false, false));
        AMADEUS_MAPPINGS.put("L", new SeatCharacteristic("L", "SPECIAL", "Leg space seat", false, false));
        AMADEUS_MAPPINGS.put("FC", new SeatCharacteristic("FC", "SPECIAL", "Front of cabin class/compartment", false, false));
        
        // Premium/chargeable
        AMADEUS_MAPPINGS.put("CH", new SeatCharacteristic("CH", "PREMIUM", "Chargeable seats", false, true));
        AMADEUS_MAPPINGS.put("O", new SeatCharacteristic("O", "PREMIUM", "Preferential seat", false, true));
        AMADEUS_MAPPINGS.put("1A_AQC_PREMIUM_SEAT", new SeatCharacteristic("1A_AQC_PREMIUM_SEAT", "PREMIUM", "Premium seat", false, true));
        
        // Restrictions
        AMADEUS_MAPPINGS.put("1", new SeatCharacteristic("1", "RESTRICTION", "Restricted seat - General", true, false));
        AMADEUS_MAPPINGS.put("DE", new SeatCharacteristic("DE", "RESTRICTION", "Deportee", true, false));
        AMADEUS_MAPPINGS.put("C", new SeatCharacteristic("C", "RESTRICTION", "Crew seat", true, false));
        AMADEUS_MAPPINGS.put("1A", new SeatCharacteristic("1A", "RESTRICTION", "Seat not allowed for infant", true, false));
        AMADEUS_MAPPINGS.put("1B", new SeatCharacteristic("1B", "RESTRICTION", "Seat not allowed for medical", true, false));
        AMADEUS_MAPPINGS.put("1D", new SeatCharacteristic("1D", "RESTRICTION", "Restricted recline seat", true, false));
        AMADEUS_MAPPINGS.put("IE", new SeatCharacteristic("IE", "RESTRICTION", "Seat not suitable for child", true, false));
        AMADEUS_MAPPINGS.put("V", new SeatCharacteristic("V", "RESTRICTION", "Seat to be left vacant or offered last", true, false));
        
        // Special passenger types
        AMADEUS_MAPPINGS.put("U", new SeatCharacteristic("U", "SPECIAL", "Seat suitable for unaccompanied minors", false, false));
    }
    
    private static void initializeSabreMapping() {
        // Add Sabre mappings as needed - many will be similar to Amadeus
        // For now, we'll use the same mappings as a starting point
        SABRE_MAPPINGS.putAll(AMADEUS_MAPPINGS);
        
        // TODO: Add Sabre-specific characteristic codes when we implement Sabre seat map parsing
    }
    
    /**
     * Maps characteristic codes from Amadeus API to normalized SeatCharacteristic objects
     */
    public static List<SeatCharacteristic> mapAmadeusCharacteristics(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SeatCharacteristic> characteristics = new ArrayList<>();
        for (String code : codes) {
            SeatCharacteristic characteristic = AMADEUS_MAPPINGS.get(code);
            if (characteristic != null) {
                // Create a copy to avoid sharing instances
                characteristics.add(new SeatCharacteristic(
                    characteristic.getCode(),
                    characteristic.getCategory(),
                    characteristic.getDescription(),
                    characteristic.isRestriction(),
                    characteristic.isPremium()
                ));
            } else {
                // Unknown code - create a generic characteristic
                characteristics.add(new SeatCharacteristic(
                    code, 
                    "UNKNOWN", 
                    "Unmapped characteristic: " + code,
                    false,
                    false
                ));
            }
        }
        
        return characteristics;
    }
    
    /**
     * Maps characteristic codes from Sabre API to normalized SeatCharacteristic objects
     */
    public static List<SeatCharacteristic> mapSabreCharacteristics(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SeatCharacteristic> characteristics = new ArrayList<>();
        for (String code : codes) {
            SeatCharacteristic characteristic = SABRE_MAPPINGS.get(code);
            if (characteristic != null) {
                // Create a copy to avoid sharing instances
                characteristics.add(new SeatCharacteristic(
                    characteristic.getCode(),
                    characteristic.getCategory(),
                    characteristic.getDescription(),
                    characteristic.isRestriction(),
                    characteristic.isPremium()
                ));
            } else {
                // Unknown code - create a generic characteristic
                characteristics.add(new SeatCharacteristic(
                    code, 
                    "UNKNOWN", 
                    "Unmapped characteristic: " + code,
                    false,
                    false
                ));
            }
        }
        
        return characteristics;
    }
    
    /**
     * Gets all available Amadeus characteristic mappings (for documentation/debugging)
     */
    public static Map<String, SeatCharacteristic> getAmadeusMapping() {
        return new HashMap<>(AMADEUS_MAPPINGS);
    }
    
    /**
     * Gets all available Sabre characteristic mappings (for documentation/debugging)
     */
    public static Map<String, SeatCharacteristic> getSabreMapping() {
        return new HashMap<>(SABRE_MAPPINGS);
    }
}