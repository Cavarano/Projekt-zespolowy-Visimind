import os
import random
import shutil

# Шлях до папок з зображеннями та мітками
image_folder = r"C:\Усяке\university\projekt_zesp\UWM"
label_folder = r"C:\Усяке\university\projekt_zesp\yolo_labels"

# Шляхи до папок, куди потрібно копіювати дані
train_image_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\images\train"
val_image_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\images\val"
test_image_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\images\test"

train_label_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\labels\train"
val_label_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\labels\val"
test_label_folder = r"C:\Усяке\university\projekt_zesp\road_sign_detection\project_dataset\labels\test"

# Відсоток для тренування та валідації
train_percentage = 0.75
val_percentage = 0.15
test_percentage = 0.1

# Отримуємо список всіх зображень
image_files = [f for f in os.listdir(image_folder) if f.endswith(".jpg")]

# Переміщаємо їх
random.shuffle(image_files)

# Розподіляємо зображення на train, val та test
train_count = int(len(image_files) * train_percentage)
val_count = int(len(image_files) * val_percentage)
test_count = len(image_files) - train_count - val_count  # решта для тесту

train_images = image_files[:train_count]
val_images = image_files[train_count:train_count + val_count]
test_images = image_files[train_count + val_count:]

# Створюємо директорії для трьох наборів, якщо вони не існують
os.makedirs(train_image_folder, exist_ok=True)
os.makedirs(val_image_folder, exist_ok=True)
os.makedirs(test_image_folder, exist_ok=True)

os.makedirs(train_label_folder, exist_ok=True)
os.makedirs(val_label_folder, exist_ok=True)
os.makedirs(test_label_folder, exist_ok=True)

# Функція для копіювання файлів
def copy_files(files, src_folder, dest_folder, label_folder, dest_label_folder):
    for file in files:
        # Перевірка, чи існує файл зображення
        image_src_path = os.path.join(src_folder, file)
        if not os.path.exists(image_src_path):
            print(f"Зображення {image_src_path} не існує!")
            continue  # Пропускаємо, якщо файл не існує
        else:
            print(f"Зображення знайдено: {image_src_path}")
        
        # Копіюємо зображення
        print(f"Копіюємо зображення до: {os.path.join(dest_folder, file)}")
        shutil.copy(image_src_path, os.path.join(dest_folder, file))
        
        # Перевірка, чи існує відповідна мітка
        label_file = file.replace(".jpg", ".txt")
        label_src_path = os.path.join(label_folder, label_file)
        if os.path.exists(label_src_path):
            print(f"Мітка знайдена: {label_src_path}")
            shutil.copy(label_src_path, os.path.join(dest_label_folder, label_file))
        else:
            print(f"Мітка для {file} не знайдена!")

# Копіюємо файли
copy_files(train_images, image_folder, train_image_folder, label_folder, train_label_folder)
copy_files(val_images, image_folder, val_image_folder, label_folder, val_label_folder)
copy_files(test_images, image_folder, test_image_folder, label_folder, test_label_folder)

print(f"Train data: {len(train_images)} images, {len(train_images)} labels")
print(f"Validation data: {len(val_images)} images, {len(val_images)} labels")
print(f"Test data: {len(test_images)} images, {len(test_images)} labels")
