from fastapi import File
from fastapi.exceptions import HTTPException
from typing import Any, Union

from csv_loader.exceptions import CSVLoadException


class CSVLoaderService:

    @staticmethod
    async def csv_dump(path_file: str) -> Union[str, CSVLoadException]:
        pass



class InfluxDBService:

    @staticmethod
    async def fill_data(data) -> Union[str, CSVLoadException]:
        pass