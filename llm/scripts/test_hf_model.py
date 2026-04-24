import json
import random
from PIL import Image
import torch
from transformers import AutoProcessor, AutoModelForImageTextToText, TextStreamer

# 1. Configurar rutas locales
JSONL_PATH = "/Volumes/ProyectosYDocs/BALAM/agrogem-app/llm/json/gemma_finetuning_test.jsonl"
BASE_IMAGE_PATH = "/Volumes/ProyectosYDocs/BALAM/agrogem-app/llm/dataset/"

def main():
    # 2. Cargar el dataset y seleccionar un índice aleatorio
    dataset = []
    with open(JSONL_PATH, "r", encoding="utf-8") as f:
        for line in f:
            try:
                dataset.append(json.loads(line))
            except json.JSONDecodeError:
                continue

    test_idx = random.randint(0, len(dataset) - 1)
    example = dataset[test_idx]

    # 3. Extraer la información del JSONL
    user_content = example["messages"][0]["content"]
    image_rel_path = next(item["image"] for item in user_content if item["type"] == "image")
    instruction = next(item["text"] for item in user_content if item["type"] == "text")

    assistant_message = example["messages"][1]["content"][0]["text"]

    # 4. Cargar la imagen local
    image_path = BASE_IMAGE_PATH + image_rel_path
    image = Image.open(image_path).convert("RGB")

    # 5. Cargar el modelo y procesador
    model_id = "alvarog1318/gemma4-vision-crop-deseases_16bit"
    print(f"Cargando modelo y procesador desde Hugging Face Hub: {model_id}...")

    processor = AutoProcessor.from_pretrained(model_id)

    model = AutoModelForImageTextToText.from_pretrained(
        model_id,
        device_map="auto",
        torch_dtype=torch.float16 # Float16 para optimizar inferencia
    )

    # Usaremos el chat_template nativo del modelo (Gemma 3)

    # 6. Preparar los mensajes (Gemma 4 style)
    # Deshabilitamos el thinking usando un system prompt sin el token <|think|>
    # La modalidad ya está ordenada: imagen antes que texto, como recomienda la doc.
    messages = [
        {
            "role": "system",
            "content": "Eres un experto Fitopatólogo Especialista en enfermedades de cultivos. Tu funciones es analizar la imagen y responder a la pregunta del usuario con el siguiente formato JSON: {nx: \"\", dx: \"\", est: \"\", trx: [\"\", \"\", \"\"]}"
        },
        {
            "role": "user",
            "content": [
                {"type": "image"},
                {"type": "text", "text": instruction}
            ]
        }
    ]

    # 7. Procesar la entrada usando la estructura original de Unsloth
    input_text = processor.apply_chat_template(
        messages, 
        add_generation_prompt=True
    )
    
    print(f"\n--- PROMPT EXACTO ---\n{input_text}\n---------------------\n")

    inputs = processor(
        images=image,
        text=input_text,
        add_special_tokens=False,
        return_tensors="pt"
    ).to(model.device)

    # 8. Imprimir contexto
    print("-" * 50)
    print(f"Probando con imagen aleatoria (índice #{test_idx}): {image_rel_path}")
    print(f"Pregunta: {instruction}")
    print("-" * 50)
    print("Respuesta del modelo:")

    # 9. Generar la respuesta
    text_streamer = TextStreamer(processor, skip_prompt=True, skip_special_tokens=True)

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            streamer=text_streamer,
            max_new_tokens=1024,
            use_cache=True,
            temperature=0.2, # Valor bajo para respuestas deterministas/JSON
            top_p=0.95,
            top_k=64
        )

    print("\n" + "-" * 50)
    print(f"Respuesta real esperada:\n{assistant_message}")

if __name__ == "__main__":
    main()
