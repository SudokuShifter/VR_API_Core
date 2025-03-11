from datetime import datetime

from fastapi import APIRouter, UploadFile, File, Query

from csv_loader.schemas import (
    RequestDataWithDateRangeSchema,
    RequestDataWithIDSchema,
)
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
        file: UploadFile = File(...),
):
    await csv_service.unpack_files_from_archive(file)
    return await influx_service.fill_data()


@router.get("/get_data_by_ind_id")
async def get_data_by_ind_id(
        influx_service: InfluxDBService,
        ind_id: str = Query(...),
):

    return await influx_service.get_full_data_by_id(ind_id)


@router.get("/get_data_by_id_and_range")
async def get_data_by_id_and_range(
        influx_service: InfluxDBService,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_service.get_data_by_range(date_start_str, date_end_str, ind_id)


@router.get("/get_data_by_id_before_date")
async def get_data_after_date(
        influx_service: InfluxDBService,
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_start_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_service.get_data_after_date(date_end, ind_id)


@router.get("/get_data_by_id_after_date")
async def get_data_after_date(
        influx_service: InfluxDBService,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_service.get_data_after_date(date_start_str, ind_id)