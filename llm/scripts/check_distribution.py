import json
import os
from collections import Counter

def check_distribution():
    metadata_file = "dataset_metadata.json"
    if not os.path.exists(metadata_file):
        print(f"Error: No se encuentra el archivo {metadata_file}.")
        return

    print("Analizando metadata...")
    with open(metadata_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Contadores
    split_counts = Counter()
    class_counts_by_split = {
        "Train": Counter(),
        "Val": Counter(),
        "Test": Counter()
    }
    processed_count = 0
    total_count = len(data)

    for item in data:
        split = item.get("split")
        cls = item.get("class_name")
        processed = item.get("processed", False)

        split_counts[split] += 1
        if split in class_counts_by_split:
            class_counts_by_split[split][cls] += 1
        
        if processed:
            processed_count += 1

    print("=" * 60)
    print("🌟 RESUMEN GENERAL DEL DATASET 🌟")
    print("=" * 60)
    print(f"Total imágenes     : {total_count}")
    print(f"Procesadas por IA  : {processed_count} ({(processed_count/total_count)*100 if total_count > 0 else 0:.2f}%)")
    print(f"Pendientes por IA  : {total_count - processed_count}\n")

    for split in ["Train", "Val", "Test"]:
        print("-" * 60)
        print(f"📁 Partición {split.upper()} | Total general asignado: {split_counts[split]} imágenes")
        print("-" * 60)
        
        counts = class_counts_by_split[split]
        if not counts:
            print("No hay imágenes catalogadas en esta partición.")
            print("\n")
            continue
            
        min_class_val = min(counts.values())
        max_class_val = max(counts.values())
        
        if min_class_val == max_class_val:
            print(f"✅ ESTADO: ¡Balance PERFECTO!")
            print(f"🎯 META: Las {len(counts)} clases tienen exactamente {min_class_val} imágenes cada una.\n")
        else:
            print(f"⚠️ ESTADO: No está balanceado.")
            print(f"📊 Desfase: La clase más pequeña tiene {min_class_val} y la más grande {max_class_val}.\n")
            
        # Listamos la distribución de las clases ordenadas alfabéticamente
        print("Clases individuales:")
        for cls, count in sorted(counts.items(), key=lambda x: x[0]):
            print(f"  - {cls}: {count} imágenes")
        print("\n")

if __name__ == "__main__":
    check_distribution()
