"""Module providing containers injecting dependencies."""

from dependency_injector.containers import DeclarativeContainer
from dependency_injector.providers import Factory, Singleton

from infrastructure.repositories.roadsigndb import RoadSignRepository
from infrastructure.services.roadsign import RoadSignService
from infrastructure.services.detection import DetectionService


class Container(DeclarativeContainer):
    """Container class for dependency injecting purposes."""
    road_sign_repository = Singleton(RoadSignRepository)

    road_sign_service = Factory(
        RoadSignService,
        repository=road_sign_repository,
    )

    detection_service = Factory(
        DetectionService,
        repository=road_sign_repository,
    )