import cv2
import torch
from ultralytics import YOLO

def process_video(input_video_path, output_video_path, model_path, class_names, conf_threshold=0.5):
    """
    Обробляє відео: розпізнає дорожні знаки, додає рамки та текст.

    :param input_video_path: шлях до вхідного відео
    :param output_video_path: шлях до збереження обробленого відео
    :param model_path: шлях до моделі YOLO
    :param class_names: список класів дорожніх знаків
    :param conf_threshold: поріг довіри
    """

    # Завантажуємо модель YOLO
    model = YOLO(model_path)

    # Відкриваємо відео
    cap = cv2.VideoCapture(input_video_path)

    # Отримуємо властивості відео
    fps = int(cap.get(cv2.CAP_PROP_FPS))  # Частота кадрів
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))  # Ширина
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))  # Висота
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))  # Загальна кількість кадрів

    # Налаштовуємо відеозаписувач
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')  # Кодек для MP4
    out = cv2.VideoWriter(output_video_path, fourcc, fps, (width, height))

    frame_num = 0  # Лічильник кадрів

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break  # Вихід, якщо відео закінчилося

        frame_num += 1
        print(f"Обробка кадру {frame_num}/{frame_count}...")

        # Запускаємо модель YOLO
        results = model(frame, conf=conf_threshold)

        for result in results:
            for box in result.boxes:
                if box.cls is None:
                    continue  # Пропускаємо, якщо клас не визначений

                class_id = int(box.cls[0].item())

                if 0 <= class_id < len(class_names):
                    sign_name = class_names[class_id]
                else:
                    sign_name = "Unknown"

                # Отримуємо координати bounding box'а
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())

                # Малюємо bounding box
                cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)

                # Додаємо назву знака над bounding box'ом
                cv2.putText(frame, sign_name, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

        # Записуємо оброблений кадр у вихідне відео
        out.write(frame)

    # Закриваємо всі ресурси
    cap.release()
    out.release()
    cv2.destroyAllWindows()

    print(f"Оброблене відео збережено в: {output_video_path}")

# 🔹 Тестовий запуск
if __name__ == "__main__":
    model_path = "runs/detect/train4/weights/best.pt"  # Твоя навчена модель YOLO
    input_video = "inputs/road1.mp4"  # Вхідне відео
    output_video = "outputs/detected_b.mp4"  # Вихідне відео
    class_names = [
        "A-1", "A-2", "A-3", "A-4", "A-5", "A-6a", "A-6b", "A-6c", "A-6d", "A-6e",
        "A-7", "A-8", "A-9", "A-10", "A-11", "A-11a", "A-12a", "A-12b", "A-12c", "A-13",
        "A-14", "A-15", "A-16", "A-17", "A-18a", "A-18b", "A-19", "A-20", "A-21", "A-22",
        "A-23", "A-24", "A-25", "A-26", "A-27", "A-28", "A-29", "A-30", "A-31", "A-32",
        "A-33", "A-34", "B-1", "B-2", "B-3", "B-3a", "B-4", "B-5", "B-6", "B-7", "B-8",
        "B-9", "B-10", "B-11", "B-12", "B-13", "B-13a", "B-14", "B-15", "B-16", "B-17",
        "B-18", "B-19", "B-20", "B-21", "B-22", "B-23", "B-24", "B-25", "B-26", "B-27",
        "B-28", "B-29", "B-30", "B-31", "B-32", "B-33", "B-34", "B-35", "B-36", "B-37",
        "B-38", "B-39", "B-40", "B-41", "B-42", "B-43", "B-44", "C-1", "C-2", "C-3",
        "C-4", "C-5", "C-6", "C-7", "C-8", "C-9", "C-10", "C-11", "C-12", "C-13", "C-13a",
        "C-14", "C-15", "C-16", "C-16a", "C-17", "C-18", "C-19", "D-1", "D-2", "D-3",
        "D-4a", "D-4b", "D-5", "D-6", "D-6a", "D-6b", "D-7", "D-8", "D-9", "D-10", "D-11",
        "D-12", "D-13", "D-13a", "D-14", "D-15", "D-16", "D-17", "D-18", "D-18a", "D-18b",
        "D-19", "D-20", "D-21", "D-21a", "D-22", "D-23", "D-23a", "D-24", "D-25", "D-26",
        "D-26a", "D-26b", "D-26c", "D-26d", "D-27", "D-28", "D-29", "D-30", "D-31", "D-32",
        "D-33", "D-34", "D-34a", "D-35", "D-35a", "D-36", "D-37", "D-38", "D-39", "D-40",
        "D-41", "D-42", "D-43", "D-44", "D-45", "D-46", "D-47", "D-48", "D-49", "D-50",
        "D-51", "F-1", "F-2", "F-2a", "F-3", "F-4", "F-5", "F-6", "F-7", "F-8", "F-9",
        "F-10", "F-11", "F-12", "F-13", "F-14a", "F-14b", "F-14c", "F-15", "F-16", "F-17",
        "F-18", "F-19", "F-20", "F-21", "F-22", "G-1a", "G-1b", "G-1c", "G-1d", "G-1e",
        "G-1f", "G-2", "G-3", "G-4", "P-8a", "P-8b", "P-8c", "P-8d", "P-8e", "P-8f", "P-8g",
        "P-8h", "P-8i", "P-8j", "P-8k"
    ]  # Твої класи

    process_video(input_video, output_video, model_path, class_names)
