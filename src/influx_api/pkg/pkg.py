import os

import pandas as pd


DATA = {}
DATA_BY_FILENAME = {}



def generate_well_id_by_file_name_dict(filename: str):
    global DATA
    if DATA:
        return DATA
    df = pd.read_excel(filename, skiprows=5)
    DATA = {i[0].rsplit('-', 1)[-1].strip(): set() for i in df.values}
    for i in df.values:
        if len(i[4]) > 5:
            DATA[i[0].rsplit('-', 1)[-1].strip()].add(i[4])
    return DATA


def generate_param_by_filename_dict(filename: str):
    global DATA_BY_FILENAME
    if DATA_BY_FILENAME:
        return DATA_BY_FILENAME
    df = pd.read_excel(filename, skiprows=5)
    DATA_BY_FILENAME = {i[2]: set() for i in df.values}
    for i in df.values:
        if len(i[4]) > 5:
            DATA_BY_FILENAME[i[2]].add(i[4].rsplit('(', 1)[-1].strip())
    return DATA_BY_FILENAME


generate_well_id_by_file_name_dict(os.getcwd() + '\influx_api\pkg\LUN-A.xlsx')
generate_param_by_filename_dict(os.getcwd() + '\influx_api\pkg\LUN-A.xlsx')
