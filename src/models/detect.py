from ultralytics import YOLO
import cv2

# Завантажуємо натреновану модель
model = YOLO("runs/detect/train/weights/best.pt")  # Шлях до натренованої моделі

# Завантажуємо зображення
image_path = "test_image.jpg"
image = cv2.imread(image_path)

# Запускаємо детекцію
results = model.predict(image, imgsz=640)

# Виводимо результати
for result in results:
    boxes = result.boxes
    for box in boxes:
        x1, y1, x2, y2 = map(int, box.xyxy[0])  # Отримуємо координати bound box
        conf = box.conf[0]  # Ймовірність (confidence)
        label = result.names[int(box.cls[0])]  # Назва класу
        cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(image, f"{label} {conf:.2f}", (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

# Показуємо зображення
cv2.imshow("Detected", image)
cv2.waitKey(0)
cv2.destroyAllWindows()
