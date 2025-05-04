"""Module containing road sing related domain models"""

from pydantic import BaseModel, ConfigDict


class RoadSignIn(BaseModel):
    """Model representing road sing's DTO attributes."""
    id: str
    name: str
    description: str
    photo_url: str


class RoadSign(RoadSignIn):
    """Model representing road sing's attributes in the database."""
    id: str

    model_config = ConfigDict(from_attributes=True, extra="ignore")
