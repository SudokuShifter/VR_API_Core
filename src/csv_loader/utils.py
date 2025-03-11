import os
from uuid import uuid4, UUID
from datetime import datetime
from typing import List, Optional

import pandas as pd
from pandas import DataFrame


def convert_date(date: str) -> datetime:
    return datetime.strptime(date, '%d-%b-%y %H:%M:%S.%f')


async def convert_csv_to_dataframe(
        storage: str,
        header_list: List[str],
) -> List[DataFrame]:
    start = datetime.now()
    tmp_storage = os.walk(storage)
    df_list = []
    for root, _, files in tmp_storage:
        for file in files:
            data = pd.read_csv(
                os.path.join(root, file),
                names=header_list, delimiter=',',
                engine='python'
            )
            data['ind_tag'] = uuid4()
            data['indicator'] = pd.to_numeric(data['indicator'], errors='coerce')
            data['date'] = data['date'].apply(convert_date)
            data['indicator'] = data['indicator'].astype('float64')
            df_list.append(data)
    print(datetime.now() - start)
    return df_list
