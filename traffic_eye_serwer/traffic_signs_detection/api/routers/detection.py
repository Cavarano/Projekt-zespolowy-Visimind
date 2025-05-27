from dependency_injector.wiring import inject, Provide
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException
from container import Container
from infrastructure.services.idetection import IDetectionService
from infrastructure.services.ivideo_detection import IVideoDetectionService
from core.domain.detection import DetectionResponse
import logging

router = APIRouter()

@router.post("/detect-signs/", response_model=DetectionResponse, status_code=201)
@inject
async def detect_sign(
    file: UploadFile = File(...),
    service: IDetectionService = Depends(Provide[Container.detection_service]),
) -> DetectionResponse:
    logging.info(f"Received file: {file.filename}")
    return await service.detect_signs_from_file(file)


@router.post("/detect-signs-video/", response_model=DetectionResponse, status_code=201)
@inject
async def detect_signs_video(
    file: UploadFile = File(...),
    video_service: IVideoDetectionService = Depends(Provide[Container.video_detection_service])
) -> DetectionResponse:
    """Detect traffic signs from an uploaded video file and return annotated video."""
    result = await video_service.detect_signs_from_video(file)
    if result is None:
        return DetectionResponse(signs=[], total_boxes=0, image_url="", boxes=[])
    return result