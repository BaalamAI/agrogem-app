#!/bin/bash
# =============================================================================
# Setup script para RunPod (CPU-only, High RAM)
# =============================================================================
# Este script crea un virtual environment aislado en /workspace e instala
# todas las dependencias necesarias para exportar Gemma 4 a LiteRT-LM.
# Usa /workspace porque el disco raíz (overlay) es muy pequeño (~10 GB).
#
# Uso:
#   chmod +x scripts/runpod_setup.sh
#   ./scripts/runpod_setup.sh
# =============================================================================

set -e

VENV_DIR="/workspace/gemma4-venv"
WORKSPACE_DIR="/workspace"

# Detectar versión de Python disponible (preferir 3.12, 3.11, 3.10)
detect_python() {
    for ver in python3.12 python3.11 python3.10 python3; do
        if command -v "$ver" &>/dev/null; then
            echo "$ver"
            return
        fi
    done
    echo "python3"
}
PYTHON_BIN=$(detect_python)
PYTHON_VER=$("$PYTHON_BIN" -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")

echo "=========================================="
echo "🔧 Setup para RunPod - Gemma 4 Export"
echo "=========================================="
echo ""

# Verificar RAM
TOTAL_RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
TOTAL_RAM_GB=$((TOTAL_RAM_KB / 1024 / 1024))
echo "💾 RAM detectada: ${TOTAL_RAM_GB} GB"

# Verificar disco
echo ""
echo "📦 Verificando espacio en disco..."
echo "  Raíz (/) — sistema:"
df -h / | tail -1
echo "  /workspace — trabajo (usa este):"
df -h /workspace | tail -1 || echo "    ⚠️ /workspace no montado"

# Fijar pip cache en /workspace (evita warning de permisos en RunPod)
export PIP_CACHE_DIR="/workspace/.cache/pip"
mkdir -p "${PIP_CACHE_DIR}"

# Advertir si hay torch del sistema en conflicto
SYS_TORCH=$(python3 -c "import torch; print(torch.__version__)" 2>/dev/null || echo "")
if [ -n "$SYS_TORCH" ]; then
    echo ""
    echo "⚠️  Se detectó torch ${SYS_TORCH} preinstalado en el sistema."
    echo "   Crearemos un venv aislado en ${VENV_DIR} para no interferir."
fi

# Instalar python3-venv y python3-pip para la versión detectada
echo ""
echo "📦 Instalando python3-venv para Python ${PYTHON_VER}..."
apt-get update -qq
apt-get install -y -qq "python${PYTHON_VER}-venv" "python${PYTHON_VER}-pip" 2>/dev/null || \
    apt-get install -y -qq python3-venv python3-pip

# Crear venv en /workspace
echo ""
echo "📦 Creando virtual environment en ${VENV_DIR} (${PYTHON_BIN})..."
# Limpiar venv roto si existía de un intento anterior
[ -d "${VENV_DIR}" ] && rm -rf "${VENV_DIR}"
"${PYTHON_BIN}" -m venv "${VENV_DIR}"

# Activar venv
source "${VENV_DIR}/bin/activate"

# Actualizar pip dentro del venv
echo ""
echo "📦 Actualizando pip dentro del venv..."
pip install -q --upgrade pip

# Instalar dependencias
# litert-torch==0.9.0 requiere torch>=2.4.0,<2.12.0, pero torch<2.5.0 falla con
# infer_schema() si hay parámetros con default de tipo str (bug conocido en 2.4.x).
# Usar el índice oficial de PyTorch para acceder a 2.5.x+ (no están en PyPI estándar).
# El export es 100% CPU-bound, así que la variante CPU-only es suficiente y más liviana.
echo ""
echo "📦 Instalando torch ≥ 2.5.1 desde índice oficial de PyTorch (CPU)..."
pip install \
    "torch>=2.5.1,<2.12.0" \
    "torchvision" \
    --index-url https://download.pytorch.org/whl/cpu

echo ""
echo "📦 Instalando resto de dependencias..."
pip install \
    "litert-torch==0.9.0" \
    "transformers>=4.51" \
    "huggingface_hub" \
    "pillow"

echo ""
echo "📦 Verificando instalaciones..."
"${VENV_DIR}/bin/python" -c "import torch; print(f'  torch: {torch.__version__}')"
"${VENV_DIR}/bin/python" -c "import litert_torch; print(f'  litert_torch: {litert_torch.__version__}')"
"${VENV_DIR}/bin/python" -c "import transformers; print(f'  transformers: {transformers.__version__}')"

echo ""
echo "=========================================="
echo "✅ Setup completo!"
echo "=========================================="
echo ""
echo "Para activar el entorno en futuras sesiones, ejecuta:"
echo "  source ${VENV_DIR}/bin/activate"
echo ""
echo "Próximos pasos:"
echo "  1. Activar venv:   source ${VENV_DIR}/bin/activate"
echo "  2. Login HF:       huggingface-cli login"
echo "  3. Exportar:       python3 scripts/convert_gemma4_cloud.py"
echo ""
echo "⚠️  IMPORTANTE: La exportación usará /workspace como directorio de salida"
echo "   para evitar llenar el disco raíz (10 GB)."
echo ""
