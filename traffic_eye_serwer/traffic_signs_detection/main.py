"""Main module of the app"""

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.exception_handlers import http_exception_handler
from api.routers.roadsign import router as road_sign_router
from api.routers.detection import router as detection_router
from container import Container
from db import database, init_db


container = Container()
container.wire(modules=[
    "api.routers.roadsign",
    "api.routers.detection"
])

@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncGenerator:
    """Lifespan function working on app startup."""
    await init_db()
    await database.connect()
    yield
    await database.disconnect()

# FastAPI app
app = FastAPI(lifespan=lifespan)

app.include_router(road_sign_router, prefix="/roadsign")
app.include_router(detection_router, prefix="/detection")

@app.exception_handler(HTTPException)
async def http_exception_handle_logging(
    request: Request,
    exception: HTTPException,
) -> Response:
    return await http_exception_handler(request, exception)

