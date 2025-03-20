import csv

import uvicorn
from fastapi import FastAPI, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Dict, Optional, Union, List
import pandas as pd
pd.options.display.float_format = '{:.3f}'.format
from utils import add_features
from model_utils import post_processing
from utils import mean_absolute_percentage_error
from manipulation import features_check, model_info
import pickle
from xgboost import XGBRegressor
import dateutil.parser
from pathlib import Path
import os
import glob
import datetime as dt
from datetime import datetime
import re
import ast
from healthcheck import HealthCheck
import re
import logging
import time
import json
from hashlib import blake2b
import sys, subprocess

logging.basicConfig(filename='server.log', level=logging.DEBUG, encoding='utf-8')



# import chardet
import cchardet as chardet
import warnings
warnings.filterwarnings("ignore")

'''                   Типизация принимаемых параметров для API, нужно для запуска fastapi 
По умолчанию значение None, так как на отсутствие некоторых параметров в приложении описано более подробное объяснение 
ошибки, чем ошибка fastapi по умолчанию                           
'''


class PredictRequest(BaseModel):
    features: Dict[str, Union[float, int, None]] = None
    well_id: int     = None
    target_name: str = None


class HistoryRequest(BaseModel):
    well_id: int              = None
    target_name: str          = None
    start_date: Optional[str] = None
    end_date: Optional[str]   = None


class AvailabilityOfModelRequest(BaseModel):
    well_id: Optional[int] = None
    target_name: Optional[str] = None


class Training(BaseModel):
    test_from_train: bool = None
    train_start: str = None
    train_end: str = None
    test_start: str = None
    test_end: str = None
    valid_start: str = None
    valid_end: str = None
    tech_line_number: int = None
    selected_features: List[str] = None
    targets: List[str] = None
    description: str = None
    model_id: str = None
    wells: Optional[List[int]] = None


class Checking(BaseModel):
    model_id: str = None






encoding    = 'utf-8'
app         = FastAPI()
health      = HealthCheck() # инициализация healthcheck
model_id    = 'models' #'models' # 8818ba72a5b43ffa30a012c05bdc96e6
MODEL_PATH  = Path(model_id) if model_id.startswith('models') else Path(f'models{model_id}')
LOG_PATH    = Path("logs")
host        = '0.0.0.0'
port        = 8082 #5000
print(f"Активная модель {model_id}")
print(MODEL_PATH)

use_chardet = False

target_names = [
    'Расход по газу',
    # 'Расход по воде',
    #'Расход по конденсату'
]


target_config =  {}

target_models_dict   = {}
target_models_config = {}


# загружаем модели и конфигурацию
for target_n in target_names:
    try:
        models_config = {}
        models_dict = {}

        target_base = target_n
        target_n    = target_n + str('_Вентури')
        target_n    = target_n.replace(" ","_")
        target_config[target_base] = target_n

        route_name   = MODEL_PATH / 'type' / target_n / 'model_name'
        route_config = MODEL_PATH / 'type' / target_n / 'model_config'

        subfolders_name   = [f.path for f in os.scandir(route_name) if f.is_dir()]
        subfolders_config = [f.path for f in os.scandir(route_config) if f.is_dir()]

        for sub_id in subfolders_name:
            sub_name = re.sub("[^0-9]", "", sub_id.split("Скважина")[1].split('ТЛ')[0])

            models_dict[sub_name] = XGBRegressor()
            models_dict[sub_name].load_model(glob.glob(f"{sub_id}/*.json")[0])

        for sub_id in subfolders_config:
            sub_name = re.sub("[^0-9]", "", sub_id.split("Скважина")[1].split('ТЛ')[0])

            objects = []
            with (open(glob.glob(f"{sub_id}/*.pickle")[0], "rb")) as openfile:
                while True:
                    try:
                        models_config[sub_name] = pickle.load(openfile)
                    except EOFError:
                        break
        target_models_dict[target_base] = models_dict
        target_models_config[target_base] = models_config

    except Exception as e:
        print(e)
        print(f'Нет модели для таргета {target_n}. Завершено')
        exit()

# создание директории и файла с логами
if not os.path.exists(LOG_PATH):
    os.mkdir(LOG_PATH)
dict_ = {'running_process': 0}
with open(f'{LOG_PATH}/training.log', 'w') as f:
    json.dump(dict_, f)

logging.info('Инициализация моделей прошла успешно.')

@app.post('/predict')
async def predict(data: PredictRequest):
    """
    ###################################
    Метод расчета целевого показателя
    ###################################
    """
    start_time = time.time()
    # data = request.get_json()
    if all(value is None for value in dict(data).values()):
        logging.error(f'predict: Error № 1 Передача пустого массива данных.')
        return JSONResponse(content={"Error": {'Code': '1', 'Message': 'Передача пустого массива данных.'}})

    missing_fields = []
    for col_name in ['features', 'well_id',  'target_name']:
        if getattr(data, col_name) is None:
            missing_fields.append(col_name)

    if len(missing_fields) > 0:
        fields = ', '.join(missing_fields)
        logging.error(f'predict: Error № 2 Отсутствие полей: {fields}.')
        return JSONResponse(content={"Error": {'Code': '2', 'Message': f'Отсутствие полей: {fields}.'}})

    # все ли нужные поля были получены
    try:
        features    = data.features
        well_id     = str(data.well_id)
        target_name = str(data.target_name)
        if any([features is None, well_id is None, target_name is None]):
            logging.error(f'predict: Error № 2 Отсутствие необходимых полей.')
            return JSONResponse(content={"Error": {"Code": "2", "Message": "Отсутствие необходимых полей."}})
    except KeyError:
        logging.error(f'predict: Error № 2 Отсутствие необходимых полей.')
        return JSONResponse(content={"Error": {"Code": "2", "Message": "Отсутствие необходимых полей."}})


    # проверяем наличие модели
    try:
        # отбираем нужную модель
        get_models = target_models_config[target_name][f'{well_id}']
    except KeyError:
        logging.error(f'predict: Error № 3 Модель для данной скважины отсутствует.')
        return JSONResponse(content={"Error": {"Code": "3", "Message": "Модель для данной скважины отсутствует."}})

    # проверяем наличие таргета
    if target_name not in target_names:
        logging.error(f'predict: Error № 4 Данный таргет отсутствует.')
        return JSONResponse(content={"Error": {"Code": "4", "Message": "Данный таргет отсутствует."}})

    # проверяем входит ли "Расход по газу Вентури" в features
    if "Расход по газу Вентури" in features:
        # создаем переменную для расчета mape
        gas_feature = features["Расход по газу Вентури"]
        # удаляем фичу
        del features["Расход по газу Вентури"]
    else:
        gas_feature = None
    # проверяем переданные фичи на пропуски
    missed_values = [key for key, value in features.items() if value is None]
    # если длина списка с пустыми фичами больше 0
    if len(missed_values) > 0:
        logging.error(f'predict: Error № 5 Были переданы пустые значения фичей.')
        # выдаем ошибку с указанием этих фичей
        return JSONResponse(content={"Error": {"Code": "5",
                                               "Message": f"Были переданы пустые значения фичей. Необходимый список фичей для заполнения значений: {missed_values}"}})

    # генерим синтетические фичи
    features_df = add_features(pd.DataFrame(features, index=[0]))
    features_df = features_df.to_dict('records')

    try:
        keys = list(get_models['features'])
        features_df = {k: features_df[0][k] for k in keys} # сортируем входные параметры в порядке, как подавались в модель
    except KeyError:
        logging.error(f'predict: Error № 6 Были переданы некорректные данные фичей.')
        return JSONResponse(content={"Error": {"Code": "6", "Message": f"Были переданы некорректные данные фичей. Необходимый список {keys}"}})
    # создаем датафрейм
    features_df = {k: [v] for k, v in features_df.items()}
    features_df = pd.DataFrame(features_df)

    # делаем предсказание
    regressor = target_models_dict[target_name][f'{well_id}']
    prediction = regressor.predict(features_df)
    prediction = list(pd.Series(prediction).round(3)) # без этой строчки не выводится число
    #print(prediction)

    # print(list(features_res['Процент открытия штуцера']))
    if "Процент открытия штуцера" in features_df:
        prediction = post_processing(prediction, features_df['Процент открытия штуцера'])
        # print(prediction)
    # собираем все данные в датафрейм

    PREDICTIONS_PATH = MODEL_PATH / 'type' / target_config[target_name] / 'model_log' / 'scoring_api.csv'
    print(PREDICTIONS_PATH)

    importances = pd.DataFrame({'features': target_models_config[target_name][well_id]['importance']['features'].values(),
                                'importance': target_models_config[target_name][well_id]['importance']['importance'].values()})

    features_interval = features_check(importances = importances, features = features_df, min_max_interval = target_models_config[target_name][well_id]['confidence']['features'])
    outliers = dict()
    for key, value in features_interval.drop('trust_rate', axis=1).dropna(axis = 1).to_dict('list').items():
        print(key, features_df[key][0])
        feature_dict = {'feature_value': float(features_df[key][0]),
                        'trust_rate_importance': round(float(features_interval[key][0]), 3),
                        'interval': target_models_config[target_name][well_id]['confidence']['features'][key]}
        outliers[key] = feature_dict
    features_interval = {
        'trust_rate': features_interval['trust_rate'][0],
        'outliers': outliers
    }


    speed_time = time.time() - start_time
    # MAPE
    if gas_feature != None:
        if gas_feature == 0 and prediction > 0:
            mape_value = 100
        elif gas_feature == 0 and prediction == 0:
            mape_value = 0
        else:
            mape_value = round(mean_absolute_percentage_error([gas_feature], prediction), 3)
    else:
        mape_value = None

    res = pd.DataFrame({'timestamp': datetime.now().strftime("%d-%m-%Y %H:%M:%S"),
                        'well_id': well_id,
                        'features': features_df.to_dict(orient='records'),
                        'actual_target': gas_feature,
                        'predicted_target': prediction[0],
                        'speed_time': speed_time,
                        'target_name': target_name,
                        'mape': mape_value,
                        'trust_rate': features_interval['trust_rate']
                        }, index=[0]).round(3)
    # проверка существует ли файл с результатми
    # если нет, то создаем файл и записываем данные
    if not os.path.exists(PREDICTIONS_PATH):
        res.to_csv(PREDICTIONS_PATH, sep=';', encoding=encoding)
    # иначе записываем в него данные
    else:
        enc = dict()
        if use_chardet:
            with open(PREDICTIONS_PATH, 'r+b') as f:
                enc = chardet.detect(f.read(-1))
        else:
            enc['encoding'] = encoding
        res.to_csv(PREDICTIONS_PATH, sep=';', mode='a', header=False, encoding=enc['encoding'])
    # выводим результат
    logging.info(f'predict: Модель {well_id} по таргету {target_name} вернула ответ {prediction[0]}')

    print('Время запроса: ', time.time() - start_time)
    # если переменная Расход по газу Вентури существует, то находим mse между ней и предсказанием модели
    if gas_feature != None:
        #return JSONResponse(content={target_name: prediction[0], "mape": mape_value, "confidence": features_interval})
        return JSONResponse(content={target_name: prediction[0], "mape": mape_value})
    #return JSONResponse(content={target_name: prediction[0], "confidence": features_interval})
    return JSONResponse(content={target_name: prediction[0]})


@app.post('/history')
async def history(data: HistoryRequest):
    """
    ###################################
    Метод получения историчности данных по отработке модели
    ###################################
    """
    if all(value is None for value in dict(data).values()):
        logging.error(f'history: Error № 1 Передача пустого массива данных.')
        return JSONResponse(content={"Error": {'Code': '1', 'Message': 'Передача пустого массива данных.'}})

    model = str(data.well_id)
    # проверяем указана ли модель
    if model == 'None':
        logging.error(f'history: Error № 2 Отсутствие необходимых полей: well_id.')
        return JSONResponse(content={"Error": {"Code": "2", "Message": "Отсутствие необходимых полей: well_id."}})

    # проверяем указан ли таргет
    target_name = str(data.target_name)
    if target_name == 'None':
        logging.error(f'history: Error № 2 Отсутствие необходимых полей: target_name.')
        return JSONResponse(content={"Error": {"Code": "2", "Message": "Отсутствие необходимых полей: target_name."}})

    # загружаем файл с предсказаниями
    try:
        PREDICTIONS_PATH = MODEL_PATH / 'type' / target_config[target_name] / 'model_log' / 'scoring_api.csv'
    except KeyError:
        logging.error(f'history: Error № 3 Таргет {target_name} отсутствует.')
        return JSONResponse(content={"Error": {"Code": "3", "Message": f"Таргет {target_name} отсутствует"}})


    if not os.path.exists(PREDICTIONS_PATH):
        logging.error(f'history: Error № 4 Файл с логами модели не найден.')
        return JSONResponse(content={"Error": {"Code": "4", "Message": "Файл с логами модели не найден."}})
    else:
        with open(PREDICTIONS_PATH, 'rb') as f:
            enc = chardet.detect(f.readlines()[1])
        df = pd.read_csv(PREDICTIONS_PATH, sep=';', index_col=0, encoding=enc['encoding'])


    # # проверка существует ли модель models_config
    # if f'{model}' not in models_config.keys():
    #     logging.error(f'history: Error № 3 Модель для данной скважины отсутствует.')
    #     return JSONResponse(content={"Error": {"Code": "3", "Message": "Модель для данной скважины отсутствует."}})
    # определяем границы интервала, если они есть
    start_date = data.start_date
    if start_date is None:
        start_date = df['timestamp'].min()

    end_date = data.end_date
    if end_date is None:
        end_date = df['timestamp'].max()

    df['well_id'] = df['well_id'].astype(str)

    df = df[
                    (df['timestamp'] <= end_date)
                &   (df['timestamp'] >= start_date)
                &   (df['well_id'] == model)
                &   (df['target_name'] == target_name)
            ]
    df.fillna('', inplace = True)

    if df.shape[0] > 0:
        df = df.to_dict('records')
        return JSONResponse(content=df)
    else:
        logging.error(f'history: Error № 5 История запросов отсутствует.')
        return JSONResponse(content={"Error": {"Code": "5", "Message": "История запросов отсутствует."}})

# Роут доступности модели (метод позволяет проверить если ли по данному идентификатору модель активный файл с моделью)
@app.post('/availability_of_model')
async def availability_of_model(data: AvailabilityOfModelRequest):
    """
    ###################################
    Метод проверки доступности модели
    ###################################
    """
    targets = os.listdir(f'{model_id}/type')
    if all(value is None for value in dict(data).values()):
        # logging.error(f'availability_of_model: Error № 1 Передача пустого массива данных.')
        # return JSONResponse(content={"Error": {'Code': '1', 'Message': 'Передача пустого массива данных.'}})

        model_description = model_info(model_id, models_config, targets)
        return JSONResponse(content={'Справка модели': model_description})

    # target = f'{data.target_name} Вентури'
    # target_path = target.replace(' ', '_')
    # path = f'models/type/{target_path}/model_config'

    df_features = {}
    # проверяем указана ли модель
    try:
        model = str(data.well_id)
    except KeyError:
        logging.error(f'availability_of_model: Error № 1 Отсутствие необходимых полей: well_id.')
        return JSONResponse(content={"Error": {"Code": "1", "Message": "Отсутствие необходимых полей: well_id."}})

    if model not in models_config.keys():
        logging.info(f'availability_of_model: Error № 2 Модель {model} недоступна.')
        return JSONResponse(content={"Error": {"Code": "2", "Message": f"Модель {model} недоступна."}})
    # проверяем указан ли таргет
    try:
        target_name = data.target_name
    except KeyError:
        logging.error(f'availability_of_model: Error № 3 Отсутствие необходимых полей: target_name.')
        return JSONResponse(content={"Error": {"Code": "3", "Message": "Отсутствие необходимых полей: target_name."}})

    if target_name not in target_config.keys():
        logging.info(f'availability_of_model: Error № 4 Таргет {target_name} недоступен.')
        return JSONResponse(content={"Error": {"Code": "4", "Message": f"Таргет {target_name} недоступен."}})

    if model in target_models_config[target_name].keys():
        logging.info(f'availability_of_model: Модель {model} доступна.')
        features = target_models_config[target_name][model]
        df_features = pd.DataFrame({'features': features['importance']['features'].values(),
                                    'importance': features['importance']['importance'].values()}).sort_values(
            by='importance', ascending=False).round(5).to_dict()
        model_status = True
    else:
        logging.info(f'availability_of_model: Модель {model} недоступна.')
        model_status = False
    # находим тех линию модели
    for string in os.listdir(f"{model_id}/type/{target_name.replace(' ','_')}_Вентури/model_config"):
        if f'Скважина_ЛА-{model}' in string:
            t_line = int(string[-1])

    return JSONResponse(content={
        'model_id': model,
        'model_status': model_status,
        'train_period': [str(features['train_data']['train_period']['start']),
                         str(features['train_data']['train_period']['end'])],
        'test_period': [str(features['val_data']['val_period']['start']),
                        str(features['val_data']['val_period']['end'])],
        'tech_line': t_line,
        'model_name': features['model_name'],
        'description': features['description'],
        'features_importances': df_features
    })


@app.post('/training')
async def training(data: Training):

    if all(value is None for value in dict(data).values()):
        logging.error(f'training: Error № 1 Передача пустого массива данных.')
        return JSONResponse(content={"Error": {'Code': '1', 'Message': 'Передача пустого массива данных.'}})

    missing_fields = [] # обязательные поля для заполнения
    empty_fields   = []
    for col_name in ['test_from_train', 'train_start', 'train_end', 'test_start', 'test_end', 'valid_start', 'valid_end', 'tech_line_number', 'selected_features', 'targets']:
        if getattr(data, col_name) is None:
            missing_fields.append(col_name)

        if len(str(getattr(data, col_name))) == 0:
            empty_fields.append(col_name)

    if len(missing_fields) > 0:
        fields = ', '.join(missing_fields)
        logging.error(f'training: Error № 2 Отсутствие полей: {fields}.')
        return JSONResponse(content={"Error": {'Code': '2', 'Message': f'Отсутствие полей: {fields}.'}})

    # если обязательные поля пустые
    if len(empty_fields) > 0:
        fields = ', '.join(empty_fields)
        logging.error(f'training: Error № 3 Некорректное значение у полей: {fields}.')
        return JSONResponse(content={"Error": {'Code': '3', 'Message': f'Некорректное значение у полей: {fields}.'}})

    # проверка корректности дат
    wrong_dates = [] # список для некорректных дат
    for date_col in ['train_start', 'train_end', 'test_start', 'test_end', 'valid_start', 'valid_end']:
        date_check = getattr(data, date_col)
        try:
            datetime.strptime(date_check, '%Y-%m-%d')
        except ValueError:
            wrong_dates.append(date_col)
    # if getattr(data, date_col) != dateutil.parser.parse(getattr(data, date_col), dayfirst=True).strftime('%Y-%m-%d'):

    if len(wrong_dates) > 0:
        fields = ', '.join(wrong_dates)
        logging.error(f'training: Error № 4 Некорректное значение даты у полей: {fields}.')
        return JSONResponse(content={"Error": {'Code': '4', 'Message': f'Некорректное значение даты у полей: {fields}.'}})

    # проверка на корректность, доступность таргета
    wrong_targets = []
    for target in data.targets:
        if target.replace(' Вентури', '') not in target_config.keys():
            wrong_targets.append(target)

    if len(wrong_targets) > 0:
        fields = ', '.join(wrong_targets)
        logging.error(f'training: Error № 5 Некорректное значение таргета: {fields}.')
        return JSONResponse(content={"Error": {"Code": "5", "Message": f"Таргет {fields} недоступен."}})

    # проверка корректности тех линии
    if data.tech_line_number not in [1, 2]:
        logging.error(f'training: Error № 6 Передано некорректное значение технологической линии.')
        return JSONResponse(content={"Error": {"Code": "6", "Message": f"Передано некорректное значение технологической линии"}})

    # запись параметров в json
    with open("params.json", "w") as f:
        json.dump(dict(data), f)


    # hash
    get_data = dict(data)
    if 'model_id'  in get_data.keys() and get_data['model_id'] is not None and len(get_data['model_id']) > 0:
        hash = str(data.model_id)
    else:
        k    = str(time.time()).encode('utf-8')
        h    = blake2b(key=k, digest_size=16)
        hash = h.hexdigest()


    if not os.path.exists(f'{LOG_PATH}/training_logs.csv'):
        #with open(f'{LOG_PATH}/training_logs.csv', 'w') as creating_new_csv_file:
            # pass
        init_df = pd.DataFrame(columns=['hash', 'stage', 'status', 'techline', 'timestamp'])
        init_df.to_csv(f'{LOG_PATH}/training_logs.csv', sep=';', header=True)


    with open(f'{LOG_PATH}/training.log', 'r') as f:
        log_dict = json.load(f)

    if log_dict['running_process'] == 1:
        return JSONResponse(content={"Message": "Моделирование уже запущено"})
    else:
        with open(f'{LOG_PATH}/training.log', 'w+') as f:
            log_dict = {'running_process': 1}  # если нет записи, то создаем ее в файле
            json.dump(log_dict, f)  # вносим запись в файл
        print("ok")
        process = subprocess.Popen([sys.executable, 'training.py', hash])

    logging.info(f'Запуск моделирования произведен')

    return JSONResponse(content={'callback': {'model_id': hash, 'status':'Обучение модели запущено'}})


@app.post('/training_check')
async def training_check(data: Checking):

    if all(value is None for value in dict(data).values()):
        logging.error(f'training_check: Error № 1 Передача пустого массива данных.')
        return JSONResponse(content={"Error": {'Code': '1', 'Message': 'Передача пустого массива данных.'}})

    missing_fields = []

    for col_name in ['model_id']:
        if col_name not in dict(data).keys():
            missing_fields.append(col_name)

    if len(missing_fields) > 0:
        fields = ', '.join(missing_fields)
        logging.error(f'training_check: Error № 2 Отсутствие полей: {fields}.')
        return JSONResponse(content={"Error": {'Code': '2', 'Message': f'Отсутствие полей: {fields}.'}})

    hash = str(data.model_id)

    log_file = f'{LOG_PATH}/training_logs.csv'
    if not os.path.exists(log_file):
        logging.error(f'training_check: Error № 3 Файл {log_file} не найден.')
        return JSONResponse(content={"Error": {"Code": "3", "Message": f"Файл {log_file} не найден."}})
    else:
        df = pd.read_csv(log_file, sep=';', index_col=0, engine='python', encoding=encoding)

    df = df[df['hash'] == hash]
    df['timestamp'] = pd.to_datetime(df['timestamp'])

    current_stage = df.sort_values(by='timestamp', ascending=False).head(1)
    current_stage['timestamp'] = current_stage['timestamp'].astype(str)
    current_stage = current_stage.to_dict('records')
    last_stage = df[df['status'] == "выполнено"].sort_values(by='timestamp', ascending=False).head(1)
    last_stage['timestamp'] = last_stage['timestamp'].astype(str)
    last_stage = last_stage.to_dict('records')

    return JSONResponse(content={'last_stage': last_stage, 'currrent_stage': current_stage})

"""
###################################
Метод проверки доступности сервиса
###################################
"""
@app.get('/')
@app.post('/')
async def main():
    return 'Success'

def available():
    logging.info(f'available: All work.')
    return True, "All work"

@app.get('/healthcheck')
@app.post('/healthcheck')
async def healthcheck():
    return JSONResponse(content=health.run_check(available))



if __name__ == '__main__':
    # url адрес на котором подымается ip
    uvicorn.run(app, port=int(port), host=str(host), log_level='info') # info # , log_level="debug"