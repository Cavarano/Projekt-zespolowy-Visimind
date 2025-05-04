"""A module containing DTO models for animal."""

from pydantic import BaseModel, ConfigDict


class RoadSignDTO(BaseModel):
    """A model representing DTO for road sign data."""
    id: str
    name: str
    description: str
    photo_url: str

    model_config = ConfigDict(
        from_attributes=True,
        extra="ignore",
        arbitrary_types_allowed=True,
    )

