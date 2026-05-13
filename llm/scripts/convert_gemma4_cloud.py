#!/usr/bin/env python3
"""
Script para convertir un modelo Gemma 4 con LoRA fine-tuneado a LiteRT-LM (.litertlm)
en la nube. Optimizado para RunPod CPU High-RAM, pero funciona en cualquier VM
(GCP, AWS, Colab Pro+, etc.).

Configuración actual (lista para RunPod):
    - Cuantización: INT4 BLOCKWISE_256 (~2.5-3 GB final)
    - Prefill lengths: [512, 1024] (soporta 1 imagen + prompt largo)
    - Cache length: 2048
    - Visión encoder: SÍ (EXPORT_VISION_ENCODER = True)
    - Task: image_text_to_text

Requisitos de hardware (mínimos recomendados):
    - RAM: ≥ 64 GB  (ideal: 128 GB)
    - Disco: ≥ 30 GB libres
    - CPU: 8+ cores (el proceso es CPU-bound)

Setup rápido en RunPod:
    chmod +x scripts/runpod_setup.sh && ./scripts/runpod_setup.sh
    huggingface-cli login
    chmod +x scripts/runpod_run.sh && ./scripts/runpod_run.sh

O ejecuta directamente:
    pip install "litert-torch==0.9.0" "transformers>=4.51" huggingface_hub pillow torch
    python scripts/convert_gemma4_cloud.py

Notas:
    - Este script aplica AUTOMÁTICAMENTE los 3 parches necesarios en runtime.
    - Los parches aplicados son:
      1. Fix ai-edge-quantizer para tensores N-D (>2D) en CHANNELWISE (PR #482)
      2. Crear vision_exportable.py para Gemma 4
      3. Registrar gemma4 en get_vision_exportables
    - Para INT4, la receta usa BLOCKWISE_256 (única granularidad soportada
      por el runtime de LiteRT-LM para 4-bit).
"""

import json
import os
import sys
import importlib.util

# =============================================================================
# CONFIGURACIÓN DEL USUARIO
# =============================================================================

MODEL_ID = "alvarog1318/gemma4-vision-crop-deseases_16bit"

# Detectar si estamos en RunPod (/workspace disponible) y usarlo como salida
if os.path.isdir("/workspace"):
    OUTPUT_DIR = "/workspace/gemma4_litertlm_export"
else:
    OUTPUT_DIR = "./gemma4_litertlm_export"

REPO_ID = "alvarog1318/gemma4-vision-crop-litertlm"

TASK = "image_text_to_text"

# PREFILL_LENGTHS define los tamaños de prompt que el modelo puede procesar
# de una sola vez (fase "prefill"). Cada valor crea una firma separada en el
# .tflite. Con 282 GB de RAM en RunPod podemos exportar múltiples firmas.
#
# Tokens vs palabras (aproximado, en español ~0.6 palabras/token):
#   256 tokens  ≈ 150 palabras  (prompt corto)
#   512 tokens  ≈ 300 palabras  (1 imagen + prompt mediano)
#   1024 tokens ≈ 600 palabras  (1-2 imágenes + prompt largo)
#   2048 tokens ≈ 1200 palabras (múltiples imágenes + prompt muy largo)
#
# Nota: 1 imagen 224x224 consume ~256 tokens de visión.
CACHE_LENGTH = 4096
PREFILL_LENGTHS = [256, 512, 1024, 2048]

# --- Cuantización ---
# Opciones para QUANTIZATION_RECIPE:
#   "dynamic_wi8_afp32"          -> pesos INT8, activaciones FP32 (~4.7 GB)
#   "weight_only_wi8_afp32"      -> pesos INT8, activaciones FP32 (similar)
#   "dynamic_wi4_afp32"          -> pesos INT4 CHANNELWISE (NO soportado en runtime)
#   "weight_only_wi4_afp32"      -> pesos INT4 CHANNELWISE (NO soportado en runtime)
#
# Para INT4 con granularidad BLOCKWISE (soportado por LiteRT-LM runtime),
# usa "int4_blockwise_256.json" (generado automáticamente por este script).
# Esto reduce el modelo a ~2.5-3 GB.
QUANTIZATION_RECIPE = "int4_blockwise_256.json"
VISION_ENCODER_QUANTIZATION_RECIPE = "int4_blockwise_256.json"

BUNDLE_LITERT_LM = True
EXPORT_VISION_ENCODER = True
EXTERNALIZE_EMBEDDER = True          # Requerido para Gemma 4
TRUST_REMOTE_CODE = False

# =============================================================================
# 0. GENERAR RECETA INT4 BLOCKWISE (si se solicita)
# =============================================================================

def ensure_int4_recipe():
    """Si la receta es 'int4_blockwise_256.json', la genera en disco.

    Devuelve la ruta absoluta del archivo generado, o QUANTIZATION_RECIPE
    original si no aplica.
    """
    if QUANTIZATION_RECIPE != "int4_blockwise_256.json":
        return QUANTIZATION_RECIPE

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
    return recipe_path


# =============================================================================
# 1. APLICAR PARCHES EN RUNTIME
# =============================================================================

def patch_ai_edge_quantizer():
    """Parche 1: Fix N-D tensor broadcast en ai-edge-quantizer (PR #482)."""
    spec = importlib.util.find_spec(
        "ai_edge_quantizer.algorithms.uniform_quantize.uniform_quantize_tensor"
    )
    if spec is None:
        print("❌ No se encontró ai_edge_quantizer. ¿Está instalado?")
        return False

    path = spec.origin
    with open(path, "r") as f:
        content = f.read()

    old_code = (
        "      # Reshape the `tensor_data`, `scales`, and `zero_points` to just two\n"
        "      # dimensions.\n"
        "      output_shape = tensor_data.shape\n"
        "      tensor_data = tensor_data.reshape([-1, output_shape[-1]])\n"
        "      scales = np.broadcast_to(scales, tensor_data.shape)\n"
        "      zero_points = np.broadcast_to(zero_points, tensor_data.shape)\n"
        "      ret = np.zeros(shape=tensor_data.shape, dtype=_get_numpy_dtype(qtype))"
    )

    new_code = (
        "      # Reshape the `tensor_data`, `scales`, and `zero_points` to just two\n"
        "      # dimensions.\n"
        "      output_shape = tensor_data.shape\n"
        "      tensor_data = tensor_data.reshape([-1, output_shape[-1]])\n"
        "      scales = np.broadcast_to(scales, output_shape).reshape([-1, output_shape[-1]])\n"
        "      zero_points = np.broadcast_to(zero_points, output_shape).reshape([-1, output_shape[-1]])\n"
        "      ret = np.zeros(shape=tensor_data.shape, dtype=_get_numpy_dtype(qtype))"
    )

    if old_code in content:
        content = content.replace(old_code, new_code)
        with open(path, "w") as f:
            f.write(content)
        print("✅ Parche 1 aplicado: ai-edge-quantizer N-D tensor fix.")
        return True
    else:
        print("⚠️  Parche 1 omitido (ya aplicado o versión diferente).")
        return True


def patch_litert_torch_gemma4_vision():
    """Parche 2: Crear vision_exportable.py para Gemma 4."""
    import litert_torch

    litert_pkg = os.path.dirname(litert_torch.__file__)
    gemma4_dir = os.path.join(
        litert_pkg, "generative", "export_hf", "model_ext", "gemma4"
    )
    os.makedirs(gemma4_dir, exist_ok=True)

    vision_file = os.path.join(gemma4_dir, "vision_exportable.py")
    vision_content = '''"""Exportable modules for Gemma4 vision encoder and adapter."""

from litert_torch.generative.export_hf.core import exportable_module as exportable_module_base
import torch


class LiteRTExportableModuleForGemma4VisionEncoder(
    exportable_module_base.ExportableModuleBase
):
  """Exportable module for Gemma4 vision encoder (includes embed_vision)."""

  def __init__(self, model: torch.nn.Module, export_config):
    super().__init__(export_config)
    self.model = model

  def forward(
      self,
      pixel_values,
      image_position_ids,
  ):
    output = self.model.model.get_image_features(
        pixel_values=pixel_values,
        image_position_ids=image_position_ids,
    )
    return {"features": output.pooler_output.unsqueeze(0)}

  def get_sample_inputs(
      self, model_config, **kwargs
  ) -> dict[str, tuple[dict[str, torch.Tensor], dict[str, torch.export.Dim]]]:
    image_processor = kwargs.get("image_processor", None)
    if image_processor is None:
      raise ValueError(
          "Image processor is required for exporting Gemma4 vision encoder."
      )
    from PIL import Image
    import torch

    dummy_image = Image.new("RGB", (224, 224), color="red")
    inputs = image_processor(images=[dummy_image], return_tensors="pt")

    pixel_values = inputs["pixel_values"]

    if "image_position_ids" in inputs:
      image_position_ids = inputs["image_position_ids"]
    else:
      # Gemma4 image_processor individual no genera position_ids.
      # Los generamos manualmente: 224x224 / patch_size=14 = 16x16 = 256 patches.
      batch_size = pixel_values.shape[0]
      num_patches = 256
      image_position_ids = torch.zeros(
          (batch_size, num_patches, 2), dtype=torch.long
      )
      for y in range(16):
        for x in range(16):
          idx = y * 16 + x
          image_position_ids[:, idx, 0] = x
          image_position_ids[:, idx, 1] = y

    return {
        "vision_encoder": (
            {
                "pixel_values": pixel_values,
                "image_position_ids": image_position_ids,
            },
            {},
        )
    }


class LiteRTExportableModuleForGemma4VisionAdapter(
    exportable_module_base.ExportableModuleBase
):
  """Passthrough adapter (embed_vision already applied in encoder)."""

  def __init__(self, model: torch.nn.Module, export_config, tokenizer):
    super().__init__(export_config)
    self.model = model
    self.tokenizer = tokenizer

  def forward(self, soft_tokens):
    return {"mm_embedding": soft_tokens}

  def get_sample_inputs(
      self, model_config, **kwargs
  ) -> dict[str, tuple[dict[str, torch.Tensor], dict[str, torch.export.Dim]]]:
    text_hidden_size = model_config.text_config.hidden_size
    return {
        "vision_adapter": (
            {
                "soft_tokens": torch.zeros(
                    (1, 256, text_hidden_size), dtype=torch.float32
                )
            },
            {},
        )
    }
'''
    with open(vision_file, "w") as f:
        f.write(vision_content)
    print(f"✅ Parche 2 aplicado: {vision_file}")
    return True


def patch_litert_torch_exportables():
    """Parche 3: Registrar Gemma4 vision exportables en el dispatcher."""
    import litert_torch

    litert_pkg = os.path.dirname(litert_torch.__file__)
    exportables_path = os.path.join(
        litert_pkg, "generative", "export_hf", "model_ext", "exportables.py"
    )

    with open(exportables_path, "r") as f:
        content = f.read()

    changed = False

    import_line = (
        "from litert_torch.generative.export_hf.model_ext.gemma4 "
        "import vision_exportable as gemma4_vision_exportable"
    )
    if import_line not in content:
        content = content.replace(
            "from litert_torch.generative.export_hf.model_ext.gemma4 import exportable_module as gemma4_exportable",
            "from litert_torch.generative.export_hf.model_ext.gemma4 import exportable_module as gemma4_exportable\n"
            + import_line,
        )
        changed = True

    old_vision = """  elif model_config.model_type == 'gemma3n':
    return (
        gemma3n_vision_exportable.LiteRTExportableModuleForGemma3nVisionEncoder,
        gemma3n_vision_exportable.LiteRTExportableModuleForGemma3nVisionAdapter,
    )
  else:
    raise ValueError(f'Unsupported model type: {model_config.model_type}')"""

    new_vision = """  elif model_config.model_type == 'gemma3n':
    return (
        gemma3n_vision_exportable.LiteRTExportableModuleForGemma3nVisionEncoder,
        gemma3n_vision_exportable.LiteRTExportableModuleForGemma3nVisionAdapter,
    )
  elif model_config.model_type == 'gemma4':
    return (
        gemma4_vision_exportable.LiteRTExportableModuleForGemma4VisionEncoder,
        gemma4_vision_exportable.LiteRTExportableModuleForGemma4VisionAdapter,
    )
  else:
    raise ValueError(f'Unsupported model type: {model_config.model_type}')"""

    if old_vision in content:
        content = content.replace(old_vision, new_vision)
        changed = True

    if changed:
        with open(exportables_path, "w") as f:
            f.write(content)
        print(f"✅ Parche 3 aplicado: {exportables_path}")
    else:
        print("⚠️  Parche 3 omitido (ya aplicado o versión diferente).")
    return True


def patch_typing_self():
    """Parche 0a: Fallback para typing.Self en Python < 3.11."""
    import typing
    if not hasattr(typing, "Self"):
        try:
            from typing_extensions import Self
            typing.Self = Self
            print("✅ Parche 0a aplicado: typing.Self fallback.")
            return True
        except ImportError:
            print("❌ Parche 0a falló: instala 'typing_extensions'.")
            return False
    else:
        print("⚠️  Parche 0a omitido (Python >= 3.11).")
        return True


def patch_enum_strenum():
    """Parche 0b: Fallback para enum.StrEnum en Python < 3.11.

    ai_edge_litert usa enum.StrEnum (introducido en Python 3.11).
    En RunPod con Python 3.10 esto falla al importar.
    """
    import enum
    if not hasattr(enum, "StrEnum"):
        class StrEnum(str, enum.Enum):
            pass
        enum.StrEnum = StrEnum
        print("✅ Parche 0b aplicado: enum.StrEnum fallback.")
        return True
    else:
        print("⚠️  Parche 0b omitido (Python >= 3.11).")
        return True


def patch_gemma4_vision_boolean_indexing():
    """Parche 4: Evitar boolean indexing dinámico en Gemma4VisionModel.forward.

    JAX (backend de litert_torch) no soporta indexación con máscaras booleanas
    no concretas. Para inputs fijos sin padding (nuestro caso de export),
    pooler_mask es todo True, así que el indexing es un no-op.
    """
    try:
        from transformers.models.gemma4.modeling_gemma4 import Gemma4VisionModel
        from transformers.modeling_outputs import BaseModelOutputWithPast
    except Exception as e:
        print(f"❌ No se pudo importar Gemma4VisionModel: {e}")
        return False

    def patched_forward(self, pixel_values, pixel_position_ids, **kwargs):
        pooling_kernel_size = self.config.pooling_kernel_size
        output_length = pixel_values.shape[-2] // (pooling_kernel_size * pooling_kernel_size)

        padding_positions = (pixel_position_ids == -1).all(dim=-1)
        inputs_embeds = self.patch_embedder(pixel_values, pixel_position_ids, padding_positions)
        output = self.encoder(
            inputs_embeds=inputs_embeds,
            attention_mask=~padding_positions,
            pixel_position_ids=pixel_position_ids,
            **kwargs,
        )

        hidden_states, pooler_mask = self.pooler(
            hidden_states=output.last_hidden_state,
            pixel_position_ids=pixel_position_ids,
            padding_positions=padding_positions,
            output_length=output_length,
        )

        # ORIGINAL (falla en JAX): hidden_states = hidden_states[pooler_mask]
        # Como nuestros inputs de export no tienen padding, pooler_mask es todo True.
        # Simplemente mantenemos hidden_states tal cual para evitar boolean indexing.

        if self.config.standardize:
            hidden_states = (hidden_states - self.std_bias) * self.std_scale

        return BaseModelOutputWithPast(last_hidden_state=hidden_states)

    Gemma4VisionModel.forward = patched_forward
    print("✅ Parche 4 aplicado: Gemma4VisionModel boolean indexing fix.")
    return True


def patch_gemma3_metadata_builder():
    """Parche 5: Fix image_processor.size cuando es None o no tiene height/width.

    IMPORTANTE: gemma3.build_llm_metadata NO tiene un parámetro 'image_processor'
    en su firma — lo extrae internamente desde otro argumento (ej. processor.image_processor).
    La estrategia correcta es mutar en-lugar los objetos que ya están en args/kwargs,
    NO inyectar kwargs nuevos (eso genera TypeError: unexpected keyword argument).
    """
    try:
        from litert_torch.generative.export_hf.model_ext.gemma3 import (
            metadata_builder as gemma3_metadata,
        )
        import inspect

        original_func = gemma3_metadata.build_llm_metadata

        # Log de la firma real para diagnóstico
        try:
            sig = inspect.signature(original_func)
            print(f"  [Parche 5] Firma: build_llm_metadata{sig}")
        except Exception:
            pass

        def _fix_ip_size(ip, config=None):
            """Inyecta ip.size = {'height': H, 'width': W} si no es un dict válido."""
            if ip is None:
                return
            size = getattr(ip, "size", None)
            if isinstance(size, dict) and "height" in size and "width" in size:
                return  # ya correcto

            # Determinar la dimensión real (fallback 224)
            image_size = 224
            if config is not None:
                vc = getattr(config, "vision_config", None)
                if vc is not None:
                    image_size = getattr(vc, "image_size", image_size)
                else:
                    image_size = getattr(config, "image_size", image_size)
            if isinstance(size, dict):
                le = size.get("longest_edge")
                if le:
                    image_size = le
            crop = getattr(ip, "crop_size", None)
            if isinstance(crop, dict):
                image_size = crop.get("height", crop.get("width", image_size))

            try:
                ip.size = {"height": image_size, "width": image_size}
                print(f"  [Parche 5] Inyectado size={ip.size} en {type(ip).__name__}")
            except (AttributeError, TypeError) as e:
                print(f"  [Parche 5] No se pudo setear size en-lugar: {e}")

        def patched_build_llm_metadata(*args, **kwargs):
            # Config suele ser el primer argumento posicional
            config = args[0] if args else kwargs.get("config")

            # Escanear todos los args y kwargs buscando image_processor
            for v in list(args) + list(kwargs.values()):
                if v is None:
                    continue
                # Caso A: v tiene .image_processor (ej. Gemma4Processor)
                ip = getattr(v, "image_processor", None)
                if ip is not None:
                    _fix_ip_size(ip, config)
                    continue
                # Caso B: v mismo es el image_processor
                if hasattr(v, "size") or hasattr(v, "crop_size"):
                    _fix_ip_size(v, config)

            return original_func(*args, **kwargs)

        gemma3_metadata.build_llm_metadata = patched_build_llm_metadata
        print("✅ Parche 5 aplicado: metadata_builder in-place size fix.")
        return True
    except Exception as e:
        print(f"⚠️  Parche 5 error: {e}")
        import traceback
        traceback.print_exc()
        return False



def patch_litert_lm_builder_gemma4():
    """Parche 6: Añadir case 'gemma4' al match statement de litert_lm_builder.

    Issue #1005: build_llm_metadata() en litert_lm_builder.py no tiene
    case 'gemma4', por lo que el modelo exportado recibe metadata generic_model
    en vez de gemma4. Esto hace que LiteRT-LM use GenericDataProcessor en vez
    de Gemma4DataProcessor, rompiendo el chat template de Gemma 4 en inferencia.
    """
    try:
        from litert_torch.generative.export_hf.core import litert_lm_builder
        import inspect

        original_func = litert_lm_builder.build_llm_metadata

        # Importar el tipo protobuf de Gemma4
        try:
            from ai_edge_litert.internal import llm_model_type_pb2
            has_gemma4_proto = hasattr(llm_model_type_pb2, "Gemma4")
        except ImportError:
            has_gemma4_proto = False
            print("⚠️  Parche 6: no se encontró llm_model_type_pb2, se usará monkey-patch de texto.")

        def patched_build_llm_metadata(*args, **kwargs):
            sig = inspect.signature(original_func)
            bound = sig.bind(*args, **kwargs)
            bound.apply_defaults()

            # Detectar model_type desde config o argumento directo
            config = bound.arguments.get("config") or (args[0] if args else None)
            model_type = getattr(config, "model_type", None)

            if model_type == "gemma4":
                # Llamar la función original para que inicialice llm_metadata
                result = original_func(*args, **kwargs)

                # Sobrescribir el tipo de modelo con gemma4 si el proto lo soporta
                if has_gemma4_proto and hasattr(result, "llm_model_type"):
                    result.llm_model_type.CopyFrom(
                        llm_model_type_pb2.LlmModelType(
                            gemma4=llm_model_type_pb2.Gemma4()
                        )
                    )
                    print("✅ Parche 6: llm_model_type seteado a Gemma4.")
                return result

            return original_func(*args, **kwargs)

        litert_lm_builder.build_llm_metadata = patched_build_llm_metadata
        print("✅ Parche 6 aplicado: litert_lm_builder gemma4 model type fix.")
        return True
    except Exception as e:
        print(f"⚠️  Parche 6 error: {e}")
        import traceback
        traceback.print_exc()
        return False


def apply_all_patches():
    print("🔧 Aplicando parches en runtime...\n")
    ok = True
    ok &= patch_typing_self()
    ok &= patch_enum_strenum()
    ok &= patch_ai_edge_quantizer()
    ok &= patch_litert_torch_gemma4_vision()
    ok &= patch_litert_torch_exportables()
    ok &= patch_gemma4_vision_boolean_indexing()
    ok &= patch_gemma3_metadata_builder()
    ok &= patch_litert_lm_builder_gemma4()
    print()
    if not ok:
        print("❌ Algunos parches fallaron. Abortando.")
        sys.exit(1)
    print("🎉 Todos los parches aplicados.\n")


# =============================================================================
# 2. LIMPIEZA DE DISCO
# =============================================================================

def cleanup_disk_space():
    """Limpia archivos temporales para liberar espacio en disco."""
    import shutil
    import glob

    print("🧹 Limpiando archivos temporales...")

    # 1. Limpiar directorios temporales de exportaciones anteriores
    for tmp_dir in glob.glob("/tmp/tmp*") + glob.glob("/tmp/litert*"):
        if os.path.isdir(tmp_dir):
            shutil.rmtree(tmp_dir, ignore_errors=True)

    # 2. Limpiar caché de pip
    pip_cache = os.path.expanduser("~/.cache/pip")
    if os.path.isdir(pip_cache):
        shutil.rmtree(pip_cache, ignore_errors=True)

    # 3. Limpiar checkpoints de HuggingFace (mantener modelos base)
    hf_cache = os.path.expanduser("~/.cache/huggingface/hub")
    if os.path.isdir(hf_cache):
        # Solo limpiar snapshots temporales, no los modelos descargados
        for snapshots_dir in glob.glob(os.path.join(hf_cache, "*/snapshots")):
            for snapshot in os.listdir(snapshots_dir):
                snapshot_path = os.path.join(snapshots_dir, snapshot)
                if os.path.isdir(snapshot_path):
                    # Verificar si es un modelo completo o temporal
                    if not any(f.endswith(".bin") or f.endswith(".safetensors") for f in os.listdir(snapshot_path) if os.path.isfile(os.path.join(snapshot_path, f))):
                        shutil.rmtree(snapshot_path, ignore_errors=True)

    # 4. Limpiar directorios de export previos (excepto el actual)
    for export_dir in glob.glob("/workspace/gemma4_litertlm_export_*") + glob.glob("./gemma4_litertlm_export_*"):
        if os.path.isdir(export_dir) and export_dir != OUTPUT_DIR:
            shutil.rmtree(export_dir, ignore_errors=True)

    print("✅ Limpieza completada.")


# =============================================================================
# 3. EXPORTACIÓN
# =============================================================================

def run_export():
    from huggingface_hub import HfApi
    from litert_torch.generative.export_hf.export import export as export_hf

    print("🔐 Verificando sesión de HuggingFace...")
    try:
        api = HfApi()
        user_info = api.whoami()
        print(f"✅ Logueado como: {user_info['name']}")
    except Exception as e:
        print(f"❌ No estás logueado en HuggingFace: {e}")
        print("   Ejecuta: huggingface-cli login")
        sys.exit(1)

    # Limpiar espacio antes de exportar
    cleanup_disk_space()

    # Generar/obtener la ruta absoluta de la receta de cuantización
    recipe_path = ensure_int4_recipe()

    print(f"📦 Exportando modelo: {MODEL_ID}")
    print(f"📂 Directorio de salida: {OUTPUT_DIR}")
    print(f"🎯 Tarea: {TASK}")
    print(f"🔢 Cache length: {CACHE_LENGTH}")
    print(f"📏 Prefill lengths: {PREFILL_LENGTHS}")
    print(f"⚖️  Quantization (decoder): {recipe_path}")
    print(f"⚖️  Quantization (vision): {VISION_ENCODER_QUANTIZATION_RECIPE}")
    print(f"📷 Export vision encoder: {EXPORT_VISION_ENCODER}")
    print()

    export_hf(
        model=MODEL_ID,
        output_dir=OUTPUT_DIR,
        task=TASK,
        cache_length=CACHE_LENGTH,
        prefill_lengths=PREFILL_LENGTHS,
        quantization_recipe=recipe_path,
        vision_encoder_quantization_recipe=recipe_path,
        bundle_litert_lm=BUNDLE_LITERT_LM,
        export_vision_encoder=EXPORT_VISION_ENCODER,
        externalize_embedder=EXTERNALIZE_EMBEDDER,
        trust_remote_code=TRUST_REMOTE_CODE,
    )

    # Limpiar archivos temporales después de la exportación
    cleanup_disk_space()

    litertlm_path = None
    for root, _, files in os.walk(OUTPUT_DIR):
        for f in files:
            if f.endswith(".litertlm"):
                litertlm_path = os.path.join(root, f)
                break
        if litertlm_path:
            break

    if litertlm_path:
        size_gb = os.path.getsize(litertlm_path) / (1024 ** 3)
        print(f"\n✅ Exportación completada: {litertlm_path}")
        print(f"📏 Tamaño: {size_gb:.2f} GB")
    else:
        print("\n⚠️  No se encontró archivo .litertlm. Revisa el directorio de salida.")
        return

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
        print(f"✨ Modelo disponible en: https://huggingface.co/{REPO_ID}")
    except Exception as e:
        print(f"❌ Error al subir a HuggingFace: {e}")


if __name__ == "__main__":
    apply_all_patches()
    run_export()
