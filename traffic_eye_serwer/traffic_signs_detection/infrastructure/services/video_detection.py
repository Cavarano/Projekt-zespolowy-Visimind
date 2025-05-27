from fastapi import UploadFile
from core.config_model import get_model, class_names
from core.domain.detection import DetectionResponse, Box
from core.repositories.iroadsign import IRoadSignRepository
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
import io
import uuid
import os
from colorsys import hsv_to_rgb

class VideoDetectionService:
    """A class implementing the video detection service."""

    sign_repository: IRoadSignRepository

    def __init__(self, repository: IRoadSignRepository) -> None:
        """Initialize the video detection service."""
        self.sign_repository = repository

    async def iou(self, box1, box2):
        """Calculate IoU between two boxes."""
        x1 = max(box1[0], box2[0])
        y1 = max(box1[1], box2[1])
        x2 = min(box1[2], box2[2])
        y2 = min(box1[3], box2[3])
        
        intersection = max(0, x2 - x1) * max(0, y2 - y1)
        area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
        area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
        union = area1 + area2 - intersection
        return intersection / union if union != 0 else 0

    async def detect_signs_from_video(self, file: UploadFile) -> DetectionResponse | None:
        """Detect traffic signs in a video and save annotated video."""

        # Check if uploaded file is a video
        if not file.content_type.startswith("video/"):
            print(f"Error: Uploaded file is not a video. Detected MIME type: {file.content_type}")
            return None

        allowed_extensions = {".mp4", ".avi", ".mov", ".mkv"}
        file_extension = Path(file.filename).suffix.lower()
        if file_extension not in allowed_extensions:
            print(f"Error: Unsupported video file extension: {file_extension}")
            return None

        model = get_model()
        unique_filename = f"{uuid.uuid4()}.mp4"
        temp_dir = Path("temp")
        outputs_dir = Path("outputs")
        temp_dir.mkdir(exist_ok=True)
        outputs_dir.mkdir(exist_ok=True)

        temp_file = temp_dir / unique_filename

        # Save uploaded video
        try:
            content = await file.read()
            if not content:
                print("Error: Uploaded video is empty")
                return None
            with open(temp_file, "wb") as f:
                f.write(content)
        except Exception as e:
            print(f"Error saving video: {e}")
            return None

        # Verify file
        if not temp_file.exists() or temp_file.stat().st_size == 0:
            print(f"Error: Temporary video {temp_file} does not exist or is empty")
            return None

        # Open video
        cap = cv2.VideoCapture(str(temp_file))
        if not cap.isOpened():
            print(f"Error: Failed to open video {temp_file}")
            temp_file.unlink(missing_ok=True)
            return None

        # Video properties
        fps = cap.get(cv2.CAP_PROP_FPS)
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

        # Output video
        output_file = outputs_dir / unique_filename
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        out = cv2.VideoWriter(str(output_file), fourcc, fps, (width, height))

        if not out.isOpened():
            print(f"Error: Failed to create output video {output_file}")
            cap.release()
            temp_file.unlink(missing_ok=True)
            return None

        # Detection parameters
        conf_threshold = 0.5
        iou_threshold = 0.5
        persistence_frames = 5  # Keep label for 5 frames after detection stops
        recognized_boxes = []
        tracked_signs = {}  # {track_id: (box, class_id, confidence, frame_count, color)}
        track_id_counter = 0

        # Generate unique colors for classes
        class_colors = {}
        for i, class_id in enumerate(range(len(class_names))):
            hue = i / len(class_names)
            rgb = hsv_to_rgb(hue, 0.7, 1.0)
            class_colors[class_id] = tuple(int(c * 255) for c in rgb)

        frame_idx = 0
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            current_time = frame_idx / fps  # Time in seconds
            frame_idx += 1

            # Detect signs
            results = model(frame, conf=conf_threshold)
            current_boxes = []

            for result in results:
                for box in result.boxes:
                    class_id = int(box.cls.item())
                    confidence = box.conf.item()
                    x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                    current_boxes.append((x1, y1, x2, y2, class_id, confidence))

            # Track signs
            new_tracked_signs = {}
            used_track_ids = set()

            for box in current_boxes:
                x1, y1, x2, y2, class_id, confidence = box
                matched = False

                # Match with existing tracks
                for track_id, (prev_box, prev_class_id, prev_conf, frame_count, color) in tracked_signs.items():
                    if class_id == prev_class_id and await self.iou((x1, y1, x2, y2), prev_box[:4]) > iou_threshold:
                        new_tracked_signs[track_id] = ((x1, y1, x2, y2, class_id, confidence), class_id, confidence, 0, color)
                        used_track_ids.add(track_id)
                        matched = True
                        break

                if not matched:
                    # New track
                    track_id_counter += 1
                    color = class_colors.get(class_id, (0, 255, 0))  # Default green if class_id out of range
                    new_tracked_signs[track_id_counter] = ((x1, y1, x2, y2, class_id, confidence), class_id, confidence, 0, color)
                    used_track_ids.add(track_id_counter)

            # Update persistence for unmatched tracks
            for track_id, (prev_box, prev_class_id, prev_conf, frame_count, color) in tracked_signs.items():
                if track_id not in used_track_ids and frame_count < persistence_frames:
                    new_tracked_signs[track_id] = (prev_box, prev_class_id, prev_conf, frame_count + 1, color)

            tracked_signs = new_tracked_signs

            # Draw boxes and labels
            for track_id, (box, class_id, confidence, frame_count, color) in tracked_signs.items():
                x1, y1, x2, y2, _, _ = box
                label = f"{class_names[class_id]} ({int(confidence * 100)}%)" if class_id < len(class_names) else "Unknown"
                thickness = 3
                cv2.rectangle(frame, (x1, y1), (x2, y2), color, thickness)
                cv2.putText(frame, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 3)

                # Store detection
                if frame_count == 0:  # Only store new or updated detections
                    recognized_boxes.append((x1, y1, x2, y2, class_id, confidence, current_time))

            out.write(frame)

        # Cleanup
        cap.release()
        out.release()
        temp_file.unlink(missing_ok=True)

        # Prepare response
        recognized_ids = [class_names[box[4]] if box[4] < len(class_names) else None for box in recognized_boxes]
        unique_ids = list(set(filter(None, recognized_ids)))

        sign_objects = []
        for sign_id in unique_ids:
            sign = await self.sign_repository.get_road_sign_by_id(sign_id)
            if sign:
                sign_objects.append(sign)

        file_url = f"/static/{unique_filename}"

        print(f"Recognized signs: {sign_objects}")
        print(f"Boxes: {recognized_boxes}")
        print(f"File URL: {file_url}")

        return DetectionResponse(
            signs=sign_objects,
            total_boxes=len(recognized_boxes),
            file_url=file_url,
            boxes=[Box(
                x1=box[0],
                y1=box[1],
                x2=box[2],
                y2=box[3],
                class_id=class_names[box[4]],
                confidence=box[5],
                time_detected=box[6]
            ) for box in recognized_boxes],
        )