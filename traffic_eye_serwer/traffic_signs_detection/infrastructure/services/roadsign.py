"""Module containing road sign service implementation."""

from core.domain.roadsign import RoadSign, RoadSignIn
from core.repositories.iroadsign import IRoadSignRepository
from infrastructure.services.iroadsign import IRoadSignService


class RoadSignService(IRoadSignService):
    """A class implementing the road sign service."""

    _repository: IRoadSignRepository

    def __init__(self, repository: IRoadSignRepository) -> None:
        """The initializer of the 'road sign service'.

        Args:
            repository (IRoadSignRepository): the reference to the repository.
        """

        self._repository = repository

    async def get_road_sign_by_id(self, road_sign_id: str) -> RoadSign | None:
        """The method getting a road sign from the data storage.

        Args:
            road_sign_id (str): the id of the road sign.

        Returns:
            RoadSign | None: Road sign if found.
        """
        return await self._repository.get_road_sign_by_id(road_sign_id)
