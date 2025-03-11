import os
from pathlib import Path
import shutil
import asyncio

import zipfile
import patoolib
from dependency_injector.wiring import Provide
from fastapi import UploadFile
from fastapi.responses import JSONResponse
from fastapi_storages import FileSystemStorage
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS

from containers.config_containers import ConfigContainer
from csv_loader.config import InfluxDBConfig
from csv_loader.utils import read_csv


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
    def __init__(
            self,
            storage: FileSystemStorage
    ):
        self.storage = storage
        self.storage_path = self.storage._path


    async def clear_folder_and_create(self):
        shutil.rmtree(self.storage_path, ignore_errors=True)
        os.mkdir(self.storage_path)


    async def csv_loader(
            self,
            file: UploadFile,
    ) -> JSONResponse:

        if not file.filename.endswith('.csv'):
            return self.make_response(
                success=False,
                detail='Incorrect file type',
                status_code=400)

        try:
            await self.clear_folder_and_create()
            self.storage.write(file.file, name=file.filename)
            return self.make_response(
                success=True,
                detail='File successfully saved',
                status_code=201
            )

        except Exception as e:
            return self.make_response(
                success=False,
                detail=str(e),
                status_code=400)


    async def unpack_files_from_archive(
            self,
            file: UploadFile
    ) -> JSONResponse:
        if not file.filename.endswith(('.zip', '.rar')):
            return self.make_response(
                success=False,
                detail='Incorrect file type',
                status_code=400)

        await self.clear_folder_and_create()
        ext = file.filename.split('.')[-1]
        self.storage.write(file.file, name=f'temp.{ext}')

        temp_path = Path(f'{self.storage_path}/temp.{ext}')
        if ext == 'zip':
            return await self.unpack_zip_folder_with_csvs(temp_path)
        else:
            return await self.unpack_rar_folder_with_csvs(temp_path)


    async def unpack_zip_folder_with_csvs(
            self,
            temp_path: Path
    ) -> JSONResponse:

        with zipfile.ZipFile(temp_path, 'r') as zip_file:
            zip_file.extractall(self.storage_path)

        temp_path.unlink()
        return self.make_response(
            success=True,
            detail='files successfully extracted',
            status_code=201
        )


    async def unpack_rar_folder_with_csvs(
            self,
            temp_path: Path
    ) -> JSONResponse:
        str_temp_path = str(temp_path)
        patoolib.extract_archive(archive=str_temp_path, outdir=self.storage_path)
        os.remove(str_temp_path)
        return self.make_response(
            success=True,
            detail='files successfully extracted',
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
            config: InfluxDBConfig = Provide[ConfigContainer.influxdb_config]
    ):
        self.storage_path = storage._path
        self.client = InfluxDBClient(url=config.DB_URL, org=config.DB_ORG, token=config.DB_TOKEN)
        self.query_api = self.client.query_api()
        self.write_api = self.client.write_api(write_options=SYNCHRONOUS)


    async def fill_data(
            self,
    ) -> JSONResponse:
        df_list = await read_csv(storage=self.storage_path,
                                 header_list=self.HEADER_LIST)
        for df in df_list:
            print(df)
            df.set_index('date', inplace=True)
            try:
                self.write_api.write(bucket='test', record=df,
                                     data_frame_measurement_name='indicator')
                await asyncio.sleep(0.1)
            except Exception as e:
                print(e)
        return self.make_response(
            success=True,
            detail='Data successfully filled',
            status_code=201
        )

    async def get_data(self):
        result = self.query_api.query(f'from(bucket: "test/autogen") |> range(start: -3y)')

