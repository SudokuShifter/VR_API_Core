import os
from uuid import uuid4, UUID
from datetime import datetime
from typing import List, Optional

import pandas as pd
from loguru import logger
from fastapi import UploadFile
from pandas import DataFrame


from influx_api.pkg.pkg import DATA, DATA_BY_FILENAME


def check_type_doc_by_filename(filename: str) -> str | UUID:
    for i in DATA_BY_FILENAME.items():
        if filename in i[1]:
            return i[0]
    else:
        return uuid4()


def check_well_id_by_filename(filename: str) -> str | UUID:
    for i in DATA.items():
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
    return datetime.strptime(date, '%d-%b-%y %H:%M:%S')


def convert_csv_to_dataframe(
        storage: str,
        header_list: List[str],
) -> (List[DataFrame], List[str]):
    logger.info('Start converting csvs to dataframe')
    tmp_storage = os.walk(storage)
    df_list = []
    well_ids = []
    for root, _, files in tmp_storage:
        for file in files:
            data = pd.read_csv(
                os.path.join(root, file),
                names=header_list, delimiter=',',
                engine='python'
            )
            well_id = check_well_id_by_filename(file.rsplit('.', 1)[0])
            doctype = check_type_doc_by_filename(file.rsplit('.', 1)[0])

            well_ids.append(well_id)
            data['name_ind'] = doctype
            data['indicator'] = pd.to_numeric(data['indicator'], errors='coerce')
            data['date'] = data['date'].apply(convert_date)
            data['indicator'] = data['indicator'].astype('float64')
            df_list.append(data)
    logger.success('Finished converting csvs to dataframe')
    return df_list, well_ids


def convert_tsdb_validate_response(response: list):
    processed_data = {
        'q_gas_timed': [],
        'q_gc_timed': [],
        'q_wat_timed': []
    }

    for table in response:
        for record in table.records:
            record_name = record.values.get('name_ind')
            if isinstance(record.get_value(), (int, float)):
                if record_name == 'Расход по газу Вентури':
                    processed_data['q_gas_timed'].append(record.get_value())
                elif record_name == 'Расход по конденсату Вентури':
                    processed_data['q_gc_timed'].append(record.get_value())
                elif record_name == 'Расход по воде Вентури':
                    processed_data['q_wat_timed'].append(record.get_value())

    return processed_data


def convert_tsdb_adapt_response(response: list):
    processed_data = {
          "gamma_gas": 0.7, #Относительная плотность газа
          "gamma_gc": 0.86, #Относительная плотность газаконденсата
          "gamma_wat": 1.1, #Относительная плотность воды
          "d_tube": 0.3048, #Диаметр трубы выше по потоку
          "d_choke_percent_timed": [], #Процент открытия штуцера
          "p_out_timed": [], #Давление на манифольде
          "p_buf_timed": [], #Давление над буферной задвижкой ФА
          "t_buf_timed": [], #Температура на трубке Вентури
          "t_out_timed": [], #Температура на выкидной линии
          "q_gas_timed": [], #Расход по газу Вентури
          "q_gc_timed": [], #Расход по конденсату Вентури
          "q_wat_timed": [], #Расход по воде Вентури
          "timestamp": [] #Таймпоинты
    }
    for table in response:
        for record in table.records:
            record_name = record.values.get('name_ind')
            if isinstance(record.get_value(), (int, float)):
                if record_name == "Процент открытия штуцера":
                    processed_data['d_choke_percent_timed'].append(record.get_value())
                    processed_data['timestamp'].append(record.values.get("_time"))
                elif record_name == "Давление":
                    processed_data['p_out_timed'].append(record.get_value())
                elif record_name == "Давление над буферной задвижкой ФА":
                    processed_data['p_buf_timed'].append(record.get_value())
                elif record_name == "Температура на трубке Вентури":
                    processed_data['t_buf_timed'].append(record.get_value())
                elif record_name == "Температура на выкидной линии":
                    processed_data['t_out_timed'].append(record.get_value())
                elif record_name == "Расход по газу Вентури":
                    processed_data['q_gas_timed'].append(record.get_value())
                elif record_name == "Расход по конденсату Вентури":
                    processed_data['q_gc_timed'].append(record.get_value())
                elif record_name == "Расход по воде Вентури":
                    processed_data['q_wat_timed'].append(record.get_value())

    return processed_data


def convert_tsdb_fmm_response(response: list):
    processed_data = {
          "gamma_gas": 0.7, #Относительная плотность газа
          "gamma_gc": 0.86, #Относительная плотность газаконденсата
          "gamma_wat": 1.1, #Относительная плотность воды
          "d_tube": 0.3048, #Диаметр трубы выше по потоку
          "d_choke_percent": None, #Процент открытия штуцера
          "p_out": None, #Давление на манифольде
          "p_buf": None, #Давление над буферной задвижкой ФА
          "t_buf": None, #Температура на трубке Вентури
          "t_out": None, #Температура на выкидной линии
          "q_gas": None, #Расход по газу Вентури
          "q_gc": None, #Расход по конденсату Вентури
          "q_wat": None #Расход по воде Вентури
        }
    for table in response:
        for record in table.records:
            record_name = record.values.get('name_ind')
            if isinstance(record.get_value(), (int, float)):
                if record_name == "Процент открытия штуцера":
                    processed_data['d_choke_percent'] = record.get_value()
                elif record_name == "Давление":
                    processed_data['p_out'] = record.get_value()
                elif record_name == "Давление над буферной задвижкой ФА":
                    processed_data['p_buf'] = record.get_value()
                elif record_name == "Температура на трубке Вентури":
                    processed_data['t_buf'] = record.get_value()
                elif record_name == "Температура на выкидной линии":
                    processed_data['t_out'] = record.get_value()
                elif record_name == "Расход по газу Вентури":
                    processed_data['q_gas'] = record.get_value()
                elif record_name == "Расход по конденсату Вентури":
                    processed_data['q_gc'] = record.get_value()
                elif record_name == "Расход по воде Вентури":
                    processed_data['q_wat'] = record.get_value()

    return processed_data
