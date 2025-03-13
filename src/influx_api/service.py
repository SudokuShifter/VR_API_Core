import datetime
import os
from pathlib import Path
import shutil
from typing import Tuple

import zipfile
from loguru import logger
import patoolib
from dependency_injector.wiring import Provide
from fastapi import UploadFile
from fastapi.responses import JSONResponse
from fastapi_storages import FileSystemStorage
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS

from containers.config_containers import (
    ConfigContainer,
    RequestContainer
)
from influx_api.config import InfluxDBConfig

from influx_api.utils import (
    convert_csv_to_dataframe,
    convert_date
)


class CoreResponse:
    """
    Core-класс для создания метода генерации ответа от сервера для reg_auth роутера
    """
    @staticmethod
    def make_response(
            success: bool,
            detail: str,
            status_code: int
    ) -> JSONResponse:

        return JSONResponse(
            {"success": success, "detail": detail},
            status_code=status_code
        )


class CSVService(CoreResponse):
    """
    Service-класс для реализации основного функционала для работы с csv-файлами:
    - Загрузка csv-файлов во внутреннее хранилище проекта
    - Очищение загруженных файлов
    - Распаковка архивов с csv-файлами
    """

    def __init__(self, storage: FileSystemStorage):
        self.storage = storage
        self.storage_path = self.storage._path


    def clear_folder_and_create(
            self
    ):

        shutil.rmtree(self.storage_path, ignore_errors=True)
        os.mkdir(self.storage_path)


    def tmp_file_data(
            self,
            file: UploadFile
    ) -> Tuple[str, Path]:

        ext = file.filename.split('.')[-1]
        return ext, Path(f'{self.storage_path}/temp.{ext}')


    def save_file(
            self,
            file: UploadFile,
    ):

        if not file.filename.endswith(('.zip', '.rar', '.csv')):
            return self.make_response(
                success=False,
                detail='Incorrect file type',
                status_code=400)

        self.clear_folder_and_create()
        ext, destination = self.tmp_file_data(file)

        with open(destination, 'wb') as buffer:
            shutil.copyfileobj(file.file, buffer)


    def unpack_files_from_archive(
            self,
            file: UploadFile
    ) -> JSONResponse:

        ext, destination = self.tmp_file_data(file)
        if ext == 'zip':
            return self.unpack_zip_folder_with_csvs(destination)
        else:
            return self.unpack_rar_folder_with_csvs(destination)


    def unpack_zip_folder_with_csvs(
            self,
            temp_path: Path
    ) -> JSONResponse:

        with zipfile.ZipFile(temp_path, 'r') as zip_file:
            zip_file.extractall(self.storage_path)

        temp_path.unlink()
        return self.make_response(
            success=True,
            detail='Files successfully extracted',
            status_code=201
        )


    def unpack_rar_folder_with_csvs(
            self,
            temp_path: Path
    ) -> JSONResponse:

        str_temp_path = str(temp_path)
        patoolib.extract_archive(archive=str_temp_path, outdir=self.storage_path)
        os.remove(str_temp_path)
        return self.make_response(
            success=True,
            detail='Files successfully extracted',
            status_code=201
        )


class InfluxDBService(CoreResponse):
    """
    Service-класс для реализации основного функционала для работы с базой данных InfluxDB:
    - Загрузка данных
    - Получение данных
    """
    HEADER_LIST = ['date', 'indicator']

    def __init__(
            self,
            storage: FileSystemStorage,
            config: InfluxDBConfig = Provide[ConfigContainer.influxdb_config],
            request_manager: RequestContainer = Provide[RequestContainer.request_manager],
    ):
        self.storage_path = storage._path
        self.config = config
        self.client = InfluxDBClient(url=config.DB_URL, org=config.DB_ORG,
                                     token=config.DB_TOKEN, bucket=config.DB_BUCKET_NAME)
        self.csv_service = CSVService(storage)
        self.request_manager = request_manager
        self.query_api = self.client.query_api()
        self.write_api = self.client.write_api(write_options=SYNCHRONOUS)


    def fill_data(
            self,
            point: int,
            file: UploadFile,
    ) -> JSONResponse:
        logger.info('Start filling data in influxdb')
        if point == 2:
            self.csv_service.unpack_files_from_archive(file)

        df_list = convert_csv_to_dataframe(storage=self.storage_path,
                                 header_list=self.HEADER_LIST)
        for df in df_list:
            df.set_index('date', inplace=True)
            chunk_size = 10000
            for i in range(0, len(df), chunk_size):
                chunk = df.iloc[i:i + chunk_size]
                self.write_api.write(
                    bucket=self.config.DB_BUCKET_NAME,
                    record=chunk,
                    data_frame_measurement_name='indicator',
                    data_frame_tag_columns=['ind_tag']
                )
        logger.success('Finished filling data in influxdb')
        return self.make_response(
            success=True,
            detail='Data successfully filled',
            status_code=201
        )


class InfluxDBRequestManager(InfluxDBService):
    """
    Manager-класс для реализации функционала для работы данными внутри базы данных InfluxDB:
    - Получение данных
    - Запись (запросом)
    """
    async def get_full_data_by_id(
            self,
            ind_tag: str
    ):
        result = self.query_api.query(
            self.request_manager.FULL_DATA_BY_TAG.format(
                ind_tag
        ))
        return self.make_response(
            success=True,
            detail=str(result),
            status_code=200
        )


    async def get_data_by_range(
            self,
            date_start: datetime.datetime,
            date_end: datetime.datetime,
            ind_tag: str,
    ):
        result = self.query_api.query(
            self.request_manager.DATA_FOR_RANGE_BY_TAG.format(
                date_start,
                date_end,
                ind_tag
        ))
        return self.make_response(
            success=True,
            detail=str(result),
            status_code=200
        )


    async def get_data_before_date(
            self,
            date_end: datetime.datetime,
            ind_tag: str,
    ):
        result = self.query_api.query(
            self.request_manager.DATA_BEFORE_DATE.format(
                date_end,
                ind_tag
        ))
        return self.make_response(
            success=True,
            detail=str(result),
            status_code=200
        )


    async def get_data_after_date(
            self,
            date_start: datetime.datetime,
            ind_tag: str,
    ):
        result = self.query_api.query(
            self.request_manager.DATA_AFTER_DATE.format(
                date_start,
                ind_tag
        ))
        return self.make_response(
            success=True,
            detail=str(result),
            status_code=200
        )