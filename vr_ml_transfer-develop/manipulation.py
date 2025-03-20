import pandas as pd
pd.set_option('display.max_rows', 500)
pd.set_option('display.max_columns', 500)
pd.set_option('display.width', 1000)
import numpy as np
from xgboost import XGBRegressor
import datetime
import pickle
from pathlib import Path
from sqlalchemy import create_engine, text
import re
import ast
import os
from IPython.display import display

def save_model(
                model,
                prefix:str,
                well_id:int,
                config_meta:dict,
                subdir_config: Path,
                subdir_name: Path

                ) -> tuple:

    # прописываем пути моделей и кионфигурации
    path_config  = f"{subdir_config}/{prefix}__{well_id}"
    path_model   = f"{subdir_name}/{prefix}__{well_id}"


    # сохраняем модель
    model.save_model(f"{path_model}.json")

    # сохраняем словарь с конфигурацией на которой обучалась модель (метаданные)
    with open(f'{path_config}.pickle', 'wb') as handle:
        pickle.dump(config_meta, handle, protocol=pickle.HIGHEST_PROTOCOL)

    return f'Модель сохранена: {path_model}'


def load_model(
                filename:str,
                well_id:int,
                ):

    return False

def features_check(importances: pd.DataFrame, features: pd.DataFrame, min_max_interval: dict):

    df = features.copy()
    importances_dict = dict(zip(importances['features'], importances['importance']))

    for feature in df.columns:
        df[feature] = (~df[feature].between(min_max_interval[feature]['min_value'], min_max_interval[feature]['max_value'])) \
            .astype(int).replace([0, 1], [np.nan, importances_dict[feature]])

    trust_rate = round(1 - df.sum(axis=1), 3)
    df.insert(0, 'trust_rate', trust_rate)

    return df

def model_info(model_id, models_config, targets):
    # находим все скважины
    wells = [f'Скважина_ЛА-{key}' for key in models_config.keys()]
    # берем первый файл из config
    configs = models_config[next(iter(models_config))]
    # технологические линии
    tech_lines = sorted(list(set([f'Технологическая линия {line[-1]}' for line in
                                  os.listdir(f'{model_id}/type/{targets[0]}/model_config')])))

    targets = [target.replace('_', ' ') for target in targets]

    descript_dict = {
        'train_period': [str(configs['train_data']['train_period']['start']), str(configs['train_data']['train_period']['end'])],
        'test_period':  [str(configs['val_data']['val_period']['start']), str(configs['val_data']['val_period']['end'])],
        'tech_lines': tech_lines,
        'features': list(configs['features']),
        'targets': targets,
        'description': configs['description'],
        'model_name': configs['model_name'],
        'created_date': str(configs['val_data']['created_dt']),
        'wells_list': sorted(wells)
    }
    return descript_dict

