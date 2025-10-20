#!/bin/bash

echo "🚀 Configurando backup en GitHub para SeatMap Pro..."

# Verificar si git está inicializado
if [ ! -d ".git" ]; then
    echo "📁 Inicializando repositorio Git..."
    git init
fi

# Agregar todos los archivos
echo "📝 Agregando archivos al repositorio..."
git add .

# Crear commit inicial
echo "💾 Creando commit..."
git commit -m "SeatMap Pro - Complete flight booking app with Amadeus integration

✈️ Features:
- Real-time flight search with Amadeus API
- Interactive seat maps with availability
- User authentication with Convex Auth
- Airport search with intelligent caching
- Responsive design for all devices
- Exit row and seat type detection

🛠️ Tech Stack:
- Frontend: React + TypeScript + TailwindCSS
- Backend: Convex real-time database
- API: Amadeus Travel API integration
- Auth: Convex Auth system

📊 Deployment: flexible-firefly-91
🔗 Dashboard: https://dashboard.convex.dev/d/flexible-firefly-91"

echo ""
echo "✅ Repositorio local listo!"
echo ""
echo "🔗 Próximos pasos:"
echo "1. Ve a GitHub.com y crea un nuevo repositorio"
echo "2. Copia el nombre del repositorio"
echo "3. Ejecuta estos comandos (reemplaza TU_USUARIO y TU_REPO):"
echo ""
echo "   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "📋 Información del proyecto:"
echo "- Convex Deployment: flexible-firefly-91"
echo "- Dashboard: https://dashboard.convex.dev/d/flexible-firefly-91"
echo "- Variables de entorno requeridas: AMADEUS_API_KEY, AMADEUS_API_SECRET"
