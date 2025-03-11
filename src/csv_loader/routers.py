from fastapi import APIRouter, UploadFile, File

from csv_loader.dependencies import (
    CSVService,
    InfluxDBService
)


router = APIRouter()


@router.post("/fill_influx_single_csv")
async def csv_load_single(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file: UploadFile = File(..., description="CSV file"),
):
    await csv_service.csv_loader(file)
    return await influx_service.fill_data()


@router.post("/fill_influx_archive_csv", description="CSV file")
async def csv_load_archive(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file: UploadFile = File(...)
):
    await csv_service.unpack_files_from_archive(file)
    return await influx_service.fill_data()


@router.get("/get_data_for_id")
async def get_data(
        influx_service: InfluxDBService,
):
    return await influx_service.get_data()
