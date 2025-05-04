"""Module containing road sign repository implementation."""

from core.domain.roadsign import RoadSign
from core.repositories.iroadsign import IRoadSignRepository
from infrastructure.repositories.db import road_signs


class RoadSignMockRepository(IRoadSignRepository):
    """A class implementing road sign repository."""

    async def get_road_sign_by_id(self, road_sign_id: str) -> RoadSign | None:
        """The method getting a road sign from the data storage.

        Args:
            road_sign_id: The id of the road sign.

        Returns:
            RoadSign | None: The road sign data if exists.
        """
        return next(
            (obj for obj in road_signs if obj.id == road_sign_id),
            None,
        )

