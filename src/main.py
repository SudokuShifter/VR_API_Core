from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from containers.config_containers import ConfigContainer
from csv_loader.routers import router


@asynccontextmanager
async def lifespan(_application: FastAPI) -> AsyncGenerator:
    config_container = ConfigContainer()
    config_container.wire(packages=[__name__, 'csv_loader'])
    yield

app = FastAPI(lifespan=lifespan)
app.include_router(router, prefix='/api')


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/healthcheck", summary="Проверка работы приложения")
async def healthcheck() -> dict[str, str]:
    return {"status": "ok"}


if __name__ == '__main__':
    uvicorn.run(app, host='127.0.0.1', port=8000)