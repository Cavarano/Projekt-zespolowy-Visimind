"""Module containing road sign repository abstractions."""

from abc import ABC, abstractmethod
from typing import Any


class IRoadSignRepository(ABC):
    """An abstract class representing protocol of animal repository."""

    @abstractmethod
    async def get_road_sign_by_id(self, road_sign_id: str) -> Any | None:
        """The abstract getting a road sign from the data storage.

        Args:
            road_sign_id: The id of the road sign.

        Returns:
            Any | None: the road sign data if exists.

        """
