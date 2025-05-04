from dependency_injector.wiring import inject, Provide
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException
from container import Container
from infrastructure.services.idetection import IDetectionService
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