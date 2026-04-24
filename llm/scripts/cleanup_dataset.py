import os
import json
import shutil

def get_used_images():
    used_images = set()
    files = ["gemma_finetuning_train.jsonl", "gemma_finetuning_val.jsonl", "gemma_finetuning_test.jsonl"]
    
    for f_name in files:
        if os.path.exists(f_name):
            with open(f_name, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip(): continue
                    try:
                        data = json.loads(line)
                        # Ej: "Train/Tomato - Yellow Leaf Curl Virus/img.jpg"
                        img_path = data["messages"][0]["content"][0]["image"]
                        # Como están dentro de la carpeta dataset/:
                        full_img_path = os.path.join("dataset", img_path)
                        used_images.add(os.path.abspath(full_img_path))
                    except Exception as e:
                        pass
    return used_images

def clean_excess_images():
    used_images = get_used_images()
    print(f"Buscando conservar exactamente {len(used_images)} imágenes indicadas en los .jsonl...")
    
    if len(used_images) == 0:
        print("⚠️ No se encontraron imágenes en los JSONL. ¿Aún no has generado los datos? Abortando por seguridad.")
        return

    dataset_dir = os.path.abspath("dataset")
    if not os.path.exists(dataset_dir):
        print(f"Error: La carpeta {dataset_dir} no existe.")
        return

    deleted_count = 0
    kept_count = 0
    
    # 1. Eliminar archivos que no se usan
    for root, dirs, files in os.walk(dataset_dir):
        for file in files:
            if file.lower().endswith(('.jpg', '.jpeg', '.png')):
                filepath = os.path.abspath(os.path.join(root, file))
                if filepath not in used_images:
                    try:
                        os.remove(filepath)
                        deleted_count += 1
                    except Exception as e:
                        print(f"No se pudo borrar {filepath}: {e}")
                else:
                    kept_count += 1
                    
    # 2. (Opcional) Eliminar carpetas vacías para que el ZIP quede limpio
    for root, dirs, files in os.walk(dataset_dir, topdown=False):
        for directory in dirs:
            dir_path = os.path.join(root, directory)
            if not os.listdir(dir_path):  # Si la carpeta está vacía
                os.rmdir(dir_path)
                
    print("\n✅ ¡Limpieza completada con éxito!")
    print(f"  🗑️  Imágenes purgadas y eliminadas del disco: {deleted_count}")
    print(f"  📁 Imágenes conservadas listas para el ZIP: {kept_count}")
    
    print("\n💡 Tip: Ahora puedes comprimir todo. Ingresa a la carpeta llm y ejecuta:")
    print("zip -r mi_dataset.zip dataset/ gemma_finetuning_*.jsonl")

if __name__ == "__main__":
    clean_excess_images()
