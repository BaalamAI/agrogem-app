# https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/main
# https://github.com/google-ai-edge/litert-torch/tree/main/litert_torch/generative/#convert-pytorch-llm-to-a-litert-model
# https://ai.google.dev/edge/litert-lm/android
import os
from huggingface_hub import HfApi, login, create_repo, snapshot_download
import torch
import litert_torch as ltt
from litert_torch.generative import generative_model
from transformers import AutoModelForVision2Seq, AutoProcessor

# --- 1. CONFIGURACIÓN Y LOGIN ---
# RECUERDA: Genera un nuevo token en HF y no lo compartas.
HF_TOKEN = "" 
REPO_ID = "alvarog1318/gemma4-vision-crop-litertlm" 
login(token=HF_TOKEN)

# --- 2. RUTAS LOCALES ---
input_repo = "alvarog1318/gemma4-vision-crop-deseases_16bit"
print("📥 Descargando modelo desde HF Hub...")
input_model = snapshot_download(repo_id=input_repo, token=HF_TOKEN)

output_dir = "./gemma4_litertlm_export"
output_filename = "gemma4_vision_crop_q4.litertlm"

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

# 1. Login y Carga del modelo
login(token="TU_TOKEN_HF")
model_id = "alvarog1318/gemma4-vision-crop-deseases_16bit"

print("📥 Cargando modelo y procesador...")
model = AutoModelForVision2Seq.from_pretrained(
    model_id, 
    device_map="cpu", 
    torch_dtype=torch.float32
)
processor = AutoProcessor.from_pretrained(model_id)

# 2. Configuración del exportador generativo
# litert-torch tiene preconfiguraciones para la familia Gemma
print("📦 Configurando conversión para Gemma 4 Vision...")

# Definimos la configuración de cuantización Int4 (4-bit)
# Esta librería maneja mejor los embeddings automáticamente
quant_config = ltt.quantization.QuantConfig(
    weight_bits=4,
    mode=ltt.quantization.QuantMode.WEIGHT_ONLY
)

# 3. Conversión mediante el módulo 'generative'
# Este paso mapea los pesos de PyTorch a los tensores optimizados de LiteRT
try:
    # Usamos la clase GenerativeModel que es la que mencionan tus links
    # Esta clase se encarga de crear el grafo de inferencia (KV Cache, etc.)
    edge_model = generative_model.export_from_pytorch(
        model,
        # Indicamos que es un modelo de la familia Gemma
        model_type=generative_model.ModelType.GEMMA, 
        quant_config=quant_config
    )

    # 4. Guardar el archivo final
    output_path = "gemma4_crop_vision_q4.tflite" # O .litertlm
    edge_model.save(output_path)
    print(f"✅ ¡Modelo convertido exitosamente! Guardado en: {output_path}")

except Exception as e:
    print(f"❌ Error durante la exportación: {e}")

# 5. Subida a Hugging Face (Opcional)
# Puedes usar HfApi como en los scripts anteriores para subir el .tflite resultante
# --- 5. PUBLICACIÓN EN HUGGING FACE ---
print(f"🚀 Subiendo a Hugging Face: {REPO_ID}...")
api = HfApi()

try:
    create_repo(repo_id=REPO_ID, repo_type="model", exist_ok=True)
    api.upload_folder(
        folder_path=output_dir,
        repo_id=REPO_ID,
        repo_type="model",
        commit_message="Add quantized 4-bit Gemma 4 Vision model in .litertlm format"
    )
    print(f"✨ ¡Proceso completado! Modelo disponible en: https://huggingface.co/{REPO_ID}")
except Exception as e:
    print(f"❌ Error al subir a Hugging Face: {e}")