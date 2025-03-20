from fastapi import APIRouter

from api.v1.routers import api_v1_router

api_router = APIRouter()

api_router.include_router(api_v1_router, prefix="/v1")
