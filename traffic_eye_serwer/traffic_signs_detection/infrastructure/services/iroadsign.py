"""Module containing road sign service abstractions."""

from abc import ABC, abstractmethod

from core.domain.roadsign import RoadSign


class IRoadSignService(ABC):
    """An abstract class representing protocol of road sign repository."""

    @abstractmethod
    async def get_road_sign_by_id(self, road_sign_id: str) -> RoadSign | None:
        """The abstract getting a road sign from the repository.

        Args:
            road_sign_id: The id of the road sign to get.

        Returns:
            RoadSign | None: THe road sign if exists.
        """
