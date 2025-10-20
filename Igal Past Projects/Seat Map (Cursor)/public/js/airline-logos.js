// Airline Logos Dynamic Loader
class AirlineLogosLoader {
    constructor() {
        this.logosContainer = document.querySelector('.airline-logos-track');
        this.logosFolder = 'content/airlines-logo/';
        this.knownAirlines = {
            'american-airlines-seeklogo.png': 'American Airlines',
            'delta-seeklogo.png': 'Delta Airlines',
            'turkish-airlines-seeklogo.png': 'Turkish Airlines'
        };
        this.init();
    }

    async init() {
        try {
            // Try to fetch the directory listing or use known logos
            await this.loadLogos();
        } catch (error) {
            console.log('Using fallback logo loading method');
            this.loadKnownLogos();
        }
    }

    async loadLogos() {
        // Try to get directory listing (this might not work in all environments)
        try {
            const response = await fetch(this.logosFolder);
            if (response.ok) {
                // If we can access the folder, try to parse it
                const text = await response.text();
                this.parseDirectoryListing(text);
            } else {
                throw new Error('Cannot access folder');
            }
        } catch (error) {
            // Fallback to known logos
            this.loadKnownLogos();
        }
    }

    parseDirectoryListing(html) {
        // Simple regex to find PNG files
        const pngFiles = html.match(/[a-zA-Z0-9\-_]+\.png/g);
        if (pngFiles) {
            this.createLogoElements(pngFiles);
        } else {
            this.loadKnownLogos();
        }
    }

    loadKnownLogos() {
        // Use the known logos as fallback
        const logoFiles = Object.keys(this.knownAirlines);
        this.createLogoElements(logoFiles);
    }

    createLogoElements(logoFiles) {
        if (!this.logosContainer) return;

        // Clear existing content
        this.logosContainer.innerHTML = '';

        // Create logo elements for each file
        logoFiles.forEach(filename => {
            const logoElement = this.createLogoElement(filename);
            this.logosContainer.appendChild(logoElement);
        });

        // Duplicate logos for infinite scroll effect
        logoFiles.forEach(filename => {
            const logoElement = this.createLogoElement(filename);
            this.logosContainer.appendChild(logoElement);
        });

        // Update animation duration based on number of logos
        this.updateAnimationDuration(logoFiles.length);
    }

    createLogoElement(filename) {
        const logoDiv = document.createElement('div');
        logoDiv.className = 'airline-logo';

        const img = document.createElement('img');
        img.src = this.logosFolder + filename;
        img.alt = this.knownAirlines[filename] || filename.replace('.png', '').replace(/-/g, ' ');
        img.className = 'airline-logo-img';

        // Add error handling
        img.onerror = () => {
            img.style.display = 'none';
            const fallback = document.createElement('div');
            fallback.className = 'airline-fallback';
            fallback.textContent = this.getFallbackText(filename);
            logoDiv.appendChild(fallback);
        };

        logoDiv.appendChild(img);
        return logoDiv;
    }

    getFallbackText(filename) {
        const name = filename.replace('.png', '').replace(/-/g, ' ');
        return name.split(' ').map(word => word.charAt(0).toUpperCase()).join('');
    }

    updateAnimationDuration(logoCount) {
        // Adjust animation speed based on number of logos
        const baseDuration = 30; // seconds
        const adjustedDuration = Math.max(baseDuration, logoCount * 2);
        
        const track = document.querySelector('.airline-logos-track');
        if (track) {
            track.style.animationDuration = `${adjustedDuration}s`;
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new AirlineLogosLoader();
});

// Export for potential use in other scripts
window.AirlineLogosLoader = AirlineLogosLoader; 