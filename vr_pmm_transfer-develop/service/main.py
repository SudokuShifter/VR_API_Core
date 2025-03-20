import os

from api.routers import api_router
from fastapi import FastAPI, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, ORJSONResponse
from models.request import BaseResponse

app = FastAPI(
    title="VR",
    docs_url="/api/openapi",
    openapi_url="/api/openapi.json",
    default_response_class=ORJSONResponse,
    max_body_size=10000000,
)

app.include_router(api_router)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    messages = []
    for error in exc.errors():
        error_data = ', '.join(map(str, error['loc'][1:]))
        message = f"{error['msg']}: {error_data}"
        messages.append(message)
    response = BaseResponse(message=messages, success=False).dict()
    return JSONResponse(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                        content=response)
