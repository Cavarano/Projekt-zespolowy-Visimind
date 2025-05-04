"""A module containing road sign endpoints """

from dependency_injector.wiring import inject, Provide
from fastapi import APIRouter, Depends, HTTPException

from container import Container
from core.domain.roadsign import RoadSign
from infrastructure.services.iroadsign import IRoadSignService

router = APIRouter()

@router.get("/{road_sign_id}", response_model=RoadSign, status_code=200)
@inject
async def get_road_sign_by_id(
        road_sign_id: str,
        service: IRoadSignService = Depends(Provide[Container.road_sign_service]),
) -> RoadSign:
    """An endpoint for getting road sign details by id.

    Args:
        road_sign_id (str): The road sign id.
        service (IRoadSignService): The injected service dependency.

    Raises:
        HTTPException: 404 if the road sign id is not found.

    Returns:
        RoadSign: The requested road sign.
    """

    road_sign = await service.get_road_sign_by_id(road_sign_id)
    
    if road_sign:
        return road_sign

    raise HTTPException(status_code=404, detail="Road sign not found.")

