// Interface Navigation Management
class InterfaceManager {
    constructor() {
        this.searchInterface = document.getElementById('searchInterface');
        this.resultsSection = document.getElementById('results');
        this.searchResults = document.getElementById('searchResults');
        this.seatMapSection = document.getElementById('seatMapSection');
        this.backButton = document.getElementById('backButton');
        this.seatMapContainer = document.getElementById('seatMap');
    }

    // Show search interface (form + results)
    showSearchInterface() {
        this.searchInterface.style.display = 'block';
        this.resultsSection.style.display = 'block';
        if (this.searchResults) {
            this.searchResults.style.display = 'block';
        }
        this.seatMapSection.style.display = 'none';
        this.backButton.style.display = 'none';
        
        // Clear seat map if it exists
        if (this.seatMapContainer) {
            this.seatMapContainer.innerHTML = '';
        }
        
        // Restore all select flight buttons to their original state
        this.restoreFlightButtons();
    }

    // Show seat map interface (hide search and results)
    showSeatMapInterface() {
        this.searchInterface.style.display = 'none';
        this.resultsSection.style.display = 'none';
        if (this.searchResults) {
            this.searchResults.style.display = 'none';
        }
        this.seatMapSection.style.display = 'block';
        this.backButton.style.display = 'block';
    }

    // Initialize the interface
    init() {
        // Start with search interface visible
        this.showSearchInterface();
    }
    
    // Restore all flight selection buttons to their original state
    restoreFlightButtons() {
        const selectButtons = document.querySelectorAll('.select-flight');
        selectButtons.forEach(button => {
            // Re-enable the button
            button.disabled = false;
            // Restore original text
            button.innerHTML = 'Seleccionar';
        });
    }
}

// Global interface manager instance
let interfaceManager;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    interfaceManager = new InterfaceManager();
    interfaceManager.init();
});

// Global function for back button
function showSearchInterface() {
    if (interfaceManager) {
        interfaceManager.showSearchInterface();
    }
}

// Global function to show seat map
function showSeatMapInterface() {
    if (interfaceManager) {
        interfaceManager.showSeatMapInterface();
    }
} 