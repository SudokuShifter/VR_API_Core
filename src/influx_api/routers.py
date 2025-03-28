from datetime import datetime, timedelta

from starlette.background import BackgroundTask
from starlette import status
from fastapi.responses import JSONResponse
from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Query,
)

from influx_api.utils import check_file_type
from influx_api.schemas import (
    RequestDataWithDateRangeSchema,
    RequestDataWithIDSchema,
)
from influx_api.dependencies import (
    CSVService,
    InfluxDBService,
    InfluxDBRequestManager
)
from influx_api.utils import (
    convert_tsdb_validate_response,
    convert_tsdb_adapt_response,
    convert_tsdb_fmm_response,
    convert_tsdb_ml_response,
    convert_tsdb_ml_time_point_response
)


router = APIRouter()


@router.post("/fill_influx_archive_or_single_csv",
             status_code=status.HTTP_200_OK,
             summary="Загрузить одиночный csv-файл или архив csv-файлов в InfluxDB"
             )
async def fill_influx(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file_or_archive: UploadFile = File(..., description="CSV file or archive (zip/rar)")
):
    point = check_file_type(file_or_archive)
    csv_service.save_file(file_or_archive)
    tasks = BackgroundTask(influx_service.fill_data, point, file_or_archive)
    return JSONResponse({'status': 'in progress'},200, background=tasks)


@router.get('/get_data_for_validate_by_range',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для валидации за диапазон времени'
            )
async def get_data_for_validate_by_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели'),
):
    date_start = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_validate_by_range(
        date_start, date_end, well_id
    )
    return convert_tsdb_validate_response(data)


@router.get('/get_data_for_validate_by_time_point',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для валидации за метку времени'
            )
async def get_data_for_validate_by_time_point(
        influx_request_manager: InfluxDBRequestManager,
        date: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели'),
):
    date_start = date.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end = (date + timedelta(days=1)).strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_validate_by_range(
        date_start, date_end, well_id
    )
    return convert_tsdb_validate_response(data)


@router.get('/get_data_for_adapt_by_range',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для адаптации за диапазон времени'
            )
async def get_data_for_adapt_by_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели')
):
    date_start = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_adapt(
        date_start, date_end, well_id
    )
    return convert_tsdb_adapt_response(data)


@router.get('/get_data_for_fmm_by_time_point',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для fmm-расчета за метку времени'
            )
async def get_data_for_fmm_by_time_point(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели')
):
    date_end = (date_start + timedelta(minutes=30)).strftime('%Y-%m-%dT%H:%M:%SZ')
    date_start = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_fmm_by_time_point(
        date_start, date_end, well_id
    )
    return convert_tsdb_fmm_response(data)


@router.get('/get_data_for_ml_by_range',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для ml-обработки за диапазон дат')
async def get_data_for_fmm_by_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели')
):
    date_start = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_ml_by_range(
        date_start, date_end, well_id
    )
    return convert_tsdb_ml_response(data)


@router.get('/get_data_for_ml_by_time_point',
            status_code=status.HTTP_200_OK,
            summary='Получить данные для ml-обработки за метку времени')
async def get_data_for_fmm_by_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        well_id: str = Query(..., description='ID модели'),
):
    date_end = (date_start + timedelta(days=1)).strftime('%Y-%m-%dT%H:%M:%SZ')
    date_start = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    data = await influx_request_manager.get_data_for_ml_by_time_point(
        date_start, date_end, well_id
    )

    return convert_tsdb_ml_time_point_response(data)