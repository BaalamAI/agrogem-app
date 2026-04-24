import os
import json

def main():
    dataset_dir = "/Volumes/ProyectosYDocs/BALAM/agrogem-app/llm/dataset"
    splits = ["Train", "Val", "Test"]
    
    metadata = []
    
    print("Building dataset metadata...")
    for split in splits:
        split_path = os.path.join(dataset_dir, split)
        if not os.path.exists(split_path):
            print(f"Skipping {split}, not found.")
            continue
            
        classes = os.listdir(split_path)
        for class_name in classes:
            class_path = os.path.join(split_path, class_name)
            if os.path.isdir(class_path):
                images = [f for f in os.listdir(class_path) if not f.startswith('.')]
                for img in images:
                    rel_path = os.path.join(split, class_name, img)
                    metadata.append({
                        "file_path": rel_path,
                        "split": split,
                        "class_name": class_name,
                        "processed": False
                    })
                    
    output_file = "/Volumes/ProyectosYDocs/BALAM/agrogem-app/llm/dataset_metadata.json"
    print("Writing to JSON file...")
    with open(output_file, 'w') as f:
        json.dump(metadata, f, indent=4)
        
    print(f"Success! Metadata for {len(metadata)} images saved to:\n{output_file}")

if __name__ == "__main__":
    main()
