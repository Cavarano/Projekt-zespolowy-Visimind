import torch
from ultralytics import YOLO
import ultralytics

# Дозволяємо завантаження нестандартних глобальних об'єктів
torch.serialization.add_safe_globals([ultralytics.nn.tasks.DetectionModel])

def main():
    # Завантаження моделі YOLO
    model = YOLO("yolov8m.pt")  # Переконайся, що шлях до моделі правильний

    # Тренування моделі
    results = model.train(
        data="data.yaml",  # Використання твого файлу data.yaml
        epochs=50,  # Кількість епох
        imgsz=640,  # Розмір вхідного зображення
        batch=16,  # Розмір batch
        workers=4,  # Кількість потоків
        model="yolov8m.pt"
    )

    # Збереження моделі
    model.export(format="onnx")  # Опціонально: експортувати в ONNX для подальшого використання

if __name__ == '__main__':
    main()


# import torch
# from ultralytics import YOLO
# import ultralytics

# # Дозволяємо завантаження нестандартних глобальних об'єктів
# torch.serialization.add_safe_globals([ultralytics.nn.tasks.DetectionModel])

# # Завантаження моделі YOLO
# model = YOLO("yolov8m.pt")  # Переконайся, що шлях до моделі правильний

# # Тренування моделі
# results = model.train(
#     data="data.yaml",  # Використання твого файлу data.yaml
#     epochs=50,  # Кількість епох
#     imgsz=640,  # Розмір вхідного зображення
#     batch=16,  # Розмір batch
#     workers=4,  # Кількість потоків
#     model="yolov8m.pt"
# )

# # Збереження моделі
# model.export(format="onnx")  # Опціонально: експортувати в ONNX для подальшого використання

