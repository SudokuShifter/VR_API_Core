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


router = APIRouter()


@router.post("/fill_influx_archive_or_single_csv",
             status_code=status.HTTP_200_OK,
             summary="Загрузить одиночный csv-файл или архив csv-файлов в InfluxDB"
             )
async def fill_influx(
        csv_service: CSVService,
        influx_service: InfluxDBService,
        file_or_archive: UploadFile = File(..., description="CSV file or archive (zip/rar)"),
        model_id: int = Query(..., description='ID модели'),
        object_id: int = Query(..., description='ID объекта')
):
    point = check_file_type(file_or_archive)
    csv_service.save_file(file_or_archive)
    tasks = BackgroundTask(influx_service.fill_data, point, file_or_archive, model_id, object_id)
    return JSONResponse({'status': 'in progress'},200, background=tasks)


@router.get("/get_data_by_uuid_and_range",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по model_id, object_id и object_tag и диапазону дат"
            )
async def get_data_by_uuid_and_range(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        model_id: str = Query(..., description='ID модели'),
        object_id: str = Query(..., description='ID объекта'),
        object_tag: str = Query(..., description='Tag объекта'),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_by_range(
        date_start_str, date_end_str, model_id, object_id, object_tag
    )


@router.get("/get_data_by_uuid_after_date",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по model_id, object_id и object_tag после "
                    "определённой даты до последнего значения"
            )
async def get_data_after_date(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        model_id: str = Query(..., description='ID модели'),
        object_id: str = Query(..., description='ID объекта'),
        object_tag: str = Query(..., description='Tag объекта'),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_after_date(
        date_start_str, model_id, object_id, object_tag
    )


@router.get("/get_data_by_uuid_before_date",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по model_id, object_id и object_tag до "
                    "определённой даты с первого значения"
            )
async def get_data_before_date(
        influx_request_manager: InfluxDBRequestManager,
        date_end: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        model_id: str = Query(..., description='ID модели'),
        object_id: str = Query(..., description='ID объекта'),
        object_tag: str = Query(..., description='Tag объекта'),
):

    date_end_str = date_end.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_before_date(
        date_end_str, model_id, object_id, object_tag
    )


@router.get("/get_data_by_uuid_and_day",
            status_code=status.HTTP_200_OK,
            summary="Получить информацию по model_id, object_id и object_tag для конкретного дня"
            )
async def get_data_by_uuid_and_day(
        influx_request_manager: InfluxDBRequestManager,
        date_start: datetime = Query(..., description="2021-01-01T00:00:00Z"),
        model_id: str = Query(..., description='ID модели'),
        object_id: str = Query(..., description='ID объекта'),
        object_tag: str = Query(..., description='Tag объекта'),
):
    date_start_str = date_start.strftime('%Y-%m-%dT%H:%M:%SZ')
    return await influx_request_manager.get_data_by_day(
        date_start_str, model_id, object_id, object_tag
    )


@router.get("/get_objects_by_model_id",
            status_code=status.HTTP_200_OK,
            summary="Получить все вложенные объекты модели"
            )
async def get_objects_by_model_id(
        influx_request_manager: InfluxDBRequestManager,
        model_id: str = Query(..., description='ID модели')
):
    return await influx_request_manager.get_all_objects_by_model_id(model_id)