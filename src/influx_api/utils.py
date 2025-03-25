import os
from uuid import uuid4, UUID
from datetime import datetime
from typing import List, Optional

import pandas as pd
from loguru import logger
from fastapi import UploadFile
from pandas import DataFrame


from influx_api.pkg import data, data_by_filename


def check_type_doc_by_filename(filename: str) -> str | UUID:
    for i in data_by_filename.items():
        if filename in i[1]:
            return i[0]
    else:
        return uuid4()


def check_well_id_by_filename(filename: str) -> str | UUID:
    for i in data.items():
        if filename in i[1]:
            return i[0]
    else:
        return uuid4()


def check_file_type(file: UploadFile):
    file_ext = file.filename.rsplit('.', 1)[-1]
    if not file.filename.endswith(('.zip', '.rar', '.csv')):
        raise Exception("Incorrect file type")
    match file_ext:
        case "csv":
            return 1
        case "zip" | 'rar':
            return 2
        case _:
            return 3


def convert_date(date: str) -> datetime:
    return datetime.strptime(date, '%d-%b-%y %H:%M:%S.%f')


def convert_csv_to_dataframe(
        storage: str,
        header_list: List[str],
) -> (List[DataFrame], List[str]):
    logger.info('Start converting csvs to dataframe')
    tmp_storage = os.walk(storage)
    df_list = []
    filenames = []
    for root, _, files in tmp_storage:
        for file in files:
            data = pd.read_csv(
                os.path.join(root, file),
                names=header_list, delimiter=',',
                engine='python'
            )
            filenames.append(check_well_id_by_filename(file.rsplit('.', 1)[0]))
            data['name_ind'] = check_type_doc_by_filename(file.rsplit('.', 1)[0])
            data['indicator'] = pd.to_numeric(data['indicator'], errors='coerce')
            data['date'] = data['date'].apply(convert_date)
            data['indicator'] = data['indicator'].astype('float64')
            df_list.append(data)
    logger.success('Finished converting csvs to dataframe')
    return df_list, filenames


def convert_tsdb_response(response: list):
    processed_data = {'g_gas_timed': [],
                      'g_gc_timed': [],
                      'g_wat_timed': []}

    for table in response:
        for record in table.records:
            record_name = record.values.get('name_ind')
            if isinstance(record.get_value(), (int, float)) and record.get_value() != 0:
                if record_name == 'Расход по газу Вентури':
                    processed_data['g_gas_timed'].append(record.get_value())
                elif record_name == 'Расход по конденсату Вентури':
                    processed_data['g_gc_timed'].append(record.get_value())
                elif record_name == 'Расход по воде Вентури':
                    processed_data['g_wat_timed'].append(record.get_value())

    return processed_data