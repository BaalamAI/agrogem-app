"""
Script para convertir un modelo Gemma 4 con LoRA (fine-tuneado)
al formato LiteRT-LM (.litertlm) para despliegue on-device (Android/iOS).

Requisitos:
    pip install litert-torch transformers huggingface_hub

Notas:
    - Gemma 4 requiere 'externalize_embedder=True'.
    - Este script usa la API oficial 'litert_torch.generative.export_hf.export'.
    - Se aplicó un parche local en ai-edge-quantizer para soportar tensores
      N-D (>2D) en cuantización CHANNELWISE (fix del PR #482).
    - Se añadió soporte experimental de visión para Gemma 4 en litert_torch
      mediante gemma4/vision_exportable.py.
    - Para INT4 con granularidad BLOCKWISE (soportado por LiteRT-LM runtime),
      usa "int4_blockwise_256.json" (generado automáticamente por este script).
      Esto reduce el modelo a ~2.5-3 GB.

Uso:
    # Asegurate de estar logueado con hf auth login
    python scripts/transform_to_mobile.py
"""

import json
import os

from huggingface_hub import HfApi
from litert_torch.generative.export_hf.export import export as export_hf


# =============================================================================
# CONFIGURACIÓN
# =============================================================================

# Modelo fine-tuneado en HF (debe ser público o tienes acceso con el token)
MODEL_ID = "alvarog1318/gemma4-vision-crop-deseases_16bit"

# Directorio local de salida
OUTPUT_DIR = "./gemma4_litertlm_export"

# Repo de HF donde se subirá el .litertlm resultante
REPO_ID = "alvarog1318/gemma4-vision-crop-litertlm-4bit"

# Parámetros de exportación para Gemma 4
TASK = "image_text_to_text"          # o "text_generation" si solo quieres texto
CACHE_LENGTH = 1024                  # Reducido para ahorrar memoria en exportación

# PREFILL_LENGTHS define los tamaños de prompt que el modelo puede procesar
# de una sola vez (fase "prefill"). Cada valor crea una firma separada en el
# .tflite. Más firmas = más flexibilidad pero más RAM en exportación.
#
# Tokens vs palabras (aproximado):
#   - Inglés: ~0.75 palabras/token  → 256 tokens ≈ 190 palabras
#   - Español: ~0.6 palabras/token  → 256 tokens ≈ 150 palabras
#   - Con imágenes: 1 imagen 224x224 ≈ 256 tokens de visión
#
# Por eso, para image-text-to-text práctico se recomienda al menos [512].
PREFILL_LENGTHS = [512]              # Solo 1 firma para reducir memoria

# --- Cuantización ---
# Opciones para QUANTIZATION_RECIPE:
#   "dynamic_wi8_afp32"          -> pesos INT8, activaciones FP32 (~4.7 GB)
#   "weight_only_wi8_afp32"      -> pesos INT8, activaciones FP32 (similar)
#   "dynamic_wi4_afp32"          -> pesos INT4 CHANNELWISE (NO soportado en runtime)
#
# Para INT4 con granularidad BLOCKWISE (soportado por LiteRT-LM runtime),
# usa "int4_blockwise_256.json" (generado automáticamente por este script).
# Esto reduce el modelo a ~2.5-3 GB.
QUANTIZATION_RECIPE = "dynamic_wi8_afp32"   # Cambia a "int4_blockwise_256.json" para INT4
VISION_ENCODER_QUANTIZATION_RECIPE = "dynamic_wi8_afp32"
BUNDLE_LITERT_LM = True
EXPORT_VISION_ENCODER = True        # Deshabilitado por ahora para ahorrar memoria
EXTERNALIZE_EMBEDDER = True          # Requerido para Gemma 4


# =============================================================================
# 0. GENERAR RECETA INT4 BLOCKWISE (si se solicita)
# =============================================================================

def ensure_int4_recipe():
    """Si la receta es 'int4_blockwise_256.json', la genera en disco."""
    if QUANTIZATION_RECIPE != "int4_blockwise_256.json":
        return

    recipe_path = os.path.join(OUTPUT_DIR, "int4_blockwise_256.json")
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    recipe = [
        {
            "regex": ".*",
            "operation": "*",
            "algorithm_key": "min_max_uniform_quantize",
            "op_config": {
                "weight_tensor_config": {
                    "num_bits": 4,
                    "symmetric": True,
                    "granularity": "BLOCKWISE_256",
                    "dtype": "INT"
                },
                "compute_precision": "INTEGER",
                "explicit_dequantize": False,
                "skip_checks": False,
                "min_weight_elements": 0
            }
        }
    ]

    with open(recipe_path, "w") as f:
        json.dump(recipe, f, indent=2)
    print(f"✅ Receta INT4 BLOCKWISE_256 generada: {recipe_path}")


# =============================================================================
# LOGIN Y EXPORTACIÓN
# =============================================================================

def main():
    print("🔐 Verificando sesión de HuggingFace...")
    try:
        api = HfApi()
        user_info = api.whoami()
        print(f"✅ Logueado como: {user_info['name']}")
    except Exception as e:
        print(f"⚠️  No se detectó sesión activa de HuggingFace: {e}")
        print("   Por favor ejecuta: hf auth login")
        return

    print(f"📦 Exportando modelo: {MODEL_ID}")
    print(f"📂 Directorio de salida: {OUTPUT_DIR}")
    print(f"🎯 Tarea: {TASK}")
    print(f"🔢 Longitud de cache: {CACHE_LENGTH}")
    print(f"📏 Prefill lengths: {PREFILL_LENGTHS}")
    print(f"⚖️  Receta de cuantización (decoder): {QUANTIZATION_RECIPE}")
    print(f"⚖️  Receta de cuantización (vision): {VISION_ENCODER_QUANTIZATION_RECIPE}")
    print()

    export_hf(
        model=MODEL_ID,
        output_dir=OUTPUT_DIR,
        task=TASK,
        cache_length=CACHE_LENGTH,
        prefill_lengths=PREFILL_LENGTHS,
        quantization_recipe=QUANTIZATION_RECIPE,
        vision_encoder_quantization_recipe=VISION_ENCODER_QUANTIZATION_RECIPE,
        bundle_litert_lm=BUNDLE_LITERT_LM,
        export_vision_encoder=EXPORT_VISION_ENCODER,
        externalize_embedder=EXTERNALIZE_EMBEDDER,
        trust_remote_code=False,
    )

    # Buscar el archivo .litertlm generado
    litertlm_path = None
    for root, _, files in os.walk(OUTPUT_DIR):
        for f in files:
            if f.endswith(".litertlm"):
                litertlm_path = os.path.join(root, f)
                break
        if litertlm_path:
            break

    if litertlm_path:
        print(f"\n✅ Exportación completada: {litertlm_path}")
        print(f"📏 Tamaño: {os.path.getsize(litertlm_path) / (1024**3):.2f} GB")
    else:
        print("\n⚠️  No se encontró archivo .litertlm. Revisa el directorio de salida.")
        return

    # =============================================================================
    # SUBIDA A HUGGINGFACE (opcional)
    # =============================================================================
    print(f"\n🚀 Subiendo a HuggingFace: {REPO_ID} ...")
    try:
        api.create_repo(repo_id=REPO_ID, repo_type="model", exist_ok=True)
        api.upload_file(
            path_or_fileobj=litertlm_path,
            path_in_repo=os.path.basename(litertlm_path),
            repo_id=REPO_ID,
            repo_type="model",
            commit_message="Add quantized Gemma 4 vision model in .litertlm format",
        )
        print(f"✨ ¡Modelo disponible en: https://huggingface.co/{REPO_ID}")
    except Exception as e:
        print(f"❌ Error al subir a HuggingFace: {e}")


if __name__ == "__main__":
    ensure_int4_recipe()
    main()
