#!/bin/bash
# =============================================================================
# Script de ejecución para RunPod - Exportar Gemma 4 a LiteRT-LM
# =============================================================================
# Este script ejecuta la exportación completa en RunPod, mostrando
# el uso de RAM en tiempo real para evitar sorpresas.
#
# Uso:
#   chmod +x scripts/runpod_run.sh
#   ./scripts/runpod_run.sh
# =============================================================================

set -e

VENV_DIR="/workspace/gemma4-venv"

echo "=========================================="
echo "🚀 Exportando Gemma 4 en RunPod"
echo "=========================================="
echo ""

# Activar venv
if [ -f "${VENV_DIR}/bin/activate" ]; then
    echo "📦 Activando virtual environment..."
    source "${VENV_DIR}/bin/activate"
else
    echo "❌ No se encontró el virtual environment en ${VENV_DIR}"
    echo "   Ejecuta primero: ./scripts/runpod_setup.sh"
    exit 1
fi

# Verificar login en HuggingFace
if ! python3 -c "from huggingface_hub import HfApi; HfApi().whoami()" 2>/dev/null; then
    echo "❌ No estás logueado en HuggingFace."
    echo "   Ejecuta: huggingface-cli login"
    exit 1
fi

echo "✅ Sesión de HuggingFace verificada."
echo ""

# Mostrar configuración antes de empezar
echo "📋 Configuración actual:"
echo "   - Modelo: alvarog1318/gemma4-vision-crop-deseases_16bit"
echo "   - Cuantización: int4_blockwise_256.json (~2.5-3 GB)"
echo "   - Prefill lengths: [256, 512, 1024, 2048]"
echo "   - Cache length: 4096"
echo "   - Visión encoder: SÍ (EXPORT_VISION_ENCODER = True)"
echo "   - Task: image_text_to_text"
echo "   - Directorio de salida: /workspace/gemma4_litertlm_export"
echo ""

# Verificar que el script de exportación existe
if [ ! -f "scripts/convert_gemma4_cloud.py" ]; then
    echo "❌ No se encontró scripts/convert_gemma4_cloud.py"
    echo "   Asegúrate de ejecutar este script desde la raíz del proyecto."
    exit 1
fi

# Iniciar monitoreo de RAM en segundo plano
LOG_FILE="/workspace/ram_monitor.log"
echo "💾 Monitoreo de RAM activado (log: $LOG_FILE)"
echo "$(date '+%Y-%m-%d %H:%M:%S') - Inicio de exportación" > "$LOG_FILE"

# Función para mostrar RAM cada 30 segundos
monitor_ram() {
    while true; do
        echo "$(date '+%Y-%m-%d %H:%M:%S') - $(free -h | grep 'Mem:')" >> "$LOG_FILE"
        sleep 30
    done
}

# Iniciar monitoreo en background
monitor_ram &
MONITOR_PID=$!

# Asegurar que el monitoreo se detenga al salir
cleanup() {
    echo ""
    echo "🛑 Deteniendo monitoreo de RAM..."
    kill $MONITOR_PID 2>/dev/null || true
    echo "📊 Log final de RAM:"
    tail -n 5 "$LOG_FILE"
}
trap cleanup EXIT

echo ""
echo "⏳ Iniciando exportación (esto puede tomar 1-2 horas)..."
echo "   Puedes monitorear RAM en otra terminal con: watch -n 10 free -h"
echo ""

# Ejecutar la exportación
python3 scripts/convert_gemma4_cloud.py

echo ""
echo "=========================================="
echo "✅ Exportación finalizada!"
echo "=========================================="
