import os
from datetime import datetime
from typing import List

from fastapi_storages import FileSystemStorage

import pandas as pd
from pandas import DataFrame


def convert_date(date: str) -> datetime:
    return datetime.strptime(date, '%d-%b-%y %H:%M:%S.%f')


async def read_csv(
        storage: str,
        header_list: List[str],
) -> List[DataFrame]:
    tmp_storage = os.walk(storage)
    df_list = []
    for root, _, files in tmp_storage:
        for file in files:
            data = pd.read_csv(
                os.path.join(root, file),
                names=header_list, delimiter=',',
                engine='python'
            )
            data['date'] = data['date'].apply(convert_date)
            df_list.append(data)
    return df_list
