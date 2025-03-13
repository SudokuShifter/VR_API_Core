from datetime import datetime

from starlette.background import BackgroundTask
from starlette import status
from fastapi.responses import JSONResponse
from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Query,
)

from csv_loader.utils import check_file_type
from csv_loader.schemas import (
    RequestDataWithDateRangeSchema,
    RequestDataWithIDSchema,
)
from csv_loader.dependencies import (
    CSVService,
    InfluxDBService,
    InfluxDBRequestManager
)


router = APIRouter()


@router.post("/fill_influx_archive_or_single_csv",
             status_code=status.HTTP_200_OK,
             summary="Загрузить одиночный csv-файл или архив csv-файлов в InfluxDB"
             )
async def fill_influx(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file_or_archive: UploadFile = File(..., description="CSV file or archive"),
):
    point = check_file_type(file_or_archive)
    csv_service.save_file(file_or_archive)
    tasks = BackgroundTask(influx_service.fill_data, point, file_or_archive)
    return JSONResponse({'status': 'in progress'},200, background=tasks)


@router.get("/get_data_by_uuid",
            status_code=status.HTTP_200_OK,
            summary="Получить всю информацию по UUID индикатора"
            )
async def get_data_by_uuid(
        influx_request_manager: InfluxDBRequestManager,
        uuid: str = Query(..., description='UUID индикатора'),
):
    return await influx_request_manager.get_full_data_by_id(uuid)


@router.get("/get_data_by_uuid_and_range",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по UUID индикатора и диапазону дат"
            )
async def get_data_by_uuid_and_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        uuid: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_by_range(date_start_str, date_end_str, uuid)


@router.get("/get_data_by_uuid_after_date",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по UUID индикатора после "
                    "определённой даты до последнего значения"
            )
async def get_data_after_date(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        uuid: str = Query(...),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_after_date(date_start_str, uuid)


@router.get("/get_data_by_uuid_before_date",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по UUID индикатора до "
                    "определённой даты с первого значения"
            )
async def get_data_before_date(
        influx_request_manager: InfluxDBRequestManager,
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        uuid: str = Query(...),
):
    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_before_date(date_end_str, uuid)