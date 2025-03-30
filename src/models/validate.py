from ultralytics import YOLO

# Завантаження моделі
model = YOLO("runs/detect/train/weights/best.pt")

# Запуск валідації
results = model.val()

# Виведення результатів
print(results)
