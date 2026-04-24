import os
import random

def get_class_counts(split_path):
    class_counts = {}
    classes = os.listdir(split_path)
    for c in classes:
        c_path = os.path.join(split_path, c)
        if os.path.isdir(c_path):
            # Contar archivos que no sean ocultos
            images = [f for f in os.listdir(c_path) if not f.startswith('.')]
            class_counts[c] = len(images)
    return class_counts

def print_counts(counts, title):
    print(f"--- {title} ---")
    for c, count in sorted(counts.items(), key=lambda x: x[1]):
        print(f"{c}: {count}")
    print()

def balance_split(split_path):
    print("=====================================")
    print(f"ANALYZING {os.path.basename(split_path)}")
    print("=====================================")
    counts = get_class_counts(split_path)
    if not counts:
        print(f"No classes found in {split_path}")
        return
    
    print_counts(counts, f"Counts before balancing in {os.path.basename(split_path)}")
    
    min_count = min(counts.values())
    max_count = max(counts.values())
    
    if min_count == max_count:
        print("Dataset is already perfectly balanced!\n")
        return
    
    print(f"Dataset is NOT balanced. Min: {min_count}, Max: {max_count}.")
    print(f"Deleting excess images to balance at {min_count} images per class...\n")
    
    for c in counts.keys():
        c_path = os.path.join(split_path, c)
        images = [f for f in os.listdir(c_path) if not f.startswith('.')]
        if len(images) > min_count:
            # We need to delete some images
            # Let's randomly select `min_count` images to KEEP, and delete the rest
            random.seed(42)  # For reproducibility
            images_to_delete = random.sample(images, len(images) - min_count)
            for img in images_to_delete:
                os.remove(os.path.join(c_path, img))
                
    print("Done balancing.")
    # Verify
    new_counts = get_class_counts(split_path)
    print_counts(new_counts, f"Counts after balancing in {os.path.basename(split_path)}")

def main():
    dataset_dir = "/Volumes/ProyectosYDocs/BALAM/agrogem-app/llm/dataset"
    splits = ["Train", "Val", "Test"]
    
    for split in splits:
        split_path = os.path.join(dataset_dir, split)
        if os.path.exists(split_path):
            balance_split(split_path)
        else:
            print(f"Split {split} not found at {split_path}")

if __name__ == "__main__":
    main()
