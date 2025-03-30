# import os
# import json

# def convert_labelme_to_yolo(labelme_dir, output_dir, img_width, img_height):
#     os.makedirs(output_dir, exist_ok=True)

#     for filename in os.listdir(labelme_dir):
#         if filename.endswith(".json"):
#             json_path = os.path.join(labelme_dir, filename)
#             with open(json_path, "r") as f:
#                 data = json.load(f)

#             image_name = data["imagePath"]
#             txt_filename = os.path.join(output_dir, os.path.splitext(filename)[0] + ".txt")

#             with open(txt_filename, "w") as txt_file:
#                 for shape in data["shapes"]:
#                     label = shape["label"]
#                     points = shape["points"]
                    
#                     x1, y1 = points[0]
#                     x2, y2 = points[1]
                    
#                     x_center = ((x1 + x2) / 2) / img_width
#                     y_center = ((y1 + y2) / 2) / img_height
#                     width = abs(x2 - x1) / img_width
#                     height = abs(y2 - y1) / img_height

#                     txt_file.write(f"0 {x_center} {y_center} {width} {height}\n")

#             print(f"Converted {filename} to {txt_filename}")

# labelme_json_dir = "../inputs/train/json_labels"
# yolo_output_dir = "../inputs/train/labels"
# img_width, img_height = 640, 480  # Потрібно встановити правильний розмір

# convert_labelme_to_yolo(labelme_json_dir, yolo_output_dir, img_width, img_height)

import json
import glob
from pathlib import Path

# Mapowanie nazw klas na ID w YOLO (dostosuj do swojego projektu)
category_to_id = {
    "A-1": 0, "A-2": 1, "A-3": 2, "A-4": 3, "A-5": 4, "A-6a": 5, "A-6b": 6, "A-6c": 7, "A-6d": 8, "A-6e": 9,
    "A-7": 10, "A-8": 11, "A-9": 12, "A-10": 13, "A-11": 14, "A-11a": 15, "A-12a": 16, "A-12b": 17, "A-12c": 18,
    "A-13": 19, "A-14": 20, "A-15": 21, "A-16": 22, "A-17": 23, "A-18a": 24, "A-18b": 25, "A-19": 26, "A-20": 27,
    "A-21": 28, "A-22": 29, "A-23": 30, "A-24": 31, "A-25": 32, "A-26": 33, "A-27": 34, "A-28": 35, "A-29": 36,
    "A-30": 37, "A-31": 38, "A-32": 39, "A-33": 40, "A-34": 41, "B-1": 42, "B-2": 43, "B-3": 44, "B-3a": 45,
    "B-4": 46, "B-5": 47, "B-6": 48, "B-7": 49, "B-8": 50, "B-9": 51, "B-10": 52, "B-11": 53, "B-12": 54, "B-13": 55,
    "B-13a": 56, "B-14": 57, "B-15": 58, "B-16": 59, "B-17": 60, "B-18": 61, "B-19": 62, "B-20": 63, "B-21": 64,
    "B-22": 65, "B-23": 66, "B-24": 67, "B-25": 68, "B-26": 69, "B-27": 70, "B-28": 71, "B-29": 72, "B-30": 73,
    "B-31": 74, "B-32": 75, "B-33": 76, "B-34": 77, "B-35": 78, "B-36": 79, "B-37": 80, "B-38": 81, "B-39": 82,
    "B-40": 83, "B-41": 84, "B-42": 85, "B-43": 86, "B-44": 87, "C-1": 88, "C-2": 89, "C-3": 90, "C-4": 91, "C-5": 92,
    "C-6": 93, "C-7": 94, "C-8": 95, "C-9": 96, "C-10": 97, "C-11": 98, "C-12": 99, "C-13": 100, "C-13a": 101,
    "C-14": 102, "C-15": 103, "C-16": 104, "C-16a": 105, "C-17": 106, "C-18": 107, "C-19": 108, "D-1": 109, "D-2": 110,
    "D-3": 111, "D-4a": 112, "D-4b": 113, "D-5": 114, "D-6": 115, "D-6a": 116, "D-6b": 117, "D-7": 118, "D-8": 119,
    "D-9": 120, "D-10": 121, "D-11": 122, "D-12": 123, "D-13": 124, "D-13a": 125, "D-14": 126, "D-15": 127, "D-16": 128,
    "D-17": 129, "D-18": 130, "D-18a": 131, "D-18b": 132, "D-19": 133, "D-20": 134, "D-21": 135, "D-21a": 136, "D-22": 137, "D-23": 138,
    "D-23a": 139, "D-24": 140, "D-25": 141, "D-26": 142, "D-26a": 143, "D-26b": 144, "D-26c": 145, "D-26d": 146, "D-27": 147,
    "D-28": 148, "D-29": 149, "D-30": 150, "D-31": 151, "D-32": 152, "D-33": 153, "D-34": 154, "D-34a": 155, "D-35": 156,
    "D-35a": 157, "D-36": 158, "D-37": 159, "D-38": 160, "D-39": 161, "D-40": 162, "D-41": 163, "D-42": 164, "D-43": 165,
    "D-44": 166, "D-45": 167, "D-46": 168, "D-47": 169, "D-48": 170, "D-49": 171, "D-50": 172, "D-51": 173, "F-1": 174,
    "F-2": 175, "F-2a": 176, "F-3": 177, "F-4": 178, "F-5": 179, "F-6": 180, "F-7": 181, "F-8": 182, "F-9": 183,
    "F-10": 184, "F-11": 185, "F-12": 186, "F-13": 187, "F-14a": 188, "F-14b": 189, "F-14c": 190, "F-15": 191, "F-16": 192,
    "F-17": 193, "F-18": 194, "F-19": 195, "F-20": 196, "F-21": 197, "F-22": 198, "G-1a": 199, "G-1b": 200, "G-1c": 201,
    "G-1d": 202, "G-1e": 203, "G-1f": 204, "G-2": 205, "G-3": 206, "G-4": 207, "P-8a": 208, "P-8b": 209, "P-8c": 210,
    "P-8d": 211, "P-8e": 212, "P-8f": 213, "P-8g": 214, "P-8h": 215, "P-8i": 216, "P-9": 217, "P-9b": 218
}


# Ścieżki
input_folder = r"C:\Усяке\university\projekt_zesp\Labelowanie_projekt_zespolowy"
output_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\outputs"

# Tworzenie folderu wyjściowego, jeśli nie istnieje
Path(output_folder).mkdir(parents=True, exist_ok=True)

# Pobranie plików JSON
json_files = glob.glob(f"{input_folder}/*.json")
print("🔍 Znalezione JSON-y:", json_files)

for file in json_files:
    with open(file, "r", encoding="utf-8") as f:
        data = json.load(f)

    print(f"\n📂 Przetwarzanie pliku: {file}")
    print("📏 Rozmiar obrazu:", data["imageWidth"], "x", data["imageHeight"])

    image_width = data["imageWidth"]
    image_height = data["imageHeight"]
    name = Path(file).stem  # Nazwa pliku bez rozszerzenia

    output_file = Path(output_folder) / f"{name}.txt"

    with open(output_file, "w") as txt:
        for item in data["shapes"]:
            label = item["label"]
            if label not in category_to_id:
                print(f"⚠️ Pominięto nieznaną klasę: {label}")
                continue  # Pomijamy nieznane klasy

            class_id = category_to_id[label]
            points = item["points"]

            # Bounding box w formacie LabelMe (rectangle)
            x1, y1 = points[0]
            x2, y2 = points[1]

            # Konwersja do YOLO (znormalizowane wartości 0-1)
            x_center = round(((x1 + x2) / 2) / image_width, 6)
            y_center = round(((y1 + y2) / 2) / image_height, 6)
            bbox_width = round(abs(x2 - x1) / image_width, 6)
            bbox_height = round(abs(y2 - y1) / image_height, 6)

            print(f"🟩 {label}: YOLO format -> {class_id} {x_center} {y_center} {bbox_width} {bbox_height}")

            txt.write(f"{class_id} {x_center} {y_center} {bbox_width} {bbox_height}\n")

print("\n✅ Konwersja zakończona!")

