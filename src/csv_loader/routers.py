from fastapi import APIRouter, UploadFile, File
from fastapi.exceptions import HTTPException
from typing import List

from csv_loader.dependencies import CSVService, InfluxDBService


router = APIRouter()


@router.post("/fill_influx_single_csv")
async def csv_load_single(
        csv_service: CSVService,
        file: UploadFile = File(...)
):
    return await csv_service.csv_loader(file)


@router.post("/fill_influx_folder_csv")
async def csv_load_archive(
        csv_service: CSVService,
        file: UploadFile = File(...)
):
    return await csv_service.unpack_files_from_archive(file)


@router.get("/get_data_for_id")
async def get_data(_id: int):
    pass