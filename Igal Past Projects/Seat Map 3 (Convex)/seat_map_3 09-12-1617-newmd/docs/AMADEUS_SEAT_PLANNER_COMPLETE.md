# 🛩️ **DOCUMENTACIÓN COMPLETA: API AMADEUS SEAT PLANNER**

## 📋 **TABLA DE CONTENIDOS**
1. [Objetivo](#objetivo)
2. [Integración con API](#integración-con-api-amadeus-seat-planner)
3. [Estructura de Respuesta](#estructura-completa-de-la-respuesta-de-amadeus)
4. [Códigos de Características](#códigos-de-características-de-asientos)
5. [Salidas de Emergencia](#salidas-de-emergencia-exit-rows)
6. [Identificación de Pasillos](#identificación-y-construcción-de-pasillos-aisles)
7. [Identificación de Alas](#identificación-de-alas-wings)
8. [Estados de Asientos](#estados-de-asientos-seat-availability-status)
9. [Construcción del Deck](#construcción-completa-del-deck)
10. [Implementación del Endpoint](#implementación-completa-del-endpoint)
11. [Flujo de Implementación](#flujo-completo-de-implementación)
12. [Características Visuales](#características-visuales-implementadas)
13. [Checklist](#checklist-de-implementación-completa)
14. [Ejemplos Prácticos](#ejemplos-prácticos)
15. [Troubleshooting](#troubleshooting)
16. [Mejores Prácticas](#mejores-prácticas)

---

## 🎯 **OBJETIVO**
Implementar un sistema completo de planificación de asientos usando la API de Amadeus Seat Planner, incluyendo la construcción del deck, identificación de pasillos, salidas de emergencia, alas, y gestión de estados de asientos.

---

## 🔌 **INTEGRACIÓN CON API AMADEUS SEAT PLANNER**

### **1. Configuración Inicial**
```javascript
// Configuración del cliente Amadeus
const Amadeus = require('amadeus');

const amadeus = new Amadeus({
  clientId: process.env.AMADEUS_CLIENT_ID,
  clientSecret: process.env.AMADEUS_CLIENT_SECRET,
  hostname: 'production' // o 'test' para desarrollo
});
```

### **2. Endpoint de la API**
```javascript
// Endpoint: POST /v2/shopping/seatmaps
// Método: POST
// Content-Type: application/json
```

### **3. Llamada a la API**
```javascript
const response = await amadeus.shopping.seatmaps.post(JSON.stringify({
    data: [formattedFlightOffer]  // Flight offer completo
}));
```

### **4. Parámetros Requeridos en `formattedFlightOffer`:**
```javascript
{
    type: "flight-offer",                    // Tipo de oferta
    id: "string",                           // ID único del vuelo
    source: "GDS",                          // Fuente de la oferta
    instantTicketingRequired: boolean,      // Ticketing inmediato requerido
    nonHomogeneous: boolean,                // Vuelo no homogéneo
    oneWay: boolean,                        // Solo ida
    lastTicketingDate: "string",            // Última fecha de ticketing
    lastTicketingDateTime: "string",        // Última fecha/hora de ticketing
    numberOfBookableSeats: number,          // Número de asientos reservables
    itineraries: [...],                     // Array de itinerarios
    price: {...},                           // Información de precios
    travelerPricings: [...]                 // Precios por pasajero
}
```

---

## 🏗️ **ESTRUCTURA COMPLETA DE LA RESPUESTA DE AMADEUS**

### **Respuesta Principal:**
```javascript
{
  data: [
    {
      type: "seatmap",
      id: "string",
      flight: {
        number: "string",                    // Número de vuelo
        departure: {
          iataCode: "string",               // Aeropuerto origen
          terminal: "string",               // Terminal
          at: "string"                      // Fecha/hora salida ISO
        },
        arrival: {
          iataCode: "string",               // Aeropuerto destino
          terminal: "string",               // Terminal
          at: "string"                      // Fecha/hora llegada ISO
        },
        carrierCode: "string",              // Código de aerolínea
        operating: "string"                 // Aerolínea operadora
      },
      aircraft: {
        code: "string"                      // ← CÓDIGO DEL AVIÓN (ej: "320" = Airbus A320)
      },
      class: "string",                      // Clase de cabina
      flightOfferId: "string",              // ID de la oferta de vuelo
      segmentId: "string",                  // ID del segmento
      decks: [...]                          // ← ARRAY DE DECKS (CUBIERTAS)
    }
  ]
}
```

### **Estructura Detallada de Decks:**
```javascript
decks: [
  {
    deckType: "MAIN",                       // Tipo de cubierta (MAIN, UPPER, LOWER)
    deckConfiguration: {                     // ← CONFIGURACIÓN DEL DECK
      length: number,                        // Número de filas
      width: number,                         // Número de columnas
      exitRows: ["number"],                  // Filas de salida de emergencia
      exitRowsX: ["number"]                  // Coordenadas X de filas de salida
    },
    facilities: [                            // ← INSTALACIONES (BAÑOS, COCINAS, ETC.)
      {
        code: "string",                      // Código de instalación
        column: "string",                    // Columna (A, B, C, D, E, F)
        row: "number",                       // Fila (1, 2, 3, 4, 5...)
        position: "string",                  // Posición (ej: "1A", "2B")
        coordinates: {
          x: number,                         // Coordenada X (fila)
          y: number                          // Coordenada Y (columna)
        }
      }
    ],
    seats: [                                 // ← ASIENTOS INDIVIDUALES
      {
        cabin: "string",                     // Clase de cabina
        number: "string",                    // Número de asiento (ej: "1A", "2B")
        characteristicsCodes: ["string"],    // ← CÓDIGOS DE CARACTERÍSTICAS
        travelerPricing: [                   // ← PRECIOS Y DISPONIBILIDAD
          {
            seatAvailabilityStatus: "string", // ← ESTADO DEL ASIENTO
            price: { total: "string", currency: "string" }
          }
        ],
        coordinates: {
          x: number,                         // Coordenada X (fila)
          y: number                          // Coordenada Y (columna)
        }
      }
    ]
  }
]
```

---

## 🔍 **CÓDIGOS DE CARACTERÍSTICAS DE ASIENTOS (characteristicsCodes)**

### **Códigos Principales:**
```javascript
characteristicsCodes: [
  // POSICIÓN
  "W",    // ← WINDOW (Ventana)
  "A",    // ← AISLE (Pasillo)
  "9",    // ← CENTER (Centro)
  "M",    // ← MIDDLE (Medio)
  
  // CARACTERÍSTICAS ESPECIALES
  "E",    // ← EXIT (Salida de emergencia)
  "B",    // ← BULKHEAD (Mampara)
  "X",    // ← EXTRA LEGROOM (Espacio extra para piernas)
  "P",    // ← PREMIUM (Premium)
  "Q",    // ← QUIET (Silencioso)
  
  // UBICACIÓN EN EL AVIÓN
  "F",    // ← FRONT (Frente)
  "R",    // ← REAR (Trasero)
  "U",    // ← UPPER DECK (Cubierta superior)
  "L",    // ← LOWER DECK (Cubierta inferior)
  
  // INSTALACIONES CERCANAS
  "G",    // ← GALLEY (Cocina)
  "T",    // ← TOILET (Baño)
  "D",    // ← DOOR (Puerta)
  
  // OTROS
  "S",    // ← SIDE (Lateral)
  "C",    // ← CORNER (Esquina)
  "V",    // ← VIEW (Vista)
  "H",    // ← HIGH (Alto)
  "I",    // ← INSIDE (Interior)
  "O",    // ← OUTSIDE (Exterior)
  "Z"     // ← ZONE (Zona)
]
```

### **Ejemplos de Combinaciones Comunes:**
```javascript
// Asiento de ventana
["W"] → Asiento junto a la ventana

// Asiento de pasillo
["A"] → Asiento junto al pasillo

// Asiento del centro
["9"] → Asiento del medio

// Asiento de ventana con salida de emergencia
["W", "E"] → Asiento de ventana en fila de salida

// Asiento de pasillo con espacio extra
["A", "X"] → Asiento de pasillo con espacio extra para piernas

// Asiento premium de ventana
["W", "P"] → Asiento premium junto a la ventana

// Asiento cerca de baño
["A", "T"] → Asiento de pasillo cerca del baño

// Asiento en mampara con espacio extra
["B", "X"] → Asiento en mampara con espacio extra
```

---

## 🚪 **SALIDAS DE EMERGENCIA (EXIT ROWS)**

### **1. Identificación de Filas de Salida:**
```javascript
// En deckConfiguration
deckConfiguration: {
  exitRows: ["15", "16", "30", "31"],      // ← FILAS DE SALIDA (números de fila)
  exitRowsX: [15, 16, 30, 31]              // ← COORDENADAS X DE FILAS DE SALIDA
}

// En characteristicsCodes de asientos
characteristicsCodes: ["E"]                 // ← CÓDIGO "E" = EXIT
```

### **2. Implementación de Filas de Salida:**
```javascript
// Función para detectar filas de salida
detectExitRows(deckConfiguration, seats) {
    const exitRows = new Set();
    
    // Método 1: Desde deckConfiguration
    if (deckConfiguration.exitRows) {
        deckConfiguration.exitRows.forEach(row => {
            exitRows.add(parseInt(row));
        });
    }
    
    if (deckConfiguration.exitRowsX) {
        deckConfiguration.exitRowsX.forEach(row => {
            exitRows.add(row);
        });
    }
    
    // Método 2: Desde características de asientos
    seats.forEach(seat => {
        const codes = seat.characteristicsCodes || [];
        if (codes.includes('E')) {
            const row = this.extractRowFromSeatNumber(seat.number);
            exitRows.add(row);
        }
    });
    
    return Array.from(exitRows).sort((a, b) => a - b);
}

// Renderizado de filas de salida
createSeatGrid() {
    // ... código existente ...
    
    for (let x = minX; x <= maxX; x++) {
        const rowElement = document.createElement('div');
        rowElement.className = 'seat-row coordinates-layout';
        rowElement.dataset.row = x;
        
        // Verificar si es fila de salida de emergencia
        const isExitRow = this.exitRows.includes(x);
        
        if (isExitRow) {
            rowElement.classList.add('exit-row');
        }
        
        // Agregar etiqueta de fila con indicador de salida
        const rowLabel = document.createElement('div');
        rowLabel.className = 'row-label';
        
        if (isExitRow) {
            rowLabel.classList.add('exit-row-label');
            rowLabel.innerHTML = `
                <div class="row-number">${realRowNumber}</div>
                <div class="exit-indicator">
                    <span class="exit-icon">🚪</span>
                    <span class="exit-text">EXIT</span>
                </div>
            `;
        } else {
            rowLabel.textContent = realRowNumber;
        }
        
        rowElement.appendChild(rowLabel);
        // ... resto del código ...
    }
}
```

### **3. CSS para Filas de Salida:**
```css
.exit-row {
    background-color: #fff3cd;
    border: 2px dashed #ffc107;
    position: relative;
}

.exit-row-label {
    background-color: #ffc107;
    color: #856404;
    font-weight: bold;
    text-align: center;
    padding: 4px;
    border-radius: 4px;
}

.exit-indicator {
    font-size: 0.8em;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
}

.exit-icon {
    font-size: 1.2em;
}

.seat.exit-row-seat {
    border: 2px dashed #ffc107;
    background-color: #fff3cd;
    position: relative;
}

.seat.exit-row-seat::after {
    content: "EXIT";
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 0.6em;
    color: #856404;
    font-weight: bold;
}
```

---

## 🛣️ **IDENTIFICACIÓN Y CONSTRUCCIÓN DE PASILLOS (AISLES)**

### **1. Detección Automática de Pasillos:**
```javascript
// Función principal para detectar pasillos
detectAisles(seats, deckConfiguration) {
    const aisleInfo = {
        positions: new Set(),
        byRow: new Map(),
        configuration: 'unknown'
    };
    
    // Método 1: Análisis por códigos de características
    const aisleSeats = seats.filter(seat => {
        const codes = seat.characteristicsCodes || [];
        return codes.includes('A');
    });
    
    // Agrupar asientos de pasillo por fila
    const aislesByRow = new Map();
    aisleSeats.forEach(seat => {
        const row = this.extractRowFromSeatNumber(seat.number);
        const column = this.extractColumnFromSeatNumber(seat.number);
        
        if (!aislesByRow.has(row)) {
            aislesByRow.set(row, []);
        }
        aislesByRow.get(row).push(column);
    });
    
    // Analizar configuración de pasillos
    aislesByRow.forEach((columns, row) => {
        columns.sort((a, b) => this.columnLetterToNumber(a) - this.columnLetterToNumber(b));
        
        // Determinar posiciones de pasillos
        for (let i = 0; i < columns.length; i++) {
            const currentColumn = columns[i];
            const nextColumn = columns[i + 1];
            
            if (nextColumn) {
                const aislePosition = `${currentColumn}-${nextColumn}`;
                aisleInfo.positions.add(aislePosition);
            }
        }
        
        aisleInfo.byRow.set(row, columns);
    });
    
    // Determinar configuración general (3-3, 2-4-2, etc.)
    aisleInfo.configuration = this.determineAisleConfiguration(aislesByRow);
    
    return aisleInfo;
}

// Función para determinar configuración de pasillos
determineAisleConfiguration(aislesByRow) {
    const configurations = new Map();
    
    aislesByRow.forEach((aisleColumns, row) => {
        const config = this.analyzeRowConfiguration(row, aisleColumns);
        const configKey = config.join('-');
        
        if (!configurations.has(configKey)) {
            configurations.set(configKey, 0);
        }
        configurations.set(configKey, configurations.get(configKey) + 1);
    });
    
    // Retornar la configuración más común
    let mostCommon = '';
    let maxCount = 0;
    
    configurations.forEach((count, config) => {
        if (count > maxCount) {
            maxCount = count;
            mostCommon = config;
        }
    });
    
    return mostCommon || 'unknown';
}

// Análisis de configuración por fila
analyzeRowConfiguration(row, aisleColumns) {
    // Obtener todos los asientos de la fila
    const rowSeats = this.getSeatsForRow(row);
    const allColumns = rowSeats.map(seat => 
        this.extractColumnFromSeatNumber(seat.number)
    ).sort((a, b) => this.columnLetterToNumber(a) - this.columnLetterToNumber(b));
    
    const sections = [];
    let currentSection = [];
    
    allColumns.forEach((column, index) => {
        currentSection.push(column);
        
        // Si este es un asiento de pasillo y hay un siguiente asiento
        if (aisleColumns.includes(column) && index < allColumns.length - 1) {
            sections.push(currentSection.length);
            currentSection = [];
        }
    });
    
    // Agregar la última sección
    if (currentSection.length > 0) {
        sections.push(currentSection.length);
    }
    
    return sections;
}
```

### **2. Renderizado de Pasillos:**
```javascript
// Función para renderizar pasillos en la grilla
renderAisles(rowElement, row, minY, maxY) {
    const aislePositions = this.aisleInfo.positions;
    const occupiedColumns = [];
    
    // Identificar columnas ocupadas en esta fila
    for (let y = minY; y <= maxY; y++) {
        const coordKey = `${row},${y}`;
        if (this.coordinatesMap.has(coordKey)) {
            occupiedColumns.push(y);
        }
    }
    
    // Crear elementos para cada posición
    for (let y = minY; y <= maxY; y++) {
        const coordKey = `${row},${y}`;
        
        if (this.coordinatesMap.has(coordKey)) {
            // Renderizar asiento o instalación
            const item = this.coordinatesMap.get(coordKey);
            const element = this.createCoordinateBasedElement(item, this.isExitRow(row));
            rowElement.appendChild(element);
        } else {
            // Verificar si esta posición es un pasillo
            const isAisle = this.isAislePosition(row, y, occupiedColumns);
            
            if (isAisle) {
                const aisleElement = document.createElement('div');
                aisleElement.className = 'aisle-space';
                aisleElement.innerHTML = `
                    <div class="aisle-indicator">
                        <div class="aisle-line"></div>
                    </div>
                `;
                rowElement.appendChild(aisleElement);
            } else {
                // Espacio vacío
                const emptyElement = document.createElement('div');
                emptyElement.className = 'empty-space';
                rowElement.appendChild(emptyElement);
            }
        }
    }
}

// Función para determinar si una posición es un pasillo
isAislePosition(row, y, occupiedColumns) {
    // Buscar espacios entre columnas ocupadas
    for (let i = 0; i < occupiedColumns.length - 1; i++) {
        const currentCol = occupiedColumns[i];
        const nextCol = occupiedColumns[i + 1];
        
        // Si hay un espacio entre columnas y y está en ese espacio
        if (nextCol - currentCol > 1 && y > currentCol && y < nextCol) {
            return true;
        }
    }
    
    return false;
}
```

### **3. CSS para Pasillos:**
```css
.aisle-space {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(90deg, #e9ecef 0%, #f8f9fa 50%, #e9ecef 100%);
    position: relative;
}

.aisle-indicator {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.aisle-line {
    width: 2px;
    height: 80%;
    background: linear-gradient(to bottom, transparent 0%, #6c757d 20%, #6c757d 80%, transparent 100%);
    border-radius: 1px;
}

.aisle-space::before {
    content: "";
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 20px;
    height: 20px;
    background: radial-gradient(circle, rgba(108, 117, 125, 0.1) 0%, transparent 70%);
    border-radius: 50%;
}

/* Configuraciones específicas de pasillos */
.cabin-layout-3-3 .aisle-space {
    width: 60px; /* Pasillo más ancho para configuración 3-3 */
}

.cabin-layout-2-4-2 .aisle-space {
    width: 50px; /* Pasillo medio para configuración 2-4-2 */
}

.cabin-layout-2-2 .aisle-space {
    width: 40px; /* Pasillo estándar para configuración 2-2 */
}
```

---

## 🦅 **IDENTIFICACIÓN DE ALAS (WINGS)**

### **1. Detección de Alas por Múltiples Métodos:**
```javascript
// Función principal para detectar posición de alas
detectWingPosition(deckConfiguration, seats, aircraft) {
    const wingInfo = {
        startRow: null,
        endRow: null,
        wingSeats: new Set(),
        characteristics: [],
        position: 'unknown',
        affectedSeats: new Map()
    };
    
    // Método 1: Por código de aeronave
    const aircraftWingData = this.getWingDataByAircraft(aircraft.code);
    if (aircraftWingData) {
        wingInfo.startRow = aircraftWingData.startRow;
        wingInfo.endRow = aircraftWingData.endRow;
        wingInfo.position = aircraftWingData.position;
    }
    
    // Método 2: Por características de asientos
    seats.forEach(seat => {
        const codes = seat.characteristicsCodes || [];
        const row = this.extractRowFromSeatNumber(seat.number);
        
        // Buscar códigos que indiquen posición cerca del ala
        const wingCodes = codes.filter(code => 
            ['S', 'V', 'O'].includes(code) // Side, View, Outside
        );
        
        if (wingCodes.length > 0) {
            if (!wingInfo.startRow || row < wingInfo.startRow) {
                wingInfo.startRow = row;
            }
            if (!wingInfo.endRow || row > wingInfo.endRow) {
                wingInfo.endRow = row;
            }
            
            wingInfo.wingSeats.add(seat.number);
            wingInfo.characteristics.push(...wingCodes);
            wingInfo.affectedSeats.set(seat.number, {
                codes: wingCodes,
                impact: this.assessWingImpact(codes)
            });
        }
    });
    
    // Método 3: Por análisis de configuración del deck
    if (!wingInfo.startRow && deckConfiguration.length) {
        const estimatedWingPosition = this.estimateWingPosition(deckConfiguration.length);
        wingInfo.startRow = estimatedWingPosition.start;
        wingInfo.endRow = estimatedWingPosition.end;
        wingInfo.position = 'estimated';
    }
    
    return wingInfo;
}

// Base de datos de posiciones de alas por aeronave
getWingDataByAircraft(aircraftCode) {
    const wingDatabase = {
        '320': { // Airbus A320
            startRow: 12,
            endRow: 18,
            position: 'mid-cabin',
            description: 'Wing over rows 12-18'
        },
        '321': { // Airbus A321
            startRow: 14,
            endRow: 20,
            position: 'mid-cabin',
            description: 'Wing over rows 14-20'
        },
        '737': { // Boeing 737
            startRow: 10,
            endRow: 16,
            position: 'mid-cabin',
            description: 'Wing over rows 10-16'
        },
        '738': { // Boeing 737-800
            startRow: 11,
            endRow: 17,
            position: 'mid-cabin',
            description: 'Wing over rows 11-17'
        },
        '777': { // Boeing 777
            startRow: 18,
            endRow: 26,
            position: 'mid-cabin',
            description: 'Wing over rows 18-26'
        },
        '380': { // Airbus A380
            startRow: 20,
            endRow: 30,
            position: 'mid-cabin',
            description: 'Wing over rows 20-30'
        }
    };
    
    return wingDatabase[aircraftCode] || null;
}

// Función para estimar posición del ala basada en longitud del avión
estimateWingPosition(totalRows) {
    // Las alas típicamente están en el tercio medio del avión
    const wingStart = Math.floor(totalRows * 0.35);
    const wingEnd = Math.floor(totalRows * 0.65);
    
    return {
        start: wingStart,
        end: wingEnd,
        confidence: 'low'
    };
}

// Evaluar impacto del ala en la vista del asiento
assessWingImpact(characteristicsCodes) {
    const impacts = [];
    
    if (characteristicsCodes.includes('V')) {
        impacts.push('limited-view');
    }
    if (characteristicsCodes.includes('S')) {
        impacts.push('side-position');
    }
    if (characteristicsCodes.includes('O')) {
        impacts.push('outside-view');
    }
    
    return impacts;
}
```

### **2. Renderizado de Alas:**
```javascript
// Función para renderizar indicadores de ala
renderWingIndicators(seatMapContainer) {
    if (!this.wingInfo.startRow || !this.wingInfo.endRow) return;
    
    const wingOverlay = document.createElement('div');
    wingOverlay.className = 'wing-overlay';
    
    // Crear indicador de ala izquierda
    const leftWing = document.createElement('div');
    leftWing.className = 'wing-indicator left-wing';
    leftWing.innerHTML = `
        <div class="wing-shape">
            <div class="wing-body"></div>
            <div class="wing-tip"></div>
        </div>
        <div class="wing-label">Wing</div>
    `;
    
    // Crear indicador de ala derecha
    const rightWing = document.createElement('div');
    rightWing.className = 'wing-indicator right-wing';
    rightWing.innerHTML = `
        <div class="wing-shape">
            <div class="wing-body"></div>
            <div class="wing-tip"></div>
        </div>
        <div class="wing-label">Wing</div>
    `;
    
    // Posicionar las alas
    const startRowElement = seatMapContainer.querySelector(`[data-row="${this.wingInfo.startRow}"]`);
    const endRowElement = seatMapContainer.querySelector(`[data-row="${this.wingInfo.endRow}"]`);
    
    if (startRowElement && endRowElement) {
        const startTop = startRowElement.offsetTop;
        const endTop = endRowElement.offsetTop + endRowElement.offsetHeight;
        const wingHeight = endTop - startTop;
        
        leftWing.style.top = `${startTop}px`;
        leftWing.style.height = `${wingHeight}px`;
        leftWing.style.left = '-60px';
        
        rightWing.style.top = `${startTop}px`;
        rightWing.style.height = `${wingHeight}px`;
        rightWing.style.right = '-60px';
        
        wingOverlay.appendChild(leftWing);
        wingOverlay.appendChild(rightWing);
        seatMapContainer.appendChild(wingOverlay);
    }
}

// Marcar asientos afectados por el ala
markWingAffectedSeats() {
    this.wingInfo.affectedSeats.forEach((impact, seatNumber) => {
        const seatElement = document.querySelector(`[data-seat-id="${seatNumber}"]`);
        if (seatElement) {
            seatElement.classList.add('wing-affected');
            
            // Agregar tooltip con información del impacto
            const tooltip = document.createElement('div');
            tooltip.className = 'wing-impact-tooltip';
            tooltip.innerHTML = `
                <div class="tooltip-content">
                    <strong>Wing Position</strong><br>
                    ${impact.impact.join(', ')}
                </div>
            `;
            
            seatElement.appendChild(tooltip);
        }
    });
}
```

### **3. CSS para Alas:**
```css
.wing-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
}

.wing-indicator {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.7;
}

.wing-shape {
    display: flex;
    align-items: center;
}

.left-wing .wing-body {
    width: 40px;
    height: 8px;
    background: linear-gradient(90deg, #6c757d, #adb5bd);
    border-radius: 0 4px 4px 0;
    position: relative;
}

.left-wing .wing-tip {
    width: 0;
    height: 0;
    border-top: 12px solid transparent;
    border-bottom: 12px solid transparent;
    border-right: 20px solid #6c757d;
    margin-left: -1px;
}

.right-wing .wing-body {
    width: 40px;
    height: 8px;
    background: linear-gradient(270deg, #6c757d, #adb5bd);
    border-radius: 4px 0 0 4px;
    position: relative;
}

.right-wing .wing-tip {
    width: 0;
    height: 0;
    border-top: 12px solid transparent;
    border-bottom: 12px solid transparent;
    border-left: 20px solid #6c757d;
    margin-right: -1px;
}

.wing-label {
    position: absolute;
    font-size: 0.7em;
    color: #6c757d;
    font-weight: bold;
    white-space: nowrap;
}

.left-wing .wing-label {
    right: -30px;
    transform: rotate(-90deg);
}

.right-wing .wing-label {
    left: -30px;
    transform: rotate(90deg);
}

.seat.wing-affected {
    position: relative;
    border: 1px dashed #ffc107;
}

.seat.wing-affected::after {
    content: "✈️";
    position: absolute;
    top: -5px;
    right: -5px;
    font-size: 0.7em;
    opacity: 0.6;
}

.wing-impact-tooltip {
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.8);
    color: white;
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 0.7em;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s;
    z-index: 10;
}

.seat.wing-affected:hover .wing-impact-tooltip {
    opacity: 1;
}
```

---

## 🎯 **ESTADOS DE ASIENTOS (SEAT AVAILABILITY STATUS)**

### **1. Estados Disponibles en Amadeus:**
```javascript
// Estados principales de disponibilidad
const SEAT_STATUSES = {
    AVAILABLE: 'available',       // ← DISPONIBLE para selección
    OCCUPIED: 'occupied',         // ← OCUPADO por otro pasajero
    BLOCKED: 'blocked',           // ← BLOQUEADO por la aerolínea
    RESERVED: 'reserved',         // ← RESERVADO temporalmente
    UNAVAILABLE: 'unavailable',   // ← NO DISPONIBLE por restricciones
    RESTRICTED: 'restricted',     // ← RESTRINGIDO para ciertos pasajeros
    MAINTENANCE: 'maintenance',   // ← EN MANTENIMIENTO
    INOPERATIVE: 'inoperative'    // ← INOPERATIVO
};

// Estados adicionales que pueden aparecer
const EXTENDED_STATUSES = {
    PENDING: 'pending',           // ← PENDIENTE de confirmación
    CONFIRMED: 'confirmed',       // ← CONFIRMADO
    CANCELLED: 'cancelled',       // ← CANCELADO
    WAITLIST: 'waitlist',         // ← EN LISTA DE ESPERA
    UPGRADE: 'upgrade',           // ← DISPONIBLE PARA UPGRADE
    PREMIUM: 'premium'            // ← ASIENTO PREMIUM con costo extra
};
```

### **2. Procesamiento de Estados:**
```javascript
// Función para procesar estado de asiento desde Amadeus
processSeatAvailability(seat) {
    const seatInfo = {
        number: seat.number,
        status: 'unknown',
        price: null,
        currency: null,
        restrictions: [],
        characteristics: seat.characteristicsCodes || [],
        coordinates: seat.coordinates
    };
    
    // Procesar travelerPricing para obtener disponibilidad y precio
    if (seat.travelerPricing && seat.travelerPricing.length > 0) {
        const pricing = seat.travelerPricing[0];
        
        // Estado de disponibilidad
        if (pricing.seatAvailabilityStatus) {
            seatInfo.status = pricing.seatAvailabilityStatus.toLowerCase();
        }
        
        // Precio del asiento
        if (pricing.price) {
            seatInfo.price = parseFloat(pricing.price.total);
            seatInfo.currency = pricing.price.currency;
        }
        
        // Restricciones adicionales
        if (pricing.restrictions) {
            seatInfo.restrictions = pricing.restrictions;
        }
    }
    
    // Validar y normalizar estado
    seatInfo.status = this.normalizeSeatStatus(seatInfo.status);
    
    // Determinar si el asiento es seleccionable
    seatInfo.selectable = this.isSeatSelectable(seatInfo);
    
    return seatInfo;
}

// Función para normalizar estados de asientos
normalizeSeatStatus(status) {
    const statusMap = {
        'available': 'available',
        'free': 'available',
        'open': 'available',
        'occupied': 'occupied',
        'taken': 'occupied',
        'booked': 'occupied',
        'blocked': 'blocked',
        'closed': 'blocked',
        'reserved': 'reserved',
        'hold': 'reserved',
        'unavailable': 'unavailable',
        'restricted': 'restricted',
        'maintenance': 'maintenance',
        'inoperative': 'inoperative',
        'out-of-order': 'inoperative'
    };
    
    return statusMap[status] || 'unknown';
}

// Función para determinar si un asiento es seleccionable
isSeatSelectable(seatInfo) {
    const selectableStatuses = ['available', 'premium'];
    
    // Verificar estado básico
    if (!selectableStatuses.includes(seatInfo.status)) {
        return false;
    }
    
    // Verificar restricciones
    if (seatInfo.restrictions.length > 0) {
        // Aquí puedes agregar lógica para verificar restricciones específicas
        const blockingRestrictions = ['age', 'medical', 'crew-only'];
        const hasBlockingRestriction = seatInfo.restrictions.some(restriction => 
            blockingRestrictions.includes(restriction.type)
        );
        
        if (hasBlockingRestriction) {
            return false;
        }
    }
    
    return true;
}
```

### **3. Implementación Visual de Estados:**
```javascript
// Función para crear elemento de asiento con estado
createSeatElement(seatInfo, isExitRow = false) {
    const seatElement = document.createElement('div');
    const baseClasses = ['seat'];
    
    // Agregar clase de estado
    baseClasses.push(seatInfo.status);
    
    // Agregar clases adicionales basadas en características
    if (seatInfo.characteristics.includes('W')) baseClasses.push('window-seat');
    if (seatInfo.characteristics.includes('A')) baseClasses.push('aisle-seat');
    if (seatInfo.characteristics.includes('E')) baseClasses.push('exit-seat');
    if (seatInfo.characteristics.includes('P')) baseClasses.push('premium-seat');
    if (isExitRow) baseClasses.push('exit-row-seat');
    
    // Agregar clase de seleccionabilidad
    if (seatInfo.selectable) {
        baseClasses.push('selectable');
    } else {
        baseClasses.push('non-selectable');
    }
    
    seatElement.className = baseClasses.join(' ');
    seatElement.dataset.seatId = seatInfo.number;
    seatElement.dataset.status = seatInfo.status;
    seatElement.dataset.selectable = seatInfo.selectable;
    
    // Contenido del asiento
    const seatContent = document.createElement('div');
    seatContent.className = 'seat-content';
    
    // Número del asiento
    const seatNumber = document.createElement('div');
    seatNumber.className = 'seat-number';
    seatNumber.textContent = this.extractColumnFromSeatNumber(seatInfo.number);
    
    // Indicador de precio si aplica
    if (seatInfo.price && seatInfo.price > 0) {
        const priceIndicator = document.createElement('div');
        priceIndicator.className = 'seat-price';
        priceIndicator.textContent = `+${seatInfo.currency}${seatInfo.price}`;
        seatContent.appendChild(priceIndicator);
    }
    
    // Indicadores de características
    const characteristicsIndicator = document.createElement('div');
    characteristicsIndicator.className = 'seat-characteristics';
    
    if (seatInfo.characteristics.includes('W')) {
        characteristicsIndicator.innerHTML += '<span class="char-icon window">🪟</span>';
    }
    if (seatInfo.characteristics.includes('A')) {
        characteristicsIndicator.innerHTML += '<span class="char-icon aisle">🚶</span>';
    }
    if (seatInfo.characteristics.includes('E')) {
        characteristicsIndicator.innerHTML += '<span class="char-icon exit">🚪</span>';
    }
    if (seatInfo.characteristics.includes('X')) {
        characteristicsIndicator.innerHTML += '<span class="char-icon extra">📏</span>';
    }
    
    seatContent.appendChild(seatNumber);
    seatContent.appendChild(characteristicsIndicator);
    seatElement.appendChild(seatContent);
    
    // Tooltip con información detallada
    const tooltip = this.createSeatTooltip(seatInfo);
    seatElement.appendChild(tooltip);
    
    // Event listener para selección (solo si es seleccionable)
    if (seatInfo.selectable) {
        seatElement.addEventListener('click', () => this.handleSeatSelection(seatInfo));
        seatElement.addEventListener('mouseenter', () => this.showSeatTooltip(seatInfo.number));
        seatElement.addEventListener('mouseleave', () => this.hideSeatTooltip(seatInfo.number));
    }
    
    return seatElement;
}

// Función para crear tooltip de asiento
createSeatTooltip(seatInfo) {
    const tooltip = document.createElement('div');
    tooltip.className = 'seat-tooltip';
    
    let tooltipContent = `
        <div class="tooltip-header">
            <strong>Seat ${seatInfo.number}</strong>
            <span class="status-badge ${seatInfo.status}">${seatInfo.status.toUpperCase()}</span>
        </div>
    `;
    
    // Información de características
    if (seatInfo.characteristics.length > 0) {
        tooltipContent += `
            <div class="tooltip-section">
                <strong>Features:</strong>
                <ul class="features-list">
        `;
        
        seatInfo.characteristics.forEach(code => {
            const feature = this.getFeatureDescription(code);
            if (feature) {
                tooltipContent += `<li>${feature}</li>`;
            }
        });
        
        tooltipContent += `</ul></div>`;
    }
    
    // Información de precio
    if (seatInfo.price && seatInfo.price > 0) {
        tooltipContent += `
            <div class="tooltip-section">
                <strong>Extra Fee:</strong> ${seatInfo.currency} ${seatInfo.price}
            </div>
        `;
    }
    
    // Restricciones
    if (seatInfo.restrictions.length > 0) {
        tooltipContent += `
            <div class="tooltip-section">
                <strong>Restrictions:</strong>
                <ul class="restrictions-list">
        `;
        
        seatInfo.restrictions.forEach(restriction => {
            tooltipContent += `<li>${restriction.description || restriction.type}</li>`;
        });
        
        tooltipContent += `</ul></div>`;
    }
    
    tooltip.innerHTML = tooltipContent;
    return tooltip;
}

// Función para obtener descripción de características
getFeatureDescription(code) {
    const descriptions = {
        'W': 'Window seat',
        'A': 'Aisle seat',
        '9': 'Middle seat',
        'E': 'Emergency exit row',
        'B': 'Bulkhead seat',
        'X': 'Extra legroom',
        'P': 'Premium seat',
        'Q': 'Quiet zone',
        'G': 'Near galley',
        'T': 'Near lavatory',
        'F': 'Front section',
        'R': 'Rear section'
    };
    
    return descriptions[code] || null;
}
```

### **4. CSS Completo para Estados:**
```css
/* Estados básicos de asientos */
.seat {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    transition: all 0.3s ease;
    border: 2px solid transparent;
    font-size: 0.8em;
    font-weight: bold;
}

/* Estados de disponibilidad */
.seat.available {
    background-color: #28a745;
    color: white;
    cursor: pointer;
    box-shadow: 0 2px 4px rgba(40, 167, 69, 0.3);
}

.seat.available:hover {
    background-color: #218838;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(40, 167, 69, 0.4);
}

.seat.occupied {
    background-color: #dc3545;
    color: white;
    cursor: not-allowed;
    opacity: 0.8;
}

.seat.blocked {
    background-color: #6c757d;
    color: white;
    cursor: not-allowed;
    opacity: 0.7;
}

.seat.reserved {
    background-color: #ffc107;
    color: #212529;
    cursor: not-allowed;
    animation: pulse 2s infinite;
}

.seat.unavailable {
    background-color: #e9ecef;
    color: #6c757d;
    cursor: not-allowed;
    border: 2px dashed #adb5bd;
}

.seat.restricted {
    background-color: #fd7e14;
    color: white;
    cursor: not-allowed;
    position: relative;
}

.seat.restricted::after {
    content: "⚠️";
    position: absolute;
    top: -5px;
    right: -5px;
    font-size: 0.7em;
}

.seat.maintenance {
    background-color: #495057;
    color: white;
    cursor: not-allowed;
    position: relative;
}

.seat.maintenance::after {
    content: "🔧";
    position: absolute;
    top: -5px;
    right: -5px;
    font-size: 0.7em;
}

.seat.premium {
    background-color: #6f42c1;
    color: white;
    cursor: pointer;
    border: 2px solid #ffd700;
    box-shadow: 0 2px 4px rgba(111, 66, 193, 0.3);
}

.seat.premium:hover {
    background-color: #5a32a3;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(111, 66, 193, 0.4);
}

/* Estado seleccionado */
.seat.selected {
    background-color: #007bff;
    color: white;
    border: 3px solid #0056b3;
    transform: scale(1.1);
    z-index: 5;
}

/* Características especiales */
.seat.window-seat {
    border-left: 3px solid #17a2b8;
}

.seat.aisle-seat {
    border-right: 3px solid #20c997;
}

.seat.exit-row-seat {
    border: 2px dashed #ffc107;
    background-image: linear-gradient(45deg, transparent 25%, rgba(255, 193, 7, 0.1) 25%, rgba(255, 193, 7, 0.1) 50%, transparent 50%, transparent 75%, rgba(255, 193, 7, 0.1) 75%);
    background-size: 8px 8px;
}

/* Contenido del asiento */
.seat-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    position: relative;
}

.seat-number {
    font-size: 1em;
    font-weight: bold;
    line-height: 1;
}

.seat-price {
    font-size: 0.6em;
    position: absolute;
    bottom: 1px;
    right: 2px;
    background: rgba(0, 0, 0, 0.7);
    color: white;
    padding: 1px 3px;
    border-radius: 2px;
}

.seat-characteristics {
    position: absolute;
    top: -2px;
    left: -2px;
    display: flex;
    gap: 1px;
}

.char-icon {
    font-size: 0.6em;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 50%;
    width: 12px;
    height: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Tooltip */
.seat-tooltip {
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.9);
    color: white;
    padding: 8px 12px;
    border-radius: 6px;
    font-size: 0.8em;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s;
    z-index: 10;
    min-width: 200px;
    white-space: normal;
}

.seat:hover .seat-tooltip {
    opacity: 1;
}

.tooltip-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
    padding-bottom: 4px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.status-badge {
    font-size: 0.7em;
    padding: 2px 6px;
    border-radius: 10px;
    font-weight: bold;
}

.status-badge.available { background: #28a745; }
.status-badge.occupied { background: #dc3545; }
.status-badge.blocked { background: #6c757d; }
.status-badge.reserved { background: #ffc107; color: #212529; }
.status-badge.premium { background: #6f42c1; }

.tooltip-section {
    margin-bottom: 6px;
}

.features-list,
.restrictions-list {
    margin: 4px 0;
    padding-left: 16px;
    font-size: 0.9em;
}

.features-list li,
.restrictions-list li {
    margin-bottom: 2px;
}

/* Animaciones */
@keyframes pulse {
    0% { opacity: 1; }
    50% { opacity: 0.7; }
    100% { opacity: 1; }
}

/* Responsividad */
@media (max-width: 768px) {
    .seat {
        width: 35px;
        height: 35px;
        font-size: 0.7em;
    }
    
    .seat-tooltip {
        font-size: 0.7em;
        min-width: 180px;
    }
}

@media (max-width: 480px) {
    .seat {
        width: 30px;
        height: 30px;
        font-size: 0.6em;
    }
    
    .char-icon {
        width: 10px;
        height: 10px;
        font-size: 0.5em;
    }
}
```

---

## 🏗️ **CONSTRUCCIÓN COMPLETA DEL DECK**

### **1. Análisis Completo de Configuración:**
```javascript
// Clase principal para construcción del deck
class DeckBuilder {
    constructor(seatMapData) {
        this.seatMapData = seatMapData;
        this.deckConfiguration = null;
        this.seats = new Map();
        this.facilities = new Map();
        this.layoutInfo = null;
        this.wingInfo = null;
        this.aisleInfo = null;
        this.exitRows = [];
    }
    
    // Función principal para construir el deck
    buildDeck() {
        console.log('Starting deck construction...');
        
        // Paso 1: Extraer y validar datos
        this.extractDeckData();
        
        // Paso 2: Analizar configuración del deck
        this.analyzeDeckConfiguration();
        
        // Paso 3: Procesar asientos
        this.processSeats();
        
        // Paso 4: Procesar instalaciones
        this.processFacilities();
        
        // Paso 5: Detectar características especiales
        this.detectSpecialFeatures();
        
        // Paso 6: Validar y corregir layout
        this.validateAndCorrectLayout();
        
        // Paso 7: Construir grilla visual
        this.buildVisualGrid();
        
        console.log('Deck construction completed');
        return this.getConstructionSummary();
    }
    
    // Extraer datos del deck desde la respuesta de Amadeus
    extractDeckData() {
        if (!this.seatMapData.decks || this.seatMapData.decks.length === 0) {
            throw new Error('No deck data available');
        }
        
        // Usar el primer deck (MAIN) por defecto
        const mainDeck = this.seatMapData.decks.find(deck => deck.deckType === 'MAIN') 
                         || this.seatMapData.decks[0];
        
        this.deckConfiguration = mainDeck.deckConfiguration || {};
        
        // Procesar asientos
        if (mainDeck.seats) {
            mainDeck.seats.forEach(seat => {
                const processedSeat = this.processSeatData(seat);
                this.seats.set(seat.number, processedSeat);
            });
        }
        
        // Procesar instalaciones
        if (mainDeck.facilities) {
            mainDeck.facilities.forEach(facility => {
                const processedFacility = this.processFacilityData(facility);
                this.facilities.set(facility.position || `${facility.row}${facility.column}`, processedFacility);
            });
        }
        
        console.log(`Extracted ${this.seats.size} seats and ${this.facilities.size} facilities`);
    }
    
    // Procesar datos individuales de asiento
    processSeatData(seat) {
        return {
            number: seat.number,
            cabin: seat.cabin,
            coordinates: seat.coordinates,
            characteristics: seat.characteristicsCodes || [],
            availability: this.processSeatAvailability(seat),
            row: this.extractRowFromSeatNumber(seat.number),
            column: this.extractColumnFromSeatNumber(seat.number),
            type: this.determineSeatType(seat.characteristicsCodes || []),
            price: this.extractSeatPrice(seat),
            restrictions: this.extractSeatRestrictions(seat)
        };
    }
    
    // Procesar datos de instalaciones
    processFacilityData(facility) {
        return {
            code: facility.code,
            type: this.determineFacilityType(facility.code),
            position: facility.position || `${facility.row}${facility.column}`,
            coordinates: facility.coordinates,
            row: facility.row,
            column: facility.column,
            description: this.getFacilityDescription(facility.code)
        };
    }
    
    // Analizar configuración completa del deck
    analyzeDeckConfiguration() {
        this.layoutInfo = {
            totalRows: 0,
            totalColumns: 0,
            rowRange: { min: Infinity, max: -Infinity },
            columnRange: { min: Infinity, max: -Infinity },
            coordinateRange: {
                x: { min: Infinity, max: -Infinity },
                y: { min: Infinity, max: -Infinity }
            },
            seatsByRow: new Map(),
            seatsByColumn: new Map(),
            facilitiesByRow: new Map(),
            cabinSections: new Map(),
            aisleConfiguration: 'unknown'
        };
        
        // Analizar rango de asientos
        this.seats.forEach((seat, seatNumber) => {
            const row = seat.row;
            const column = seat.column;
            const coords = seat.coordinates;
            
            // Actualizar rangos
            this.layoutInfo.rowRange.min = Math.min(this.layoutInfo.rowRange.min, row);
            this.layoutInfo.rowRange.max = Math.max(this.layoutInfo.rowRange.max, row);
            
            const colNum = this.columnLetterToNumber(column);
            this.layoutInfo.columnRange.min = Math.min(this.layoutInfo.columnRange.min, colNum);
            this.layoutInfo.columnRange.max = Math.max(this.layoutInfo.columnRange.max, colNum);
            
            if (coords) {
                this.layoutInfo.coordinateRange.x.min = Math.min(this.layoutInfo.coordinateRange.x.min, coords.x);
                this.layoutInfo.coordinateRange.x.max = Math.max(this.layoutInfo.coordinateRange.x.max, coords.x);
                this.layoutInfo.coordinateRange.y.min = Math.min(this.layoutInfo.coordinateRange.y.min, coords.y);
                this.layoutInfo.coordinateRange.y.max = Math.max(this.layoutInfo.coordinateRange.y.max, coords.y);
            }
            
            // Agrupar por fila
            if (!this.layoutInfo.seatsByRow.has(row)) {
                this.layoutInfo.seatsByRow.set(row, new Map());
            }
            this.layoutInfo.seatsByRow.get(row).set(column, seat);
            
            // Agrupar por columna
            if (!this.layoutInfo.seatsByColumn.has(column)) {
                this.layoutInfo.seatsByColumn.set(column, new Map());
            }
            this.layoutInfo.seatsByColumn.get(column).set(row, seat);
        });
        
        // Calcular totales
        this.layoutInfo.totalRows = this.layoutInfo.rowRange.max - this.layoutInfo.rowRange.min + 1;
        this.layoutInfo.totalColumns = this.layoutInfo.columnRange.max - this.layoutInfo.columnRange.min + 1;
        
        // Analizar configuración de cabina
        this.analyzeCabinSections();
        
        console.log('Deck configuration analyzed:', this.layoutInfo);
    }
    
    // Analizar secciones de cabina
    analyzeCabinSections() {
        const cabinTypes = new Set();
        
        this.seats.forEach(seat => {
            if (seat.cabin) {
                cabinTypes.add(seat.cabin);
                
                if (!this.layoutInfo.cabinSections.has(seat.cabin)) {
                    this.layoutInfo.cabinSections.set(seat.cabin, {
                        seats: new Map(),
                        rowRange: { min: Infinity, max: -Infinity },
                        characteristics: new Set()
                    });
                }
                
                const section = this.layoutInfo.cabinSections.get(seat.cabin);
                section.seats.set(seat.number, seat);
                section.rowRange.min = Math.min(section.rowRange.min, seat.row);
                section.rowRange.max = Math.max(section.rowRange.max, seat.row);
                
                seat.characteristics.forEach(char => section.characteristics.add(char));
            }
        });
        
        console.log(`Found ${cabinTypes.size} cabin sections:`, Array.from(cabinTypes));
    }
    
    // Detectar características especiales
    detectSpecialFeatures() {
        // Detectar salidas de emergencia
        this.exitRows = this.detectExitRows();
        
        // Detectar pasillos
        this.aisleInfo = this.detectAisles();
        
        // Detectar posición de alas
        this.wingInfo = this.detectWingPosition();
        
        console.log('Special features detected:', {
            exitRows: this.exitRows,
            aisles: this.aisleInfo.configuration,
            wings: this.wingInfo.position
        });
    }
    
    // Detectar filas de salida de emergencia
    detectExitRows() {
        const exitRows = new Set();
        
        // Método 1: Desde configuración del deck
        if (this.deckConfiguration.exitRows) {
            this.deckConfiguration.exitRows.forEach(row => {
                exitRows.add(parseInt(row));
            });
        }
        
        if (this.deckConfiguration.exitRowsX) {
            this.deckConfiguration.exitRowsX.forEach(row => {
                exitRows.add(row);
            });
        }
        
        // Método 2: Desde características de asientos
        this.seats.forEach(seat => {
            if (seat.characteristics.includes('E')) {
                exitRows.add(seat.row);
            }
        });
        
        return Array.from(exitRows).sort((a, b) => a - b);
    }
    
    // Detectar configuración de pasillos
    detectAisles() {
        const aisleInfo = {
            positions: new Set(),
            byRow: new Map(),
            configuration: 'unknown',
            pattern: []
        };
        
        // Analizar cada fila para encontrar pasillos
        this.layoutInfo.seatsByRow.forEach((rowSeats, row) => {
            const columns = Array.from(rowSeats.keys()).sort((a, b) => 
                this.columnLetterToNumber(a) - this.columnLetterToNumber(b)
            );
            
            const aisleColumns = [];
            const sectionSizes = [];
            let currentSectionSize = 0;
            
            columns.forEach((column, index) => {
                const seat = rowSeats.get(column);
                currentSectionSize++;
                
                if (seat.characteristics.includes('A')) {
                    aisleColumns.push(column);
                    sectionSizes.push(currentSectionSize);
                    currentSectionSize = 0;
                }
            });
            
            // Agregar la última sección
            if (currentSectionSize > 0) {
                sectionSizes.push(currentSectionSize);
            }
            
            if (aisleColumns.length > 0) {
                aisleInfo.byRow.set(row, aisleColumns);
                
                // Determinar patrón de configuración
                const pattern = sectionSizes.join('-');
                aisleInfo.pattern.push(pattern);
            }
        });
        
        // Determinar configuración más común
        const patternCounts = new Map();
        aisleInfo.pattern.forEach(pattern => {
            patternCounts.set(pattern, (patternCounts.get(pattern) || 0) + 1);
        });
        
        let mostCommonPattern = '';
        let maxCount = 0;
        patternCounts.forEach((count, pattern) => {
            if (count > maxCount) {
                maxCount = count;
                mostCommonPattern = pattern;
            }
        });
        
        aisleInfo.configuration = mostCommonPattern || 'unknown';
        
        return aisleInfo;
    }
    
    // Detectar posición de alas
    detectWingPosition() {
        const wingInfo = {
            startRow: null,
            endRow: null,
            position: 'unknown',
            affectedSeats: new Set(),
            confidence: 'low'
        };
        
        // Método 1: Por código de aeronave
        if (this.seatMapData.aircraft && this.seatMapData.aircraft.code) {
            const aircraftWingData = this.getWingDataByAircraft(this.seatMapData.aircraft.code);
            if (aircraftWingData) {
                wingInfo.startRow = aircraftWingData.startRow;
                wingInfo.endRow = aircraftWingData.endRow;
                wingInfo.position = aircraftWingData.position;
                wingInfo.confidence = 'high';
            }
        }
        
        // Método 2: Por características de asientos
        this.seats.forEach(seat => {
            const wingCodes = seat.characteristics.filter(code => 
                ['S', 'V', 'O'].includes(code)
            );
            
            if (wingCodes.length > 0) {
                if (!wingInfo.startRow || seat.row < wingInfo.startRow) {
                    wingInfo.startRow = seat.row;
                }
                if (!wingInfo.endRow || seat.row > wingInfo.endRow) {
                    wingInfo.endRow = seat.row;
                }
                
                wingInfo.affectedSeats.add(seat.number);
                if (wingInfo.confidence === 'low') {
                    wingInfo.confidence = 'medium';
                }
            }
        });
        
        // Método 3: Estimación basada en longitud total
        if (!wingInfo.startRow && this.layoutInfo.totalRows > 0) {
            const estimatedStart = Math.floor(this.layoutInfo.totalRows * 0.35) + this.layoutInfo.rowRange.min;
            const estimatedEnd = Math.floor(this.layoutInfo.totalRows * 0.65) + this.layoutInfo.rowRange.min;
            
            wingInfo.startRow = estimatedStart;
            wingInfo.endRow = estimatedEnd;
            wingInfo.position = 'estimated';
            wingInfo.confidence = 'low';
        }
        
        return wingInfo;
    }
    
    // Validar y corregir layout
    validateAndCorrectLayout() {
        console.log('Validating and correcting layout...');
        
        // Validar coordenadas
        this.validateCoordinates();
        
        // Corregir posicionamiento de asientos
        this.correctSeatPositioning();
        
        // Validar instalaciones
        this.validateFacilities();
        
        console.log('Layout validation completed');
    }
    
    // Validar coordenadas de asientos
    validateCoordinates() {
        let correctedCount = 0;
        
        this.seats.forEach((seat, seatNumber) => {
            if (!seat.coordinates) {
                // Generar coordenadas basadas en fila y columna
                seat.coordinates = {
                    x: seat.row,
                    y: this.columnLetterToNumber(seat.column)
                };
                correctedCount++;
            }
        });
        
        if (correctedCount > 0) {
            console.log(`Generated coordinates for ${correctedCount} seats`);
        }
    }
    
    // Corregir posicionamiento de asientos
    correctSeatPositioning() {
        // Verificar y corregir asientos mal posicionados
        this.layoutInfo.seatsByRow.forEach((rowSeats, row) => {
            const columns = Array.from(rowSeats.keys()).sort((a, b) => 
                this.columnLetterToNumber(a) - this.columnLetterToNumber(b)
            );
            
            // Verificar continuidad de columnas
            for (let i = 0; i < columns.length - 1; i++) {
                const currentCol = this.columnLetterToNumber(columns[i]);
                const nextCol = this.columnLetterToNumber(columns[i + 1]);
                
                // Si hay un salto mayor a 1, podría indicar un pasillo
                if (nextCol - currentCol > 1) {
                    console.log(`Potential aisle detected between ${columns[i]} and ${columns[i + 1]} in row ${row}`);
                }
            }
        });
    }
    
    // Construir grilla visual
    buildVisualGrid() {
        const container = document.getElementById('seat-map-container');
        if (!container) {
            console.error('Seat map container not found');
            return;
        }
        
        // Limpiar contenedor
        container.innerHTML = '';
        
        // Crear estructura principal
        const seatMapWrapper = document.createElement('div');
        seatMapWrapper.className = 'seat-map-wrapper';
        
        // Agregar información del vuelo
        const flightInfo = this.createFlightInfoHeader();
        seatMapWrapper.appendChild(flightInfo);
        
        // Agregar leyenda
        const legend = this.createLegend();
        seatMapWrapper.appendChild(legend);
        
        // Crear grilla de asientos
        const seatGrid = this.createSeatGrid();
        seatMapWrapper.appendChild(seatGrid);
        
        // Agregar indicadores especiales
        this.addSpecialIndicators(seatMapWrapper);
        
        container.appendChild(seatMapWrapper);
        
        console.log('Visual grid constructed');
    }
    
    // Crear grilla de asientos
    createSeatGrid() {
        const gridContainer = document.createElement('div');
        gridContainer.className = 'seat-grid-container';
        
        // Usar coordenadas para posicionamiento
        const useCoordinates = this.shouldUseCoordinates();
        
        if (useCoordinates) {
            return this.createCoordinateBasedGrid();
        } else {
            return this.createRowBasedGrid();
        }
    }
    
    // Crear grilla basada en coordenadas
    createCoordinateBasedGrid() {
        const gridContainer = document.createElement('div');
        gridContainer.className = 'coordinate-based-grid';
        
        const xRange = this.layoutInfo.coordinateRange.x;
        const yRange = this.layoutInfo.coordinateRange.y;
        
        // Crear mapa de coordenadas
        const coordinatesMap = new Map();
        
        // Mapear asientos
        this.seats.forEach(seat => {
            if (seat.coordinates) {
                const key = `${seat.coordinates.x},${seat.coordinates.y}`;
                coordinatesMap.set(key, { type: 'seat', data: seat });
            }
        });
        
        // Mapear instalaciones
        this.facilities.forEach(facility => {
            if (facility.coordinates) {
                const key = `${facility.coordinates.x},${facility.coordinates.y}`;
                coordinatesMap.set(key, { type: 'facility', data: facility });
            }
        });
        
        // Crear filas
        for (let x = xRange.min; x <= xRange.max; x++) {
            const rowElement = document.createElement('div');
            rowElement.className = 'seat-row coordinate-row';
            rowElement.dataset.row = x;
            
            // Verificar si es fila de salida
            const isExitRow = this.exitRows.includes(x);
            if (isExitRow) {
                rowElement.classList.add('exit-row');
            }
            
            // Etiqueta de fila
            const rowLabel = document.createElement('div');
            rowLabel.className = 'row-label';
            if (isExitRow) {
                rowLabel.innerHTML = `${x}<br><span class="exit-text">EXIT</span>`;
            } else {
                rowLabel.textContent = x;
            }
            rowElement.appendChild(rowLabel);
            
            // Crear columnas
            for (let y = yRange.min; y <= yRange.max; y++) {
                const key = `${x},${y}`;
                const cellElement = document.createElement('div');
                cellElement.className = 'grid-cell';
                
                if (coordinatesMap.has(key)) {
                    const item = coordinatesMap.get(key);
                    const element = this.createGridElement(item, isExitRow);
                    cellElement.appendChild(element);
                } else {
                    // Verificar si es posición de pasillo
                    if (this.isAislePosition(x, y)) {
                        cellElement.className += ' aisle-cell';
                        cellElement.innerHTML = '<div class="aisle-indicator"></div>';
                    } else {
                        cellElement.className += ' empty-cell';
                    }
                }
                
                rowElement.appendChild(cellElement);
            }
            
            gridContainer.appendChild(rowElement);
        }
        
        return gridContainer;
    }
    
    // Crear elemento de grilla (asiento o instalación)
    createGridElement(item, isExitRow) {
        if (item.type === 'seat') {
            return this.createSeatElement(item.data, isExitRow);
        } else if (item.type === 'facility') {
            return this.createFacilityElement(item.data);
        }
        
        return document.createElement('div');
    }
    
    // Obtener resumen de construcción
    getConstructionSummary() {
        return {
            success: true,
            stats: {
                totalSeats: this.seats.size,
                totalFacilities: this.facilities.size,
                totalRows: this.layoutInfo.totalRows,
                totalColumns: this.layoutInfo.totalColumns,
                exitRows: this.exitRows.length,
                aisleConfiguration: this.aisleInfo.configuration,
                wingPosition: this.wingInfo.position,
                cabinSections: this.layoutInfo.cabinSections.size
            },
            features: {
                exitRows: this.exitRows,
                aisles: this.aisleInfo,
                wings: this.wingInfo,
                cabins: Array.from(this.layoutInfo.cabinSections.keys())
            }
        };
    }
    
    // Funciones auxiliares
    extractRowFromSeatNumber(seatNumber) {
        return parseInt(seatNumber.match(/\d+/)[0]);
    }
    
    extractColumnFromSeatNumber(seatNumber) {
        return seatNumber.match(/[A-Z]+/)[0];
    }
    
    columnLetterToNumber(letter) {
        return letter.charCodeAt(0) - 64; // A=1, B=2, etc.
    }
    
    shouldUseCoordinates() {
        // Usar coordenadas si están disponibles para la mayoría de asientos
        let seatsWithCoords = 0;
        this.seats.forEach(seat => {
            if (seat.coordinates) seatsWithCoords++;
        });
        
        return seatsWithCoords / this.seats.size > 0.8;
    }
}
```

---

## 🔧 **IMPLEMENTACIÓN COMPLETA DEL ENDPOINT**

### **1. Controlador Principal:**
```javascript
// src/controllers/seat-map-controller.js
const { getSeatMap } = require('../services/amadeus-service');
const { DeckBuilder } = require('../services/deck-builder');

class SeatMapController {
    async getSeatMapHandler(req, res) {
        try {
            const { flightOffer } = req.body;
            
            // Validaciones
            if (!flightOffer) {
                return res.status(400).json({ 
                    error: 'Flight offer is required',
                    code: 'MISSING_FLIGHT_OFFER'
                });
            }

            if (!this.validateFlightOffer(flightOffer)) {
                return res.status(400).json({ 
                    error: 'Invalid flight offer structure',
                    code: 'INVALID_FLIGHT_OFFER'
                });
            }

            console.log('Processing seat map request for flight:', flightOffer.id);

            // Llamar al servicio de Amadeus
            const seatMapData = await getSeatMap(flightOffer);
            
            if (!seatMapData) {
                return res.status(404).json({ 
                    error: 'No seat map data available',
                    code: 'NO_SEAT_MAP_DATA'
                });
            }

            // Construir deck usando DeckBuilder
            const deckBuilder = new DeckBuilder(seatMapData);
            const constructionResult = deckBuilder.buildDeck();

            // Preparar respuesta
            const response = {
                success: true,
                data: {
                    flight: seatMapData.flight,
                    aircraft: seatMapData.aircraft,
                    seatMap: constructionResult,
                    metadata: {
                        processedAt: new Date().toISOString(),
                        source: 'amadeus',
                        version: '2.0'
                    }
                }
            };

            res.json(response);
            
        } catch (error) {
            console.error('Error getting seat map:', error);
            
            // Manejo específico de errores de Amadeus
            if (error.response && error.response.result && error.response.result.errors) {
                const amadeusError = error.response.result.errors[0];
                return res.status(error.response.statusCode).json({
                    error: amadeusError.title,
                    detail: amadeusError.detail,
                    code: amadeusError.code,
                    source: 'amadeus'
                });
            }

            // Error genérico
            res.status(500).json({
                error: 'Failed to get seat map',
                message: error.message,
                code: 'INTERNAL_ERROR'
            });
        }
    }

    validateFlightOffer(flightOffer) {
        const requiredFields = ['id', 'itineraries', 'price', 'travelerPricings'];
        
        for (const field of requiredFields) {
            if (!flightOffer[field]) {
                console.error(`Missing required field: ${field}`);
                return false;
            }
        }

        if (!Array.isArray(flightOffer.itineraries) || flightOffer.itineraries.length === 0) {
            console.error('Invalid itineraries structure');
            return false;
        }

        return true;
    }
}

module.exports = new SeatMapController();
```

### **2. Servicio de Amadeus:**
```javascript
// src/services/amadeus-service.js
const Amadeus = require('amadeus');

class AmadeusService {
    constructor() {
        this.amadeus = new Amadeus({
            clientId: process.env.AMADEUS_CLIENT_ID,
            clientSecret: process.env.AMADEUS_CLIENT_SECRET,
            hostname: process.env.AMADEUS_HOSTNAME || 'production'
        });
    }

    async getSeatMap(flightOffer) {
        try {
            console.log('Calling Amadeus Seat Map API...');
            
            // Formatear flight offer para Amadeus
            const formattedOffer = this.formatFlightOfferForAmadeus(flightOffer);
            
            // Llamar a la API
            const response = await this.amadeus.shopping.seatmaps.post(
                JSON.stringify({
                    data: [formattedOffer]
                })
            );

            console.log(`Amadeus API response: ${response.data.length} seat maps`);

            if (!response.data || response.data.length === 0) {
                throw new Error('No seat map data returned from Amadeus');
            }

            // Procesar y retornar el primer seat map
            return this.processSeatMapResponse(response.data[0]);

        } catch (error) {
            console.error('Amadeus API error:', error);
            
            if (error.response) {
                console.error('Response status:', error.response.statusCode);
                console.error('Response body:', error.response.body);
            }
            
            throw error;
        }
    }

    formatFlightOfferForAmadeus(flightOffer) {
        return {
            type: "flight-offer",
            id: flightOffer.id,
            source: flightOffer.source || "GDS",
            instantTicketingRequired: flightOffer.instantTicketingRequired || false,
            nonHomogeneous: flightOffer.nonHomogeneous || false,
            oneWay: flightOffer.oneWay || false,
            lastTicketingDate: flightOffer.lastTicketingDate,
            lastTicketingDateTime: flightOffer.lastTicketingDateTime,
            numberOfBookableSeats: flightOffer.numberOfBookableSeats || 1,
            itineraries: flightOffer.itineraries,
            price: flightOffer.price,
            pricingOptions: flightOffer.pricingOptions,
            validatingAirlineCodes: flightOffer.validatingAirlineCodes,
            travelerPricings: flightOffer.travelerPricings
        };
    }

    processSeatMapResponse(seatMapData) {
        return {
            type: seatMapData.type,
            id: seatMapData.id,
            flight: seatMapData.flight,
            aircraft: seatMapData.aircraft,
            class: seatMapData.class,
            flightOfferId: seatMapData.flightOfferId,
            segmentId: seatMapData.segmentId,
            decks: seatMapData.decks || []
        };
    }
}

module.exports = new AmadeusService();
```

---

## 📱 **FLUJO COMPLETO DE IMPLEMENTACIÓN**

### **1. Diagrama de Flujo:**
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │     Backend      │    │   Amadeus API   │
│                 │    │                  │    │                 │
│ 1. Flight Offer │───▶│ 2. Validate      │───▶│ 3. Seat Map     │
│                 │    │    Request       │    │    Request      │
└─────────────────┘    └──────────────────┘