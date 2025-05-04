"""Module containing detection service abstractions."""

from fastapi import UploadFile
from abc import ABC, abstractmethod

from core.domain.detection import DetectionResponse


class IDetectionService(ABC):
    """An abstract class representing protocol of detection repository."""

    @abstractmethod
    async def detect_signs_from_file(self, file: UploadFile) -> DetectionResponse | None:
        """The abstract getting a detection from the repository.

        Args:
            file: The file uploaded by user.

        Returns:
            DetectionResponse | None: The detection if exists.
        """
