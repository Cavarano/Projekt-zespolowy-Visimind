"""Module containing road sign database repository implementation"""

from typing import Any
from core.domain.roadsign import RoadSign
from core.repositories.iroadsign import IRoadSignRepository
from db import road_sign_table, database


class RoadSignRepository(IRoadSignRepository):
    """A class implementing the road sign repository."""

    async def get_road_sign_by_id(self, road_sign_id: str) -> Any | None:
        """The method getting a road sign from the data storage.

        Args:
            road_sign_id (str): The id of the road sign.

        Returns:
            Any | None: The road sign data if exists.
        """

        query = road_sign_table.select().where(road_sign_table.c.id == road_sign_id)
        road_sign = await database.fetch_one(query)

        return RoadSign(**dict(road_sign)) if road_sign else None

