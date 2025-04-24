import cv2
import os
import torch
from ultralytics import YOLO
import os

def iou(box1, box2):
    """Обчислення IOU (Intersection Over Union) для фільтрації дублікатів."""
    x1 = max(box1[0], box2[0])
    y1 = max(box1[1], box2[1])
    x2 = min(box1[2], box2[2])
    y2 = min(box1[3], box2[3])
    
    intersection = max(0, x2 - x1) * max(0, y2 - y1)
    area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
    area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
    union = area1 + area2 - intersection

    return intersection / union if union != 0 else 0

def is_duplicate(new_box, existing_boxes, iou_threshold=0.5):
    """Перевірка чи новий bbox вже існує."""
    for box in existing_boxes:
        if iou(new_box[:4], box[:4]) > iou_threshold and new_box[4] == box[4]:
            return True
    return False

def detect_signs_with_tiling(
    image_path, model_path, class_names,
    conf_threshold=0.5, tile_size=640, overlap=0.2
):
    """
    Покращене розпізнавання знаків з плитками + зменшеними копіями (вдвічі та вчетверо) та фільтрацією дублікатів.
    """

    model = YOLO(model_path)
    original_img = cv2.imread(image_path)
    height, width, _ = original_img.shape
    output_img = original_img.copy()

    recognized_boxes = []  # зберігає: (x1, y1, x2, y2, class_id)

    # TILE'ING
    step = int(tile_size * (1 - overlap))
    for y in range(0, height, step):
        for x in range(0, width, step):
            x_end = min(x + tile_size, width)
            y_end = min(y + tile_size, height)
            tile = original_img[y:y_end, x:x_end]

            results = model(tile, conf=conf_threshold)

            for result in results:
                for box in result.boxes:
                    class_id = int(box.cls.item())
                    x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
                    x1_global, y1_global = x + x1, y + y1_
                    x2_global, y2_global = x + x2, y + y2

                    new_box = (x1_global, y1_global, x2_global, y2_global, class_id)
                    if not is_duplicate(new_box, recognized_boxes):
                        recognized_boxes.append(new_box)
                        sign_name = class_names[class_id] if class_id < len(class_names) else "Unknown"
                        cv2.rectangle(output_img, (x1_global, y1_global), (x2_global, y2_global), (0, 255, 0), 2)
                        cv2.putText(output_img, sign_name, (x1_global, y1_global - 10),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # Аналіз зменшених копій (0.5x та 0.25x)
    for scale_factor, color in [(0.5, (255, 0, 0)), (0.25, (0, 0, 255))]:
        small_img = cv2.resize(original_img, (0, 0), fx=scale_factor, fy=scale_factor)
        results = model(small_img, conf=conf_threshold)

        for result in results:
            for box in result.boxes:
                class_id = int(box.cls.item())
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                # Масштабуємо назад
                x1 = int(x1 / scale_factor)
                y1 = int(y1 / scale_factor)
                x2 = int(x2 / scale_factor)
                y2 = int(y2 / scale_factor)

                new_box = (x1, y1, x2, y2, class_id)
                if not is_duplicate(new_box, recognized_boxes):
                    recognized_boxes.append(new_box)
                    sign_name = class_names[class_id] if class_id < len(class_names) else "Unknown"
                    cv2.rectangle(output_img, (x1, y1), (x2, y2), color, 2)
                    cv2.putText(output_img, sign_name, (x1, y1 - 10),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

    # Підготовка результатів
    recognized_signs = [class_names[box[4]] if box[4] < len(class_names) else "Unknown" for box in recognized_boxes]
    unique_signs = list(set(recognized_signs))

    os.makedirs("outputs", exist_ok=True)

    image_name = os.path.splitext(os.path.basename(image_path))[0]

    output_name = f"outputs/{image_name}.jpg"
    output_path = output_name
    cv2.imwrite(output_name, output_img)

    return unique_signs, len(recognized_boxes), output_path

if __name__ == "__main__":
    model_path = "model/best.pt"
    image_path = "images_detect/test9.jpg"
    class_names = class_names = [
        "A-1", "A-2", "A-3", "A-4", "A-5", "A-6a", "A-6b", "A-6c", "A-6d", "A-7", 
        "A-8", "A-9", "A-10", "A-11a", "A-12a", "A-12b", "A-12c", "A-14", "A-15", "A-16", 
        "A-17", "A-18b", "A-20", "A-21", "A-24", "A-29", "A-30", "A-32", "B-1", "B-2", 
        "B-3a", "B-5", "B-9", "B-16", "B-18", "B-20", "B-21", "B-22", "B-23", "B-25", 
        "B-26", "B-33", "B-34", "B-35", "B-36", "B-39", "B-40", "B-41", "B-42", "B-43", 
        "B-44", "C-2", "C-4", "C-5", "C-8", "C-9", "C-10", "C-11", "C-12", "C-13", 
        "C-13a", "D-1", "D-2", "D-3", "D-4a", "D-4b", "D-6", "D-6b", "D-7", "D-11", 
        "D-12", "D-15", "D-18", "D-19", "D-20", "D-21", "D-23", "D-28", "D-29", "D-35", 
        "D-40", "D-41", "D-42", "D-43", "D-44", "D-45", "D-47", "F-5", "F-6", "F-10", 
        "F-11", "F-21", "G-1a", "G-1c", "P-8a", "P-8b", "P-8d", "P-8e", "P-8f", "P-8i"
    ]  # список назв класів

    signs, total_boxes, path = detect_signs_with_tiling(
        image_path=image_path,
        model_path=model_path,
        class_names=class_names
    )

    print("🚸 Розпізнані знаки:", signs)
    print("🔢 Кількість виявлень:", total_boxes)
    print("📸 Фото збережено у:", path)

# import cv2
# import numpy as np
# from ultralytics import YOLO
# import albumentations as A

# def detect_signs_enhanced(
#     image_path, model_path, class_names,
#     conf_threshold=0.5, tile_sizes=(640, 320), scale_factors=(1.0, 0.5, 0.25, 0.125)
# ):
#     model = YOLO(model_path)
#     original_img = cv2.imread(image_path)
#     height, width, _ = original_img.shape
#     output_img = original_img.copy()
#     detected_signs = []

#     # Аугментації для підсилення виявлення
#     augmentations = A.Compose([
#         A.HorizontalFlip(p=0.3),
#         A.VerticalFlip(p=0.1),
#         A.RandomBrightnessContrast(p=0.3),
#         A.Rotate(limit=10, p=0.3)
#     ])

#     def detect_on_tile(tile_img, offset_x=0, offset_y=0, scale=1.0):
#         nonlocal detected_signs, output_img
#         augmented_imgs = [tile_img]

#         # Аугментовані варіанти
#         for _ in range(2):  # 2 варіації
#             aug = augmentations(image=tile_img)['image']
#             augmented_imgs.append(aug)

#         for aug_img in augmented_imgs:
#             results = model(aug_img, conf=conf_threshold)
#             for result in results:
#                 for box in result.boxes:
#                     class_id = int(box.cls.item())
#                     sign_name = class_names[class_id] if class_id < len(class_names) else "Unknown"
#                     x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())

#                     # Масштабування bbox до оригінального зображення
#                     x1_global = int((offset_x + x1) / scale)
#                     y1_global = int((offset_y + y1) / scale)
#                     x2_global = int((offset_x + x2) / scale)
#                     y2_global = int((offset_y + y2) / scale)

#                     # Унікальність bbox
#                     duplicate = False
#                     for existing in detected_signs:
#                         ex_name, ex_x1, ex_y1, ex_x2, ex_y2 = existing
#                         if sign_name == ex_name and abs(ex_x1 - x1_global) < 20 and abs(ex_y1 - y1_global) < 20:
#                             duplicate = True
#                             break
#                     if not duplicate:
#                         detected_signs.append((sign_name, x1_global, y1_global, x2_global, y2_global))
#                         cv2.rectangle(output_img, (x1_global, y1_global), (x2_global, y2_global), (0, 255, 255), 2)
#                         cv2.putText(output_img, sign_name, (x1_global, y1_global - 10),
#                                     cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 255), 2)

#     # Основні проходи — різні масштаби
#     for scale in scale_factors:
#         resized = cv2.resize(original_img, (0, 0), fx=scale, fy=scale)
#         for tile_size in tile_sizes:
#             step = int(tile_size * 0.8)  # 20% перекриття
#             h, w, _ = resized.shape

#             for y in range(0, h, step):
#                 for x in range(0, w, step):
#                     x_end = min(x + tile_size, w)
#                     y_end = min(y + tile_size, h)
#                     tile = resized[y:y_end, x:x_end]

#                     if tile.shape[0] > 10 and tile.shape[1] > 10:
#                         detect_on_tile(tile, offset_x=x, offset_y=y, scale=scale)

#     # Вивід
#     unique_sign_names = list(set([sign[0] for sign in detected_signs]))
#     saved_path = image_path.replace(".", "_detected.")
#     cv2.imwrite(saved_path, output_img)

#     return unique_sign_names, len(unique_sign_names), saved_path

# if __name__ == "__main__":
#     model_path = "model/best.pt"
#     image_path = "images_detect/test9.jpg"
#     class_names = class_names = [
#         "A-1", "A-2", "A-3", "A-4", "A-5", "A-6a", "A-6b", "A-6c", "A-6d", "A-7", 
#         "A-8", "A-9", "A-10", "A-11a", "A-12a", "A-12b", "A-12c", "A-14", "A-15", "A-16", 
#         "A-17", "A-18b", "A-20", "A-21", "A-24", "A-29", "A-30", "A-32", "B-1", "B-2", 
#         "B-3a", "B-5", "B-9", "B-16", "B-18", "B-20", "B-21", "B-22", "B-23", "B-25", 
#         "B-26", "B-33", "B-34", "B-35", "B-36", "B-39", "B-40", "B-41", "B-42", "B-43", 
#         "B-44", "C-2", "C-4", "C-5", "C-8", "C-9", "C-10", "C-11", "C-12", "C-13", 
#         "C-13a", "D-1", "D-2", "D-3", "D-4a", "D-4b", "D-6", "D-6b", "D-7", "D-11", 
#         "D-12", "D-15", "D-18", "D-19", "D-20", "D-21", "D-23", "D-28", "D-29", "D-35", 
#         "D-40", "D-41", "D-42", "D-43", "D-44", "D-45", "D-47", "F-5", "F-6", "F-10", 
#         "F-11", "F-21", "G-1a", "G-1c", "P-8a", "P-8b", "P-8d", "P-8e", "P-8f", "P-8i"
#     ]  # список назв класів

#     signs, count, saved_image = detect_signs_enhanced(
#         image_path=image_path,
#         model_path=model_path,
#         class_names=class_names,
#         conf_threshold=0.5,
#         tile_sizes=(640, 320),  # великі та менші плитки
#         scale_factors=(1.0, 0.5, 0.25, 0.125)  # зменшення до 1/8
#     )

#     print(f"🔍 Знайдено {count} унікальних знаків: {signs}")
#     print(f"💾 Зображення з результатом збережено у: {saved_image}")