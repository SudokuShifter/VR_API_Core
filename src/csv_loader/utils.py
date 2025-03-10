import os

from fastapi_storages import FileSystemStorage

import pandas as pd
from pandas import DataFrame


async def read_csv(
        storage: str
) -> DataFrame:
    tmp_storage = os.walk(storage)
    for file in tmp_storage:
        print(file)
        # data = pd.read_csv(file, header=None, delimiter=',', engine='python')
        # print(data)