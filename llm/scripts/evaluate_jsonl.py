import json
import os
from collections import Counter

def evaluate_jsonl(filename):
    if not os.path.exists(filename):
        print(f"Error: No se encuentra {filename}")
        return

    print(f"Analizando la diversidad en {filename}...\n")
    
    class_counts = Counter()
    total_samples = 0
    
    with open(filename, 'r', encoding='utf-8') as f:
        for line in f:
            if not line.strip():
                continue
            try:
                data = json.loads(line)
                # Extraemos la clase del image path que está en el payload
                # Forma típica: "Train/Tomato - Yellow Leaf Curl Virus/img.jpg"
                image_path = data["messages"][0]["content"][0]["image"]
                
                # El formato del path asume que la estructura es Split/Clase/Imagen
                # o el nombre de la carpeta intermedia representa la clase.
                parts = image_path.split("/")
                if len(parts) >= 2:
                    class_name = parts[-2]
                else:
                    class_name = "Desconocido"
                
                class_counts[class_name] += 1
                total_samples += 1
            except Exception:
                pass
                
    print("=" * 60)
    print(f"🌟 RESUMEN DE DIVERSIDAD: {filename} 🌟")
    print("=" * 60)
    print(f"Total de imágenes procesadas: {total_samples}")
    print(f"Total de clases distintas: {len(class_counts)}\n")
    
    if len(class_counts) > 0:
        min_class_val = min(class_counts.values())
        max_class_val = max(class_counts.values())
        
        if min_class_val == max_class_val:
            print("✅ ESTADO: ¡Balance PERFECTO!")
            print(f"🎯 META: Cada clase aportó exactamente {min_class_val} imágenes.")
        else:
            print("⚠️ ESTADO: No está balanceado perfectamente.")
            print(f"📊 Desfase: La clase menos representada tiene {min_class_val} y la mayor {max_class_val}.\n")
            
        print("\nDesglose de imágenes por clase:")
        for cls, count in sorted(class_counts.items(), key=lambda x: x[0]):
            print(f"  - {cls}: {count} imágenes")
    else:
        print("No se encontraron registros válidos.")

if __name__ == "__main__":
    evaluate_jsonl("gemma_finetuning_train.jsonl")
    print("\n\n")
    evaluate_jsonl("gemma_finetuning_val.jsonl")
