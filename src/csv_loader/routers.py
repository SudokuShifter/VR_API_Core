from datetime import datetime

from starlette.background import BackgroundTask
from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Query,
)
from fastapi.responses import JSONResponse

from csv_loader.schemas import (
    RequestDataWithDateRangeSchema,
    RequestDataWithIDSchema,
)
from csv_loader.dependencies import (
    CSVService,
    InfluxDBService,
    InfluxDBRequestManager
)
from csv_loader.utils import check_file_type


router = APIRouter()


@router.post("/fill_influx_archive_or_single_csv")
async def csv_load_single(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file: UploadFile = File(..., description="CSV file"),
):
    point = check_file_type(file)
    csv_service.save_file(file)
    tasks = BackgroundTask(influx_service.fill_data, point, file)
    return JSONResponse({'status': 'in progress'}, background=tasks)


@router.get("/get_data_by_ind_id")
async def get_data_by_ind_id(
        influx_request_manager: InfluxDBRequestManager,
        ind_id: str = Query(...),
):
    return await influx_request_manager.get_full_data_by_id(ind_id)


@router.get("/get_data_by_id_and_range")
async def get_data_by_id_and_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_by_range(date_start_str, date_end_str, ind_id)


@router.get("/get_data_by_id_before_date")
async def get_data_after_date(
        influx_request_manager: InfluxDBRequestManager,
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_end = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_after_date(date_end, ind_id)


@router.get("/get_data_by_id_after_date")
async def get_data_after_date(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        ind_id: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_after_date(date_start_str, ind_id)