"""Вспомогательные функции для обучения и валидации моделей
и формирования графиков.
"""

import numpy as np
import pandas as pd
from datetime import date
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_percentage_error, mean_absolute_error
from xgboost import XGBRegressor
import datetime
import matplotlib.pyplot as plt
import manipulation
from utils import read_test_dates, select_test_periods, filter_target_values, create_logfile
from settings import TRAIN_START, TRAIN_END, TEST_START, TEST_END, test_from_train, VALID_START, VALID_END
import re
plt.rcParams.update({'font.size': 9})
plt.rcParams['figure.figsize'] = 12, 8
import numpy as np
np.random.seed(24)
import os

def get_split_indeces(data: pd.DataFrame, n_splits=5) -> list:
    """Функция возвращает пары индексов для определения начала и конца
    валидационного отрезка в данных.
    :param data: Датафрейм с данными по скважине
    :param n_splits: Количество фолдов
    :return: Список с индексами начала и конца валидационных фолдов
    """
    n_samples = len(data)
    step_size = n_samples // n_splits
    splits = []
    for i in range(n_splits):
        splits.append([i * step_size, (i + 1) * step_size])
    splits[-1][1] = n_samples
    return splits


def split_data(data: pd.DataFrame, indeces: list, features: list, target: str) -> tuple:
    """Функция преобразует датафрейм в обучающие и валидационные
    входные параметры и таргеты.
    :param data: Датафрейм с данными по скважине
    :param indeces: Индексы начала и конца валидационного отрезка
    :param features: Список входных параметров
    :param target: Название столбца, содержащего таргет
    :return: Кортеж, содержащий x_train, y_train, x_val, y_val
    """
    data_val = data[(data.index >= indeces[0]) & (data.index < indeces[1])]
    data_train = data.drop(data_val.index)
    return data_train[features], data_train[target], data_val[features], data_val[target]


def split_data_by_dates(data: pd.DataFrame, features: list, target: str,
                        train_start=TRAIN_START, train_end=TRAIN_END,
                        test_start=TEST_START, test_end=TEST_END,
                        valid_start=VALID_START, valid_end=VALID_END
                        ) -> tuple:
    """Функция преобразует датафрейм в обучающие и валидационные
    входные параметры и таргеты.
    :param data: Датафрейм с данными по скважине
    :param features: Список входных параметров
    :param target: Название столбца, содержащего таргет
    :param train_start: Дата начала обучающей выборки
    :param train_end: Дата окончания обучающей выборки
    :param test_start: Дата начала тестовой выборки
    :param test_end: Дата окончания тестовой выборки
    :return: Кортеж, содержащий x_train, y_train, x_val, y_val
    """
    # так как при таком условии теряются данные за train_end после 00:00, поэтому нужно добавит день и поставить строго <
    train_end = pd.to_datetime(train_end) + datetime.timedelta(days=1)
    test_end  = pd.to_datetime(test_end)  + datetime.timedelta(days=1)
    valid_end = pd.to_datetime(valid_end) + datetime.timedelta(days=1)

    # data_train = data[(data['timestamp'] >= train_start) & (data['timestamp'] <= train_end)]
    # data_val = data[(data['timestamp'] >= test_start) & (data['timestamp'] <= test_end)]

    data_train = data[(data['timestamp'] >= train_start) & (data['timestamp'] < train_end)] # обучение
    data_test  = data[(data['timestamp'] >= valid_start) & (data['timestamp'] < valid_end)] # валидация на обучении
    data_val   = data[(data['timestamp'] >= test_start)  & (data['timestamp'] < test_end)] # тест


    return data_train[features], data_train[target],  data_test[features], data_test[target],data_val[features], data_val[target]


def display_feature_importance(data: pd.DataFrame, target: pd.Series,
                               model: XGBRegressor, val_dates: pd.Series,
                               subdir: Path, well_id: str,
                                tech_line_id:int,
                               ) -> pd.DataFrame:
    """Функция сохраняет график значимости параметров модели
    и возвращает отсортированный в алфавитном порядке датафрейм значимости.
    :param data: Датафрейм входных параметров модели
    :param target: Series значений прогнозируемой переменной
    :param model: Обученная модель XGBoost
    :param val_dates: Даты начала и конца валидационного отрезка
    :param subdir: Имя директории
    :param well_id: Название скважины
    :return: Датафрейм с отсортированным списком параметров
    """
    start_date = val_dates.min().date()
    end_date = val_dates.max().date()
    importance = pd.DataFrame({
        'features': data.columns,
        'importance': model.feature_importances_
    })
    importance.sort_values(by='importance', inplace=True)

    plt.figure(figsize=(12, 16))
    plt.barh(importance['features'], importance['importance'])
    plt.title(f'XGBoost Feature Importance\n{well_id}__ТЛ-{tech_line_id}: {target.name}')
    plt.tight_layout()
    filename = str(subdir / f'{target.name}_{well_id}__ТЛ-{tech_line_id}_importances_{start_date}_{end_date}.png')
    plt.savefig(filename, dpi=300)
    # plt.show()
    plt.close()
    importance.sort_values(by='features', inplace=True)
    return importance


def display_forecast(y_true: pd.Series, y_pred: np.array, x: pd.DataFrame,
                     val_dates: pd.Series, subdir: Path, well_id: str,
                    tech_line_id:int,

                     ) -> None:
    """Функция сохраняет график с прогнозом модели на валидационном отрезке
    и фактическими значениями таргета.
    :param y_true: Фактические значения таргета
    :param y_pred: Прогноз модели
    :param x: Входные параметры
    :param val_dates: Даты валидационного отрезка
    :param subdir: Название директории
    :param well_id: Название скважины
    :return: None
    """
    start_date = val_dates.min().date()
    end_date = val_dates.max().date()
    errors = np.abs(y_true.values - y_pred)

    fig, ax = plt.subplots()
    ax_double_x = ax.twinx()

    ax_double_x.scatter(val_dates, x['Процент открытия штуцера'], color='0.8', label='% штуцера', s=1)
    ax_double_x.set_ylabel('Процент открытия штуцера')

    ax.scatter(val_dates, y_true, color='b', label='Ground truth')
    ax.scatter(val_dates, y_pred, color='g', label='Prediction')
    ax.scatter(val_dates, errors, color='r', label='Absolute Error', s=1)
    ax.set_ylabel(f'{y_true.name}')

    ax.legend(loc='upper left')
    ax.set_title(f'{well_id}_ТЛ-{tech_line_id}')
    ax_double_x.legend(loc='upper right')
    fig.tight_layout()
    filename = str(subdir / f'{y_true.name}_{well_id}__ТЛ-{tech_line_id}_forecast_{start_date}_{end_date}.png')
    plt.savefig(filename, dpi=300)
    # plt.show()
    plt.close()


# def display_forecast(y_true: pd.Series, y_pred: np.array, x: pd.DataFrame,
#                      val_dates: pd.Series, subdir: Path, well_id: str):
#     start_date = val_dates.min().date()
#     end_date = val_dates.max().date()
#     errors = np.abs(y_true.values - y_pred)
#
#     plt.scatter(val_dates, x['Процент открытия штуцера'], color='0.8', label='% штуцера', s=1)
#     plt.scatter(val_dates, y_true, color='b', label='Ground truth')
#     plt.scatter(val_dates, y_pred, color='g', label='Prediction')
#     plt.scatter(val_dates, errors, color='r', label='Absolute Error', s=1)
#     plt.ylabel(f'{y_true.name}')
#     plt.legend(loc='upper left')
#     plt.title(f'{well_id}')
#     plt.tight_layout()
#     filename = str(subdir / f'{y_true.name}_{well_id}_forecast_{start_date}_{end_date}.png')
#     plt.savefig(filename, dpi=300)
#     # plt.show()
#     plt.close()


def calculated_filtered_gas_scores(y_true, y_pred) -> tuple:
    """Функция вычисляет метрики тольк по тем точкам,
    где фактическое значение таргета было не менее 5.
    :param y_true: Фактические значения
    :param y_pred: Прогноз модели
    :return: MAE, MAPE
    """
    print('Warning: validation scores are calculated on filtered data.')
    mask = np.where(y_true.values >= 5)
    y_true = pd.Series(y_true.values[mask])
    y_pred = y_pred[mask]
    mae_score = mean_absolute_error(y_true, y_pred)
    mape_score = mean_absolute_percentage_error(y_true, y_pred)
    return mae_score, mape_score


def calculate_test_day_metrics(subdir: Path, well_id: str, y_true: pd.Series,
                               y_pred: np.array, val_dates: pd.Series, x: pd.DataFrame) -> None:
    """Функция вычисляет метрики точности модели на днях проведения тестов,
    если эти дни попадают в валидационный отрезок текущей модели,
    сохраняет график с прогнозом модели на тестовый день и фактическими значениями,
    добавляет полученные метрики в файл "test_day_scores.txt".
    :param subdir: Название директории
    :param well_id: Название скважины
    :param y_true: Фактические значения
    :param y_pred: Прогноз модели
    :param val_dates: Даты валидационного отрезка
    :param x: Входные параметры
    :return: None
    """
    well_num = well_id.strip().split('-')[1]
    print(test_days_dict)
    if well_num in test_days_dict:
        log_data = []
        for date in test_days_dict[well_num]:
            test_date = date.strftime('%Y-%m-%d')
            next_day = (date + pd.Timedelta(2, 'd')).strftime('%Y-%m-%d')

            mask = (val_dates >= test_date) & (val_dates < next_day)


            if mask.sum() > 0:
                test_day_y_true = y_true[mask.values]
                test_day_y_pred = y_pred[mask.values]

                mae_score  = mean_absolute_error(test_day_y_true, test_day_y_pred)
                mape_score = mean_absolute_percentage_error(test_day_y_true, test_day_y_pred)

                x_axis = val_dates[mask.values]
                errors = np.abs(test_day_y_true.values - test_day_y_pred)

                fig, ax = plt.subplots()
                ax_double_x = ax.twinx()

                ax_double_x.scatter(x_axis, x['Процент открытия штуцера'][mask.values],
                                    color='0.8', label='% штуцера', s=1)
                ax_double_x.set_ylabel('Процент открытия штуцера')

                ax.scatter(x_axis, test_day_y_true, color='b', label='Ground truth')
                ax.scatter(x_axis, test_day_y_pred, color='g', label='Prediction')
                ax.scatter(x_axis, errors, color='r', label='Absolute Error', s=1)
                ax.set_ylabel(f'{y_true.name}')

                ax.legend(loc='upper left')
                ax.set_title(f'{well_id}: MAE = {np.round(mae_score, 2)}')
                ax_double_x.legend(loc='upper right')
                fig.tight_layout()

                filename = str(subdir / f'test_day_{y_true.name}_{well_id}_{test_date}.png')
                plt.savefig(filename, dpi=300)
                # plt.show()
                plt.close()


                filename = str(subdir / f'test_day_scores.txt')
                create_logfile(filename, ';', ['target','well_id','date','mae_score','mape_score'], [y_true.name,well_id,date,mae_score, mape_score ])
                # with open(filename, 'a') as f:
                #     f.write(f'{y_true.name},{well_id},{date},{mae_score}\n')

                log_data.append({'date': date, 'mae_score': mae_score, 'mape_score': mape_score})


        return log_data


def post_processing(y_pred: np.array, stutser_values: np.array) -> np.array:
    """Функция выполняет постобработку прогноза модели.
    Отрицательные значения приравниваются к нулю.
    В точках, где процент открытия штуцера равен нулю,
    прогнозируемое значение приравнивается к нулю.
    :param y_pred: Массив, содержащий прогнозируемые значения
    :param stutser_values:  Массив значений штуцера
    :return:
    """
    y_pred = np.clip(y_pred, a_min=0, a_max=None)
    mask = stutser_values == 0
    y_pred[mask] = 0
    return y_pred


def train_model(x_train, y_train,  x_test, y_test, x_val, y_val,train_dates, val_dates: pd.Series,
                subdir: Path,
                subdir_config: Path,
                subdir_name: Path,
                well_id: str,
                tech_line_id:int) -> tuple:
    """Функция выполняет обучение и валидацию модели на выбранном отрезке.
    :param x_train: Входные параметры для обучения модели
    :param y_train: Значения таргетов для обучения модели
    :param x_val: Входные параметры для валидации модели
    :param y_val: Значения таргетов для валидации модели
    :param val_dates: Даты валидационного отрезка
    :param subdir: Имя директории
    :param well_id: Название скважины
    :return: Кортеж, содержащий MAE, MAPE, датафрейм со значимостью признаков, модель
    """

    model = XGBRegressor(tree_method='hist', predictor='cpu_predictor',
                         objective='reg:squarederror', booster='gbtree',
                         eval_metric='rmse', early_stopping_rounds=30,
                         n_jobs=-1, max_depth=4, n_estimators=300, subsample=0.7,
                         min_child_weight=20, learning_rate=0.1, random_state=24)
    fit_params = {
        'eval_set': [( x_test, y_test)],
        'verbose': True,
    }

    model.fit(x_train, y_train, **fit_params)
    y_pred = model.predict(x_val)
    #if len(y_pred) == 0: # сли на валидационной выборки
    y_pred= post_processing(y_pred, x_val['Процент открытия штуцера'].values)

    if y_val.name == 'Расход по газу Вентури':
        try:
            mae_score, mape_score = calculated_filtered_gas_scores(y_val, y_pred)
        except Exception as e:
            # Одна из скважин содержит 0 строк в валидационном сете после фильтрации. Для
            # нее метрики считаются без фильтрации.
            print(f'Failed to calculate filtered scores. Error: {e}')
            mae_score  = None
            mape_score =  None
            # mae_score = mean_absolute_error(y_val, y_pred)
            # mape_score = mean_absolute_percentage_error(y_val, y_pred)
    else:
        mae_score = mean_absolute_error(y_val, y_pred)
        mape_score = mean_absolute_percentage_error(y_val, y_pred)
    print(f'Mean Absolute Error: {mae_score}')
    print(f'Mean Absolute Percentage Error: {mape_score}')

    # есть дни для стресс теста, когда делают нрагузку на скважину
    # выгружается список так как могут быть несоклько дней
    if len(y_pred) > 0:
        y_pred_add = pd.DataFrame(y_pred, columns=['y_pred'])
        y_val_add = pd.DataFrame()
        y_val_add['y_val'] = y_val#
        y_val_add['timestamp'] = val_dates
        y_val_add =  y_val_add.reset_index(drop=True)
        x_val_add = x_val.reset_index(drop=True)
        total_data            = pd.concat([x_val_add, y_val_add,y_pred_add],axis=1)
        total_data['well_id'] = well_id
        if os.path.exists('check_api.csv'):
            total_data.to_csv(f'check_api__tl{tech_line_id}.csv', mode='a', header=False, sep=';')
        else:
            total_data.to_csv(f'check_api__tl{tech_line_id}.csv', sep=';')


    stress_metrics = calculate_test_day_metrics(subdir, well_id, y_val, y_pred,  val_dates, x_val)
    importance     = display_feature_importance(x_train, y_train, model, val_dates, subdir, well_id, tech_line_id)
    display_forecast(y_val, y_pred, x_val, val_dates, subdir, well_id, tech_line_id)

    well_id_sign = re.sub("[^0-9]", "", well_id.split("-")[1])
    config_meta = {
        'well_id'   : well_id_sign,
        'mae_score' : mae_score,
        'mape_score': mape_score,
        'features'  : x_train.columns,
        'importance': importance.to_dict(),
        'train_data':{
                'train_size':x_train.shape[0],
                'train_period':{
                    'start':train_dates.min(),
                    'end':train_dates.max(),
                },
            },
        'val_data':{
                'val_size':x_val.shape[0],
                'val_period':{
                    'start':val_dates.min(),
                    'end':val_dates.max(),
                },
        'stress_val_data':stress_metrics, # список результатов тестовых дней с высокой нагрузкой
        'created_dt':date.today()
        },



    }
    # сохранение модели при определенных трешхолдах mae_score и/или mape_score
    manipulation.save_model(model = model, prefix='wellname',  well_id = well_id_sign, config_meta=config_meta, subdir_config = subdir_config, subdir_name= subdir_name)
    return mae_score, mape_score, importance, model


def filter_train_set(x_train: pd.DataFrame, y_train: pd.Series, train_dates: pd.Series) -> tuple:
    """Функция выбирает в датасете только дни с высокой вариативностью
    диаметра штуцера.
    :param x_train: Входные параметры для обучения модели
    :param y_train: Значения таргетов для обучения модели
    :param train_dates: Даты обучающего отрезка данных
    :return: Отфильтрованные параметры для обучения модели и таргеты
    """
    x_train['timestamp'] = train_dates.values
    print(f'Source train_dates shape {train_dates.shape[0]}')
    x_train = select_test_periods(x_train)
    train_dates = train_dates[train_dates.index.isin(x_train.index.to_list())]

    y_train = y_train.loc[x_train.index]
    x_train.drop(['date', 'timestamp'], axis=1, inplace=True)
    print(f'Processed train_dates shape {train_dates.shape[0]}')
    print(f'Reduced training set to {len(x_train)} rows.')
    return x_train, y_train, train_dates


def run_experiment(data: pd.DataFrame, features: list, target: str,
                   subdir: Path,
                   subdir_config: Path,
                   subdir_name: Path,
                   well_id: str,
                   use_only_high_var_days:bool,
                   tech_line_id:int
                   ) -> tuple:
    """Функция выполняет обучение модели XGBoost и сравнивает прогноз с фактическими данными.
    :param data: Датафрейм с данными по скважине
    :param features: Список параметров для обучения модели
    :param target: Название прогнозируемой переменной
    :param subdir: Название директории
    :param well_id: Название скважины
    :param use_only_high_var_days: Аргумент определяет, будут ли для обучения модели
    выбраны только дни с высокой вариативностью штуцера
    :return: MAE, MAPE и датафреймы значимости признаков для всех фолдов
    """
    original_data = data.copy()
    total_param = target[:-8]
    print(f'original_data: {original_data.timestamp.min(), original_data.timestamp.max(), original_data.shape[0]}')
    data = filter_target_values(data)  # Удаление экстремальных значений таргетов.
    print(f'data: {data.timestamp.min(), data.timestamp.max(), data.shape[0]}')
    x_train, y_train, x_test, y_test, x_val, y_val = split_data_by_dates(data, features, target)

    #train_dates = data.iloc[x_train.index]['timestamp']
    #val_dates   = data.iloc[x_val.index]['timestamp']
    #val_dates   = data[(data['timestamp'] >= TEST_START) & (data['timestamp'] <= TEST_END)]['timestamp']
    train_dates = data[data.index.isin(x_train.index)]['timestamp']
    val_dates   = data[data.index.isin(x_val.index)]['timestamp']

    print("========================")
    print(well_id)
    print(f'x_train: {x_train.shape[0]}, x_val:   {x_val.shape[0]}')
    print(f'train_dates: {train_dates.shape[0]}, {train_dates.min()}, {train_dates.max()}')
    print(f'val_dates: {val_dates.shape[0]}, {val_dates.min()}, {val_dates.max()}')
    print(x_train.columns.to_list())

    if use_only_high_var_days:
        print("!!!!!!!!!!!!use_only_high_var_days (correct)!!!!!!!!!")
        x_train, y_train, train_dates = filter_train_set(x_train, y_train, train_dates)
        x_val, y_val, val_dates       = filter_train_set(x_val, y_val, val_dates)

    # если выбран способ собрать тестовую выборку для обучения из трейновой, то переопределяем
    if test_from_train:
        x_train, x_test, y_train, y_test = train_test_split(x_train, y_train, test_size=0.2, random_state=24)



    results = train_model(x_train, y_train, x_test, y_test, x_val, y_val,train_dates,\
                          val_dates, subdir,subdir_config,subdir_name,  well_id, tech_line_id)
    mae_score, mape_score, imp, model = results

    # Прогноз для нефильтрованных данных для сопоставления
    # с фактической суммой по этой скважине и в целом на сепараторе.
    print(f'Origin:{original_data.shape[0]}', {original_data.timestamp.min()}, {original_data.timestamp.max()})
    #original_data.to_csv('original_data.csv', sep=';')
    x_train, y_train, x_test, y_test, x_val, y_val = split_data_by_dates(original_data, features, target)
    print(f'x_train: {x_train.shape[0]}, x_test: {x_test.shape[0]}, x_val:   {x_val.shape[0]}')
    #val_dates = original_data.iloc[x_val.index]['timestamp']
    val_dates = original_data[original_data.index.isin(x_val.index)]['timestamp']
    #x_val.to_csv('x_val.csv', sep=';')


    actual_total_train = y_train.sum()
    actual_total_val = y_val.sum()

    y_pred = model.predict(x_train)
    y_pred = post_processing(y_pred, x_train['Процент открытия штуцера'].values)
    #y_pred = model.predict(x_train)
    pred_total_train = np.sum(y_pred)
    # (!!!!!!) занулять

    pred_val = model.predict(x_val)
    pred_val = post_processing(pred_val, x_val['Процент открытия штуцера'].values)
    #pred_val = model.predict(x_val)
    pred_total_val = np.sum(pred_val)

    filename = str(subdir / f'val_predictions_{total_param}.txt')
    create_logfile(filename, ';', ['well_id', 'actual_total_train', 'pred_total_train', 'actual_total_val', 'pred_total_val'],
                   [well_id, actual_total_train, pred_total_train, actual_total_val, pred_total_val])

    # with open(filename, 'a') as f:
    #     f.write(f'{well_id},{actual_total_train},{pred_total_train},{actual_total_val},{pred_total_val}\n')


    # val_dates = original_data[(original_data['timestamp'] >= TEST_START)
    #                           & (original_data['timestamp'] <= TEST_END)]['timestamp']

    val_prediction, val_actual = pd.DataFrame(), pd.DataFrame()
    if len(val_dates) > 0:
        val_prediction = pd.DataFrame({'well_id':well_id,'timestamp': val_dates, total_param: pred_val})
        val_actual     = pd.DataFrame({'well_id':well_id,'timestamp': val_dates, total_param: y_val})
    return mae_score, mape_score, imp, val_prediction, val_actual


# Считываем словарь с датами проведения тестов скважин.
test_days_dict = read_test_dates()