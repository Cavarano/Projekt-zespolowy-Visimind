"""Module containing detection related domain models"""

from pydantic import BaseModel, ConfigDict
from typing import List
from core.domain.roadsign import RoadSign

class Box(BaseModel):
    x1: int
    y1: int
    x2: int
    y2: int
    class_id: str

class DetectionResponse(BaseModel):
    """Model representing detection DTO attributes."""
    signs: List[RoadSign]
    total_boxes: int
    image_url: str
    boxes: List[Box]