from fastapi import APIRouter

from api.v1.adapt import adapt_router
from api.v1.fmm import fmm_router
from api.v1.validate import validate_router

api_v1_router = APIRouter()

api_v1_router.include_router(fmm_router, prefix="/fmm", tags=["fmm"])
api_v1_router.include_router(adapt_router, prefix="/adapt", tags=["adapt"])
api_v1_router.include_router(validate_router, prefix="/validate", tags=["validate"])
