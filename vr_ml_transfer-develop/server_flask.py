import urllib.request
import time

import requests
from flask import Flask, request, jsonify
import pandas as pd
import json
from utils import add_features
from model_utils import post_processing
from datetime import datetime
import pickle
from xgboost import XGBRegressor
from pathlib import Path
import os
import glob
import re
from healthcheck import HealthCheck
import logging
logging.basicConfig(filename='server.log', level=logging.DEBUG)
import warnings
warnings.filterwarnings("ignore")
from IPython.display import display


app        = Flask(__name__)
health     = HealthCheck() # инициализация healthcheck
MODEL_PATH = Path('models')


# url адрес на котором подымается ip
host       = '0.0.0.0'
port       = 8082

target_names = [
    'Расход по газу',
    # 'Расход по воде',
    # 'Расход по конденсату'
]

models_config =  {}
models_dict   =  {}
target_config =  {}

# загружаем модели и конфигурацию
for target_n in target_names:
    target_base = target_n
    target_n    = target_n + str('_Вентури')
    target_n    = target_n.replace(" ","_")
    target_config[target_base] = target_n

    route_name   = MODEL_PATH / 'type' / target_n / 'model_name'
    route_config = MODEL_PATH / 'type' / target_n / 'model_config'

    subfolders_name   = [f.path for f in os.scandir(route_name) if f.is_dir()]
    subfolders_config = [f.path for f in os.scandir(route_config) if f.is_dir()]

    for sub_id in subfolders_name:
        sub_name = re.sub("[^0-9]", "", sub_id.split("-")[1])

        models_dict[sub_name] = XGBRegressor()
        models_dict[sub_name].load_model(glob.glob(f"{sub_id}/*.json")[0])

    for sub_id in subfolders_config:
        sub_name = re.sub("[^0-9]", "", sub_id.split("-")[1])

        objects = []
        with (open(glob.glob(f"{sub_id}/*.pickle")[0], "rb")) as openfile:
            while True:
                try:
                    models_config[sub_name] = pickle.load(openfile)
                except EOFError:
                    break

logging.info('Инициализация моделей прошла успешно.')

'''
###################################
Метод расчета целевого показателя
###################################
'''
@app.route('/predict', methods=['POST'])
def predict():
    start_time = time.time()

    data = request.get_json()
    if not data:
        logging.error(f'predict: Error № 1 Передача пустого массива данных.')
        return jsonify({"Error" : {'Code':'1', 'Message': 'Передача пустого массива данных.'}} )


    missing_fields = []
    for col_name in ['features', 'well_id',  'target_name']:
        if col_name not in data.keys():
            missing_fields.append(col_name)

    if len(missing_fields) > 0:
        fields = ', '.join(missing_fields)
        logging.error(f'predict: Error № 2 Отсутствие полей: {fields}.')
        return jsonify({"Error": {'Code': '2', 'Message': f'Отсутствие полей: {fields}.'}})

    # все ли поля нужны были получены
    try:
        features    = data['features']
        well_id     = data['well_id']
        target_name = data['target_name']
    except KeyError:
        logging.error(f'predict: Error № 2 Отсутствие необходимых полей.')
        return jsonify({"Error": {"Code": "2", "Message": "Отсутствие необходимых полей."}})

    # проверяем наличие модели
    try:
        # отбираем нужную модель
        get_models = models_config[f'{well_id}']
    except KeyError:
        logging.error(f'predict: Error № 3 Модель для данной скважины отсутствует.')
        return jsonify({"Error": {"Code": "3", "Message": "Модель для данной скважины отсутствует."}})

    # проверяем наличие таргета
    if target_name not in target_names:
        logging.error(f'predict: Error № 4 Данный таргет отсутствует.')
        return jsonify({"Error": {"Code": "4", "Message": "Данный таргет отсутствует."}})

    # генерим синтетические фичи

    features_df = add_features(pd.DataFrame(features, index=[0]))
    features_df = features_df.to_dict('records')
    try:
        keys = list(get_models['features'])
        features_df = {k: features_df[0][k] for k in keys} # сортируем входные параметры в порядке, как подавались в модель
    except KeyError:
        logging.error(f'predict: Error № 5 Были переданы некорректные данные фичей.')
        return jsonify({"Error": {"Code": "5", "Message": "Были переданы некорректные данные фичей."}})

    # создаем датафрейм
    features_df = {k: [v] for k, v in features_df.items()}
    features_df = pd.DataFrame(features_df)

    # делаем предсказание
    regressor =  models_dict[f'{well_id}']
    prediction = regressor.predict(features_df)
    prediction = list(pd.Series(prediction)) # без этой строчки не выводится число
    #print(prediction)

    # print(list(features_res['Процент открытия штуцера']))
    if "Процент открытия штуцера" in features_df:
        prediction = post_processing(prediction, features_df['Процент открытия штуцера'])
        print(prediction)

    # собираем все данные в датафрейм

    PREDICTIONS_PATH = MODEL_PATH / 'type' / target_config[target_name] / 'model_log' / 'scoring_api.csv'
    speed_time = time.time() - start_time
    res = pd.DataFrame({'timestamp': datetime.now().strftime("%d-%m-%Y %H:%M:%S"),
                        'well_id': well_id, 'features': features_df.to_dict(orient='records'),
                        'target_name': target_name,
                        'score': prediction[0],
                        'speed_time':speed_time,
                        }, index=[0])
    # проверка существует ли файл с результатми
    # если нет, то создаем файл и записываем данные
    if not os.path.exists(PREDICTIONS_PATH):
        res.to_csv(PREDICTIONS_PATH, sep=';', encoding='windows-1251')
    # иначе записываем в него данные
    else:
        with open(PREDICTIONS_PATH, 'a') as f:
            res.to_csv(f, sep=';', header=False, encoding='windows-1251')


    # выводим результат
    logging.info(f'predict: Модель {well_id} по таргету {target_name} вернула ответ {prediction[0]}')
    return jsonify({target_name: prediction[0]})




'''
###################################
Метод получения историчности данных по отработке модели
###################################
'''
@app.route('/history', methods=['POST'])
def history():
    data = request.get_json()

    # проверяем указана ли модель
    try:
        model = data['well_id']
    except KeyError:
        logging.error(f'history: Error № 2 Отсутствие необходимых полей: well_id.')
        return jsonify({"Error": {"Code": "2", "Message": "Отсутствие необходимых полей: well_id."}})
    # проверяем указан ли таргет
    try:
        target_name= data['target_name']
    except KeyError:
        logging.error(f'history: Error № 2 Отсутствие необходимых полей: target_name.')
        return jsonify({"Error": {"Code": "2", "Message": "Отсутствие необходимых полей: target_name."}})


    # загружаем файл с предсказаниями
    PREDICTIONS_PATH = MODEL_PATH / 'type' / target_config[target_name] / 'model_log' / 'scoring_api.csv'

    if not os.path.exists(PREDICTIONS_PATH):
        logging.error(f'history: Error № 6 Файл с логами модели не найден.')
        return jsonify({"Error": {"Code": "6", "Message": "Файл с логами модели не найден."}})
    else:

        df = pd.read_csv(PREDICTIONS_PATH, sep=';', index_col=0, encoding='windows-1251', engine='python')


    # проверка существует ли модель models_config
    if f'{model}' not in models_config.keys():
        logging.error(f'history: Error № 3 Модель для данной скважины отсутствует.')
        return jsonify({"Error": {"Code": "3", "Message": "Модель для данной скважины отсутствует."}})

    # определяем границы интервала, если они есть
    if 'start_date' in data.keys():
        start_date = data['start_date']
    else:
        start_date = df['timestamp'].min()
    if 'end_date' in data.keys():
        end_date = data['end_date']
    else:
        end_date = df['timestamp'].max()

    df = df[
                    (df['timestamp'] <= end_date)
                &   (df['timestamp'] >= start_date)
                &   (df['well_id'] == model)
                &   (df['target_name'] == target_name)
            ].to_dict('records')

    return jsonify(df)


'''
###################################
Метод проверки доступности модели
###################################
'''
# Роут доступности модели (метод позволяет проверить если ли по данному идентификатору модель активный файл с моделью)
@app.route('/availability_of_model', methods=['POST'])
def availability_of_model():
    # {"well_id":529}
    data         = request.get_json()
    model        = str(data['well_id'])
    model_status = None # устанавливает статус модели


    if model in models_config.keys():
        logging.info(f'availability_of_model: Модель {model} доступна.')
        model_status = True

    else:
        logging.info(f'availability_of_model: Модель {model} недоступна.')
        model_status = False

    return jsonify({
                        'model_id': model,
                        'model_status':model_status
                    })

'''
###################################
Метод проверки доступности сервиса
###################################
'''
@app.route('/')
def main():
    return 'Success'

def available():

    url  = f'http://{host}:{port}'
    code = urllib.request.urlopen(url).getcode()

    if code == 200:
        logging.info(f'available: All work.')
        return True, "All work"
    else:
        logging.info(f'available: Something is wrong!')
        return False, "Something is wrong!"

health.add_check(available)
app.add_url_rule("/healthcheck", "healthcheck", view_func=lambda: health.run())


# Старт  API
if __name__ == '__main__':
    app.run(host=host, port=port, debug=True)
