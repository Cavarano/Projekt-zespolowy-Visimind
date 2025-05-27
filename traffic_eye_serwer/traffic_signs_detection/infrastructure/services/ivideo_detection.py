"""Module containing detection service abstractions."""

from fastapi import UploadFile
from abc import ABC, abstractmethod

from core.domain.detection import DetectionResponse


class IVideoDetectionService(ABC):
    """An abstract class representing protocol of video detection service."""

    @abstractmethod
    async def detect_signs_from_video(self, file: UploadFile) -> DetectionResponse | None:
        """The abstract getting a detection from the video detection service.

        Args:
            file: The file uploaded by user.

        Returns:
            DetectionResponse | None: The detection if exists.
        """