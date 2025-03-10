from typing import Annotated

from fastapi_storages import FileSystemStorage
from fastapi import Depends

from csv_loader.service import CSVService, InfluxDBService


storage = FileSystemStorage(path='./tmp')


async def get_scv_service() -> CSVService:
    return CSVService(storage=storage)


async def get_infludb_service() -> InfluxDBService:
    return InfluxDBService()


CSVService = Annotated[CSVService, Depends(get_scv_service)]
InfluxDBService = Annotated[InfluxDBService, Depends(get_infludb_service)]

