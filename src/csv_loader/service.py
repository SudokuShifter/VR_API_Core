from fastapi import File
from fastapi.exceptions import HTTPException

import pandas as pd
from typing import Any, Union


from csv_loader.exceptions import CSVLoadException
from csv_loader.dependencies import storage


class CSVService:

    @staticmethod
    async def csv_loader(path_file: str) -> Union[str, CSVLoadException]:
        pass



class InfluxDBService:

    @staticmethod
    async def fill_data(data) -> Union[str, CSVLoadException]:
        pass