#!/bin/bash

# Script para subir SeatMap Pro a GitHub
echo "🚀 Configurando respaldo en GitHub..."

# Paso 1: Inicializar git (si no está inicializado)
if [ ! -d ".git" ]; then
    echo "📁 Inicializando repositorio Git..."
    git init
fi

# Paso 2: Agregar todos los archivos
echo "📝 Agregando archivos al repositorio..."
git add .

# Paso 3: Crear commit inicial
echo "💾 Creando commit inicial..."
git commit -m "Initial commit: SeatMap Pro with Amadeus integration

- Complete flight booking application with real-time seat selection
- Amadeus API integration for flight search and seat maps  
- React + TypeScript frontend with TailwindCSS
- Convex backend with real-time database
- User authentication with Convex Auth
- Interactive seat map visualization
- Airport search with caching
- Flight search with results display
- Responsive design for mobile and desktop"

echo "✅ Repositorio local configurado!"
echo ""
echo "🔗 Ahora necesitas:"
echo "1. Crear un repositorio en GitHub"
echo "2. Ejecutar estos comandos:"
echo ""
echo "git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git"
echo "git branch -M main" 
echo "git push -u origin main"
echo ""
echo "📋 Información del proyecto:"
echo "- Convex Deployment: flexible-firefly-91"
echo "- Dashboard: https://dashboard.convex.dev/d/flexible-firefly-91"
