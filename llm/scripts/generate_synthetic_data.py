import json
import os
import time
import random
import PIL.Image
import concurrent.futures
from google import genai
from pydantic import BaseModel, Field

# --- CONFIGURACIÓN ---
API_KEY = ""
METADATA_FILE = "dataset_metadata.json"  
BATCH_SIZE = 50       # Tamaño del lote a procesar antes de guardar en disco
MAX_WORKERS = 4       # Número de hilos simultáneos (AJUSTAR según tu tier de API)

# Inicializar cliente de la nueva API
client = genai.Client(api_key=API_KEY)

# --- ESQUEMA ESTRUCTURADO CON PYDANTIC ---
class CropAnalysis(BaseModel):
    nx: str = Field(description="Nombre común y preciso de la enfermedad o estado.")
    dx: str = Field(description="Diagnóstico objetivo y preciso de la enfermedad o estado.")
    est: str = Field(description="Etapa de avance, evaluar basándote solamente en la imagen. Ej: 'Inicial', 'Intermedia' o 'Avanzada'.")
    trx: list[str] = Field(description="Lista de 3 pasos de tratamiento (químico, orgánico y cultural).")

SYSTEM_PROMPT = """
Eres un experto fitopatólogo con enfoque en agricultura tropical de **Guatemala y Centroamérica**. Genera un diagnóstico sintético basado en la planta y enfermedad indicada.

**Reglas estrictas:**
1. **Nomenclatura (nx):** Usa nombres regionales (ej. usa 'Hielo' o 'Rancha' si aplica, junto al nombre técnico). No uses siglas.
2. **Tratamientos (trx):** Menciona únicamente **ingredientes activos** (Fungicidas/Bactericidas) y extractos naturales. Prohibido usar nombres comerciales (ej. no digas 'Amistar', di 'Azoxistrobina').
3. **Realismo:** Los tratamientos deben ser los disponibles en el mercado centroamericano.
4. **Descripción (dx):** No definas la enfermedad de forma general; descríbela **como si estuvieras viendo una fotografía específica**, mencionando texturas, colores de las manchas y bordes.
5. **Variabilidad (est):** Genera una etapa (est) analizando la imagen proporcionada de la enfermedad (Inicial, Intermedia o Avanzada) y adapta la descripción (dx) a esa etapa. Si es una imagen de una hoja o planta sana, entonces agrega "(N/A) Sana".

"""

def get_ai_diagnosis(class_name, image_path, max_retries=6):
    """Llamada a la API de Gemini 3 con sistema de Auto-Retry (Exponencial Backoff)"""
    prompt = f"Planta y enfermedad objetivo: {class_name}. Analiza la imagen minuciosamente para determinar la etapa (est)."
    full_image_path = os.path.join("dataset", image_path)
    
    for attempt in range(max_retries):
        try:
            # Abrir imagen localmente
            img = PIL.Image.open(full_image_path)
            
            # Generar contenido usando Schema
            response = client.models.generate_content(
                model="gemini-3-flash-preview", 
                contents=[SYSTEM_PROMPT, prompt, img],
                config={
                    "response_mime_type": "application/json",
                    "response_json_schema": CropAnalysis.model_json_schema(),
                }
            )
            
            # Validar y convertir de vuelta a JSON (dict) asegurando la estructura
            parsed_obj = CropAnalysis.model_validate_json(response.text)
            return json.loads(parsed_obj.model_dump_json())
            
        except Exception as e:
            err_str = str(e).lower()
            # Si el error es de Cuota/Rate Limit (429), hacemos backoff automático
            if "429" in err_str or "quota" in err_str or "too many requests" in err_str:
                wait_time = (2 ** attempt) + random.uniform(1, 3)
                print(f"  ⏳ [Rate Limit] Esperando {wait_time:.1f}s para {class_name}...")
                time.sleep(wait_time)
            else:
                print(f"❌ Error en API para {class_name} ({image_path}): {e}")
                return None
                
    print(f"⛔ Se alcanzó el límite de reintentos para {image_path}.")
    return None

def process_single_image(item, idx):
    """Función envoltura para procesar en hilos"""
    class_name = item["class_name"]
    image_path = item["file_path"]
    
    print(f"  👁️  Analizando imagen {idx} ({class_name})...")
    diagnosis = get_ai_diagnosis(class_name, image_path)
    return idx, diagnosis, item["split"], image_path

def main():
    if not os.path.exists(METADATA_FILE):
        print(f"Error: No se encuentra {METADATA_FILE}")
        return

    with open(METADATA_FILE, 'r', encoding='utf-8') as f:
        metadata_list = json.load(f)

    # Límite por clase/split. 29 clases * 550 = 15,950 imágenes en Train 🎉
    MAX_SAMPLES_BY_SPLIT = {
        "Train": 550,
        "Val": 100,  # ~2,900 para Validación
        "Test": 41   # Toma todas las de Test
    }

    processed_counters = {}
    to_process = []
    
    # 1. Contabilizar los que ya se han procesado anteriormente
    for item in metadata_list:
        if item.get("processed", False):
            key = f"{item['split']}_{item['class_name']}"
            processed_counters[key] = processed_counters.get(key, 0) + 1

    # 2. Encolar respetando el límite diversificado por partición
    for i, item in enumerate(metadata_list):
        if not item.get("processed", False):
            split = item['split']
            key = f"{split}_{item['class_name']}"
            
            # Obtener el límite específico. Si no existe la partición, usa 0 por defecto.
            max_limit = MAX_SAMPLES_BY_SPLIT.get(split, 0)
            
            if processed_counters.get(key, 0) < max_limit:
                to_process.append(i)
                processed_counters[key] = processed_counters.get(key, 0) + 1
    
    if not to_process:
        print("¡Todas las imágenes asignadas al límite ya han sido procesadas!")
        return

    print(f"Total en JSON: {len(metadata_list)} | Encoladas ahora para la meta de 15.9k: {len(to_process)}")
    
    # --- PROCESAMIENTO EN BATCH CON HILOS MULTIPLES ---
    for i in range(0, len(to_process), BATCH_SIZE):
        batch_indices = to_process[i:i + BATCH_SIZE]
        print(f"\n🚀 Procesando batch de {len(batch_indices)} imágenes (Índices: {batch_indices[0]} al {batch_indices[-1]})...")

        batch_results = []
        
        # Ejecutar en concurrencia (Multithreading)
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            # Crear un diccionario de futuros
            futures = {
                executor.submit(process_single_image, metadata_list[idx], idx): idx 
                for idx in batch_indices
            }
            
            for future in concurrent.futures.as_completed(futures):
                idx, diagnosis, split, img_path = future.result()
                
                if diagnosis:
                    training_entry = {
                        "question_id": f"img_{idx}",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {"type": "image", "image": img_path},
                                    {"type": "text", "text": "Analiza esta imagen y dime qué tipo de enfermedad es, cómo puedo tratarla y en qué etapa está. Devuelve tu respuesta únicamente en este formato JSON: {nx: "", dx: "", est: "", trx: ["", "", ""]}"}
                                ]
                            },
                            {
                                "role": "assistant",
                                "content": [
                                    {"type": "text", "text": json.dumps(diagnosis, ensure_ascii=False)}
                                ]
                            }
                        ]
                    }
                    batch_results.append((split, training_entry))
                    metadata_list[idx]["processed"] = True

        # Guardar progreso tras cada Batch
        with open(METADATA_FILE, 'w', encoding='utf-8') as f:
            json.dump(metadata_list, f, indent=4, ensure_ascii=False)

        # Append a los ficheros JSONL separando por Split
        for split, entry in batch_results:
            output_file = f"gemma_finetuning_{split.lower()}.jsonl"
            with open(output_file, 'a', encoding='utf-8') as f:
                f.write(json.dumps(entry, ensure_ascii=False) + "\n")

        print(f"✅ Batch finalizado ({len(batch_results)} procesadas exitosamente). Progreso guardado.")

if __name__ == "__main__":
    main()