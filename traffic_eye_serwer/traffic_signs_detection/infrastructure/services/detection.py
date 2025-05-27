# # from fastapi import UploadFile
# # from app.core.config import model, class_names
# # from app.models.response_models import DetectionResponse
# # from pathlib import Path
# # import shutil
# # import cv2
# # import numpy as np

# # async def detect_signs_from_file(file: UploadFile) -> DetectionResponse:
# #     temp_file = Path("temp") / file.filename
# #     temp_file.parent.mkdir(exist_ok=True)
# #     with open(temp_file, "wb") as buffer:
# #         shutil.copyfileobj(file.file, buffer)

# #     img = cv2.imread(str(temp_file))
# #     img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

# #     results = model(img_rgb)

# #     detected = []
# #     for result in results:
# #         for box in result.boxes:
# #             class_id = int(box.cls.item())
# #             sign_name = class_names[class_id] if class_id < len(class_names) else "Unknown"
# #             detected.append(sign_name)

# #     temp_file.unlink()

# #     return DetectionResponse(signs=list(set(detected)))


# from fastapi import UploadFile
# from core.config_model import get_model, class_names
# from core.domain.detection import DetectionResponse, Box
# from infrastructure.services.idetection import IDetectionService
# from core.repositories.iroadsign import IRoadSignRepository
# from pathlib import Path
# import shutil
# import cv2
# import numpy as np
# import os
# import uuid

# class DetectionService(IDetectionService):
#     """A class implementing the detection service."""

#     sign_repository: IRoadSignRepository

#     def __init__(self, repository: IRoadSignRepository) -> None:
#         """The initializer of the 'detection service'."""

#         self.sign_repository = repository

#     # async def get_road_sign_by_id(self, road_sign_id: str) -> RoadSign | None:
#     #     """The method getting a road sign from the data storage.

#     #     Args:
#     #         road_sign_id (str): the id of the road sign.

#     #     Returns:
#     #         RoadSign | None: The road sign data if exists.
#     #     """
#     #     return await self._repository.get_road_sign_by_id(road_sign_id)

#     async def iou(self, box1, box2):
#         x1 = max(box1[0], box2[0])
#         y1 = max(box1[1], box2[1])
#         x2 = min(box1[2], box2[2])
#         y2 = min(box1[3], box2[3])
        
#         intersection = max(0, x2 - x1) * max(0, y2 - y1)
#         area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
#         area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
#         union = area1 + area2 - intersection

#         return intersection / union if union != 0 else 0

#     async def is_duplicate(self, new_box, existing_boxes, iou_threshold=0.5):
#         for box in existing_boxes:
#             if await self.iou(new_box[:4], box[:4]) > iou_threshold and new_box[4] == box[4]:
#                 return True
#         return False

#     # async def detect_signs_from_file(self, file: UploadFile) -> DetectionResponse | None:
#     #     model = get_model()
#     #     # Створення унікального імені для тимчасового та вихідного файлу
#     #     unique_filename = f"{uuid.uuid4()}.jpg"
#     #     temp_dir = Path("temp")
#     #     outputs_dir = Path("outputs")
#     #     temp_dir.mkdir(exist_ok=True)
#     #     outputs_dir.mkdir(exist_ok=True)

#     #     temp_file = temp_dir / unique_filename

#     #     # Збереження тимчасового файлу
#     #     with open(temp_file, "wb") as buffer:
#     #         shutil.copyfileobj(file.file, buffer)

#     #     # Завантаження зображення
#     #     img = cv2.imread(str(temp_file))
#     #     height, width, _ = img.shape

#     #     recognized_boxes = []

#     #     # Параметри
#     #     tile_size = 640
#     #     overlap = 0.2
#     #     conf_threshold = 0.5

#     #     # TILE'инг
#     #     step = int(tile_size * (1 - overlap))
#     #     for y in range(0, height, step):
#     #         for x in range(0, width, step):
#     #             x_end = min(x + tile_size, width)
#     #             y_end = min(y + tile_size, height)
#     #             tile = img[y:y_end, x:x_end]

#     #             results = model(tile, conf=conf_threshold)

#     #             for result in results:
#     #                 for box in result.boxes:
#     #                     class_id = int(box.cls.item())
#     #                     x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
#     #                     x1_global, y1_global = x + x1, y + y1_
#     #                     x2_global, y2_global = x + x2, y + y2

#     #                     new_box = (x1_global, y1_global, x2_global, y2_global, class_id)
#     #                     print(new_box)
#     #                     if not await self.is_duplicate(new_box, recognized_boxes):
#     #                         recognized_boxes.append(new_box)

#     #     # Зменшені копії
#     #     for scale_factor in [0.5, 0.25]:
#     #         small_img = cv2.resize(img, (0, 0), fx=scale_factor, fy=scale_factor)
#     #         results = model(small_img, conf=conf_threshold)

#     #         for result in results:
#     #             for box in result.boxes:
#     #                 class_id = int(box.cls.item())
#     #                 x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
#     #                 x1 = int(x1 / scale_factor)
#     #                 y1_ = int(y1_ / scale_factor)
#     #                 x2 = int(x2 / scale_factor)
#     #                 y2 = int(y2 / scale_factor)

#     #                 new_box = (x1, y1_, x2, y2, class_id)
#     #                 if not await self.is_duplicate(new_box, recognized_boxes):
#     #                     recognized_boxes.append(new_box)

#     #     # Намалювати бокси на картинці
#     #     for box in recognized_boxes:
#     #         x1, y1, x2, y2, class_id = box
#     #         label = class_names[class_id] if class_id < len(class_names) else "Unknown"
#     #         color = (0, 255, 0)  # Зелений колір
#     #         thickness = 2
#     #         cv2.rectangle(img, (x1, y1), (x2, y2), color, thickness)
#     #         cv2.putText(img, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

#     #     # Збереження вихідного зображення в outputs
#     #     output_file = outputs_dir / unique_filename
#     #     cv2.imwrite(str(output_file), img)

#     #     # Підготовка відповіді
#     #     # recognized_signs = [class_names[box[4]] if box[4] < len(class_names) else "Unknown" for box in recognized_boxes]
#     #     # unique_signs = list(set(recognized_signs))

#     #     recognized_ids = [class_names[box[4]] if box[4] < len(class_names) else None for box in recognized_boxes]
#     #     unique_ids = list(set(filter(None, recognized_ids)))

#     #     # Шукаємо RoadSign об'єкти по id
#     #     sign_objects = []
#     #     for sign_id in unique_ids:
#     #         sign = await self.sign_repository.get_road_sign_by_id(sign_id)
#     #         if sign:
#     #             sign_objects.append(sign)

#     #     # Очистити тимчасовий файл
#     #     temp_file.unlink()

#     #     # Створення повного URL для доступу до картинки
#     #     image_url = f"/static/{unique_filename}"

#     #     # print(unique_signs)
#     #     print(sign_objects)
#     #     print("BOXY: ", recognized_boxes)
#     #     box_objects = [Box(x1=box[0], y1=box[1], x2=box[2], y2=box[3], class_id=class_names[box[4]]) for box in recognized_boxes]
#     #     print(image_url)

#     #     return DetectionResponse(
#     #         signs=sign_objects,
#     #         total_boxes=len(recognized_boxes),
#     #         image_url=image_url,
#     #         boxes = box_objects,
#     #     ) if image_url else None


#     async def detect_signs_from_file(self, file: UploadFile) -> DetectionResponse | None:
#         model = get_model()
#         # Stworzenie unikalnego imienia dla tymczasowego i wyjściowego pliku
#         unique_filename = f"{uuid.uuid4()}.jpg"
#         temp_dir = Path("temp")
#         outputs_dir = Path("outputs")
#         temp_dir.mkdir(exist_ok=True)
#         outputs_dir.mkdir(exist_ok=True)

#         temp_file = temp_dir / unique_filename

#         # Zapisanie przesłanego pliku
#         try:
#             content = await file.read()
#             if not content:
#                 print("Error: Uploaded file is empty")
#                 return None
#             with open(temp_file, "wb") as buffer:
#                 buffer.write(content)
#         except Exception as e:
#             print(f"Error saving file: {e}")
#             return None

#         # Weryfikacja pliku
#         if not temp_file.exists() or temp_file.stat().st_size == 0:
#             print(f"Error: Temporary file {temp_file} does not exist or is empty")
#             return None

#         # Załadowanie obrazu
#         img = cv2.imread(str(temp_file))
#         if img is None:
#             print(f"Error: Failed to load image from {temp_file}")
#             temp_file.unlink(missing_ok=True)
#             return None

#         try:
#             height, width, _ = img.shape
#         except Exception as e:
#             print(f"Error accessing image shape: {e}")
#             temp_file.unlink(missing_ok=True)
#             return None

#         recognized_boxes = []

#         # Parametry
#         tile_size = 640
#         overlap = 0.2
#         conf_threshold = 0.5

#         # Tiling
#         step = int(tile_size * (1 - overlap))
#         for y in range(0, height, step):
#             for x in range(0, width, step):
#                 x_end = min(x + tile_size, width)
#                 y_end = min(y + tile_size, height)
#                 tile = img[y:y_end, x:x_end]

#                 results = model(tile, conf=conf_threshold)

#                 for result in results:
#                     for box in result.boxes:
#                         class_id = int(box.cls.item())
#                         x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
#                         x1_global, y1_global = x + x1, y + y1_
#                         x2_global, y2_global = x + x2, y + y2

#                         new_box = (x1_global, y1_global, x2_global, y2_global, class_id)
#                         print(new_box)
#                         if not await self.is_duplicate(new_box, recognized_boxes):
#                             recognized_boxes.append(new_box)

#         # Zmniejszone kopie
#         for scale_factor in [0.5, 0.25]:
#             small_img = cv2.resize(img, (0, 0), fx=scale_factor, fy=scale_factor)
#             results = model(small_img, conf=conf_threshold)

#             for result in results:
#                 for box in result.boxes:
#                     class_id = int(box.cls.item())
#                     x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
#                     x1 = int(x1 / scale_factor)
#                     y1_ = int(y1_ / scale_factor)
#                     x2 = int(x2 / scale_factor)
#                     y2 = int(y2 / scale_factor)

#                     new_box = (x1, y1_, x2, y2, class_id)
#                     if not await self.is_duplicate(new_box, recognized_boxes):
#                         recognized_boxes.append(new_box)

#         # Rysowanie boxów na obrazie
#         for box in recognized_boxes:
#             x1, y1, x2, y2, class_id = box
#             label = class_names[class_id] if class_id < len(class_names) else "Unknown"
#             color = (0, 255, 0)  # Zielony kolor
#             thickness = 2
#             cv2.rectangle(img, (x1, y1), (x2, y2), color, thickness)
#             cv2.putText(img, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

#         # Zapisanie wyjściowego obrazu
#         output_file = outputs_dir / unique_filename
#         if not cv2.imwrite(str(output_file), img):
#             print(f"Error: Failed to save output image to {output_file}")
#             temp_file.unlink(missing_ok=True)
#             return None

#         # Przygotowanie odpowiedzi
#         recognized_ids = [class_names[box[4]] if box[4] < len(class_names) else None for box in recognized_boxes]
#         unique_ids = list(set(filter(None, recognized_ids)))

#         # Wyszukiwanie RoadSign po ID
#         sign_objects = []
#         for sign_id in unique_ids:
#             sign = await self.sign_repository.get_road_sign_by_id(sign_id)
#             if sign:
#                 sign_objects.append(sign)

#         # Usunięcie tymczasowego pliku
#         temp_file.unlink(missing_ok=True)

#         # Stworzenie URL dla obrazu
#         image_url = f"/static/{unique_filename}"

#         print(f"Recognized signs: {sign_objects}")
#         print(f"Boxes: {recognized_boxes}")
#         print(f"Image URL: {image_url}")

#         return DetectionResponse(
#             signs=sign_objects,
#             total_boxes=len(recognized_boxes),
#             image_url=image_url,
#             boxes=[Box(x1=box[0], y1=box[1], x2=box[2], y2=box[3], class_id=class_names[box[4]]) for box in recognized_boxes],
#         )


from fastapi import UploadFile
from core.config_model import get_model, class_names
from core.domain.detection import DetectionResponse, Box
from infrastructure.services.idetection import IDetectionService
from core.repositories.iroadsign import IRoadSignRepository
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
import io
import uuid
import os

class DetectionService(IDetectionService):
    """A class implementing the detection service."""

    sign_repository: IRoadSignRepository

    def __init__(self, repository: IRoadSignRepository) -> None:
        """The initializer of the 'detection service'."""
        self.sign_repository = repository

    async def iou(self, box1, box2):
        x1 = max(box1[0], box2[0])
        y1 = max(box1[1], box2[1])
        x2 = min(box1[2], box2[2])
        y2 = min(box1[3], box2[3])
        
        intersection = max(0, x2 - x1) * max(0, y2 - y1)
        area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
        area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
        union = area1 + area2 - intersection
        return intersection / union if union != 0 else 0

    async def is_duplicate(self, new_box, existing_boxes, iou_threshold=0.5):
        for box in existing_boxes:
            if await self.iou(new_box[:4], box[:4]) > iou_threshold and new_box[4] == box[4]:
                return True
        return False

    async def detect_signs_from_file(self, file: UploadFile) -> DetectionResponse | None:
        model = get_model()
        # Stworzenie unikalnego imienia dla tymczasowego i wyjściowego pliku
        unique_filename = f"{uuid.uuid4()}.jpg"
        temp_dir = Path("temp")
        outputs_dir = Path("outputs")
        temp_dir.mkdir(exist_ok=True)
        outputs_dir.mkdir(exist_ok=True)

        temp_file = temp_dir / unique_filename

        # Odczyt i konwersja obrazu na JPG
        try:
            content = await file.read()
            if not content:
                print("Error: Uploaded file is empty")
                return None
            # Konwersja na JPG za pomocą Pillow
            image = Image.open(io.BytesIO(content)).convert("RGB")
            image.save(temp_file, "JPEG", quality=95)
        except Exception as e:
            print(f"Error processing image: {e}")
            return None

        # Weryfikacja pliku
        if not temp_file.exists() or temp_file.stat().st_size == 0:
            print(f"Error: Temporary file {temp_file} does not exist or is empty")
            return None

        # Załadowanie obrazu
        img = cv2.imread(str(temp_file))
        if img is None:
            print(f"Error: Failed to load image from {temp_file}")
            temp_file.unlink(missing_ok=True)
            return None

        try:
            height, width, _ = img.shape
        except Exception as e:
            print(f"Error accessing image shape: {e}")
            temp_file.unlink(missing_ok=True)
            return None

        recognized_boxes = []

        # Parametry
        tile_size = 640
        overlap = 0.2
        conf_threshold = 0.5

        # Tiling
        step = int(tile_size * (1 - overlap))
        for y in range(0, height, step):
            for x in range(0, width, step):
                x_end = min(x + tile_size, width)
                y_end = min(y + tile_size, height)
                tile = img[y:y_end, x:x_end]

                results = model(tile, conf=conf_threshold)

                for result in results:
                    for box in result.boxes:
                        class_id = int(box.cls.item())
                        confidence = box.conf.item()  # Pobranie ufności
                        x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
                        x1_global, y1_global = x + x1, y + y1_
                        x2_global, y2_global = x + x2, y + y2

                        new_box = (x1_global, y1_global, x2_global, y2_global, class_id, confidence)
                        print(f"New box: {new_box}")
                        if not await self.is_duplicate(new_box, recognized_boxes):
                            recognized_boxes.append(new_box)

        # Zmniejszone kopie
        for scale_factor in [0.5, 0.25]:
            small_img = cv2.resize(img, (0, 0), fx=scale_factor, fy=scale_factor)
            results = model(small_img, conf=conf_threshold)

            for result in results:
                for box in result.boxes:
                    class_id = int(box.cls.item())
                    confidence = box.conf.item()  # Pobranie ufności
                    x1, y1_, x2, y2 = map(int, box.xyxy[0].tolist())
                    x1 = int(x1 / scale_factor)
                    y1_ = int(y1_ / scale_factor)
                    x2 = int(x2 / scale_factor)
                    y2 = int(y2 / scale_factor)

                    new_box = (x1, y1_, x2, y2, class_id, confidence)
                    print(f"New box (scaled): {new_box}")
                    if not await self.is_duplicate(new_box, recognized_boxes):
                        recognized_boxes.append(new_box)

        # Rysowanie boxów na obrazie
        for box in recognized_boxes:
            x1, y1, x2, y2, class_id, confidence = box
            label = f"{class_names[class_id]} ({int(confidence * 100)}%)" if class_id < len(class_names) else "Unknown"
            color = (0, 255, 0)  # Zielony kolor
            thickness = 3  # Zwiększona grubość
            cv2.rectangle(img, (x1, y1), (x2, y2), color, thickness)
            cv2.putText(img, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 3)  # Większy font

        # Zapisanie wyjściowego obrazu
        output_file = outputs_dir / unique_filename
        if not cv2.imwrite(str(output_file), img):
            print(f"Error: Failed to save output image to {output_file}")
            temp_file.unlink(missing_ok=True)
            return None

        # Przygotowanie odpowiedzi
        recognized_ids = [class_names[box[4]] if box[4] < len(class_names) else None for box in recognized_boxes]
        unique_ids = list(set(filter(None, recognized_ids)))

        # Wyszukiwanie RoadSign po ID
        sign_objects = []
        for sign_id in unique_ids:
            sign = await self.sign_repository.get_road_sign_by_id(sign_id)
            if sign:
                sign_objects.append(sign)

        # Usunięcie tymczasowego pliku
        temp_file.unlink(missing_ok=True)

        # Stworzenie URL dla pliku
        file_url = f"/static/{unique_filename}"  # Zmieniono z image_url na file_url

        print(f"Recognized signs: {sign_objects}")
        print(f"Boxes: {recognized_boxes}")
        print(f"File URL: {file_url}")

        return DetectionResponse(
            signs=sign_objects,
            total_boxes=len(recognized_boxes),
            file_url=file_url,  # Zmieniono z image_url na file_url
            boxes=[Box(x1=box[0], y1=box[1], x2=box[2], y2=box[3], class_id=class_names[box[4]], confidence=box[5]) for box in recognized_boxes],
        )