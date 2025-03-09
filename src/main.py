from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from containers.config_containers import ConfigContainer

@asynccontextmanager
async def lifespan(_application: FastAPI) -> AsyncGenerator:
    config_container = ConfigContainer()
    config_container.wire(packages=[__name__, 'influx_service', 'path_config'])
    yield

app = FastAPI(lifespan=lifespan)
app.include_router(...)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


if __name__ == '__main__':
    uvicorn.run(app, host='127.0.0.1', port=8000)