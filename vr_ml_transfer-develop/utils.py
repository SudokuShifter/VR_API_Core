"""Вспомогательные функции для чтения и преобразования исходных данных.
"""

import polars as pl
import numpy as np
import pandas as pd
from pathlib import Path
import shutil
import os
import py7zr
from datetime import datetime

import matplotlib.pyplot as plt
plt.style.use('default')

from settings import DATA_PATH

plt.rcParams.update({'font.size': 9})
import warnings
warnings.filterwarnings("ignore")

def mean_absolute_percentage_error(y_true, y_pred):
    # MAPE-value
    # < 11% высокоточный прогноз
    # >= 11 & < 21 хороший прогноз
    # >= 21 & < 51 разумный (нормальный) прогноз
    # >= 51 неточный прогноз
    # исключаем из списка y_true значения с нулевыми показателем и индекс исключенного значени  исключаем из y_pred

    empty_idx = []  # итндексы с пустыми значениями
    y_true, y_pred = np.array(y_true), np.array(y_pred)
    empty_idx = np.nonzero(y_true == 0)[0]
    y_true = np.delete(y_true, empty_idx, axis=0)  # это numpy.ndarry
    y_pred = np.delete(y_pred, empty_idx, axis=0)  # это numpy.ndarry

    _coef = float(np.mean(np.abs((y_true - y_pred) / y_true)) * 100)

    return _coef


def create_logfile(filename: str,
                   sep: str,
                   headers: list,
                   values: list,
                   ):
    if not Path(filename).exists():
        headers = f'{sep}'.join(headers)
        with open(filename, 'a') as f:
            f.write(headers + "\n")
            f.close

    values = f'{sep}'.join(map(str, values))
    with open(filename, 'a') as f:
        f.write(values + "\n")
        f.close


def unpack_archive(dir_path: str) -> None:
    """Функция извлекает содержимое множественных архивов в формате .7z
    из указанной директории в текущую рабочую директорию.
    """
    for path in Path(dir_path).iterdir():
        print(path)
        with py7zr.SevenZipFile(str(path), mode='r') as z:
            z.extractall()


def create_archive(filename: str):
    root_dir = Path().cwd()
    base_name = root_dir / filename
    shutil.make_archive(base_name=str(base_name), format='zip', root_dir=root_dir, base_dir=filename)


def read_mapping_data(path: str, index: int) -> pd.DataFrame:
    """Функция считывает данные из файла "Data_2022-2023.xlsx"
    и преобразует в удобный для дальнейшей обработки формат.
    :param path: Путь к файлу
    :param index: Индекс листа в эксель
    :return: Датафрейм с перечнем названий файлов, содержащих параметры
    по каждой скважине выбранного листа экселя
    """
    data = pd.read_excel(
        path, sheet_name=index, header=None, 
        names=['object', 'id', 'parameter_name', 'file_name', 'units'])
    data['object'] = data['object'].ffill()
    return data


def reconstruct_path(file_name: str) -> str:
    """Функция преобразует название файла в путь
    относительно текущей рабочей директории.
    :param file_name: Название файла с параметров без разширения
    :return: Путь к файлу
    """
    path = DATA_PATH / 'parameter_data' / f'{file_name}.csv'
    return str(path)


def read_wells_data(mapping: pd.DataFrame, requires_all_data=True) -> pd.DataFrame:
    """Функция считывает все данные по одной скважине и преобразует в общий датафрейм.
    :param mapping: Датафрейм с названиями файлов, содержащих параметры скважины
    :param requires_all_data: Аргумент определяет, будет ли возвращен датафрейм,
    если часть необходимых файлов отсутствует в директории с данными
    :return: Датафрейм с данными по скважине
    """
    data = pd.DataFrame()
    for param, file in mapping[['parameter_name', 'file_name']].values:
        path = reconstruct_path(file)
        try:
            if 'timestamp' in data.columns:
                df = pl.scan_csv(path, has_header=False, new_columns=['timestamp', 'value'],
                                 dtypes=[pl.Utf8, pl.Float32], null_values=['Bad'],
                                 try_parse_dates=False).collect().to_pandas()
            else:
                df = pl.scan_csv(path, has_header=False, new_columns=['timestamp', 'value'],
                                 dtypes=[pl.Utf8, pl.Float32], null_values=['Bad'],
                                 try_parse_dates=False).with_columns(pl.col('timestamp') \
                                    .str.strptime(pl.Datetime, format="%d-%b-%y %H:%M:%S")).collect().to_pandas()
                data['timestamp'] = df['timestamp'].values
            data[param] = df['value'].values
        except Exception as e:
            print(e)
            if requires_all_data:
                raise OSError
    return data


# def read_wells_data(mapping: pd.DataFrame) -> pd.DataFrame:
#     dateparse = lambda x: datetime.strptime(x, '%d-%b-%y %H:%M:%S')
#     data = pd.DataFrame()
#     for param, file in mapping[['parameter_name', 'file_name']].values:
#         path = reconstruct_path(file)
#         if 'timestamp' in data.columns:
#             df = pd.read_csv(path, header=None, names=['timestamp', 'value'],
#                              parse_dates=False, na_values=['Bad'])
#         else:
#             df = pd.read_csv(path, header=None, names=['timestamp', 'value'],
#                              parse_dates=[0], date_parser=dateparse, na_values=['Bad'])
#             data['timestamp'] = df['timestamp'].values
#         data[param] = df['value'].values
#     return data


def create_dataset(mapping: pd.DataFrame, well_ids=[], requires_all_data=True) -> dict:
    """Функция создает датасет по скважинам, содержащимся в датафрейме mapping.
    :param mapping: Датафрейм с названиями файлов, содержащих параметры скважин
    :param well_ids: Список названий скважин, если список пуст, используются данные всех скважин
    :param requires_all_data: Аргумент определяет, будет ли возвращен датафрейм по скважине,
    если часть необходимых файлов отсутствует в директории с данными
    :return: Словарь, где названиям скважин соответствуют датафреймы с данными
    """
    shared_data = read_wells_data(mapping.head(10), requires_all_data)
    
    wells_ids = well_ids or mapping.iloc[10:, :]['object'].unique()

    data = dict()
    
    for well in wells_ids:
        print(f'Started collecting data for: {well}')

        try:
            well_mapping = mapping[mapping['object'] == well]
            well_data = read_wells_data(well_mapping, requires_all_data)

            new_cols = shared_data.columns[1:]
            well_data[new_cols] = shared_data.iloc[:, 1:]

            data[well] = well_data
            print(f'Successfully loaded data: {well}')
        
        except Exception as e:
            print(f'Failed to load data: {well}. {e}')
            
    return data


# def add_features(data: pd.DataFrame) -> pd.DataFrame:
#     try:
#         data['delta_p1'] = data['Давление забойное'] - data['Давление над буферной задвижкой ФА']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_p2'] = data['Давление над буферной задвижкой ФА'] - data['Давление на трубке Вентури']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_p3'] = data['Давление над буферной задвижкой ФА'] - data['Давление']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_p4'] = data['Давление'] - data['Давление над буферной задвижкой ФА']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_p5'] = data['Давление'] - data['Давление забойное']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_t1'] = data['Температура забойная'] - data['Температура на выкидной линии']
#     except Exception as e:
#         print(e)
#     try:
#         data['delta_t2'] = data['Температура забойная'] - data['Температура на трубке Вентури']
#     except Exception as e:
#         print(e)
#     return data

def features_preprocessing(pkl: dict,
                           current_df: pd.DataFrame,
                           df_name: str,
                           base_features: list,
                           syntetic_features: list = None
                           ):
    """
    Функция добавляет в датафрейм список фичей из других датафреймов.
        pkl:
            Словарь со всеми датафреймами
        current_df:
            Текущий датафрейм
        df_name:
            Название текущего датафрейма
        base_features:
            Список базовых фичей
        syntetic_features:
            Список синтетических фичей
        """
    # создаем копию словаря
    _dict = pkl.copy()

    # удаляем нужный датафрейм
    del _dict[df_name]

    # если список синтетических фичей не пустой добавляем фичи к основным
    if type(syntetic_features) != type(None):
        base_features += syntetic_features
    # проходимся по всем остальным датафреймам в словаре
    #current_df = pd.DataFrame()
    for name in _dict.keys():
        # отбираем нужные столбцы(фичи) в текущем датафрейма
        df = pkl[name][base_features]
        # добавляем префикс(имя датафрейма) к каждой фиче
        cols = [feature + f'_{name}' for feature in base_features]
        # изменяем название столбцов в датафрейме
        df.columns = cols
        # конкат данных
        current_df = pd.concat((current_df, df), axis=1)
    return current_df


def add_features(data: pd.DataFrame) -> pd.DataFrame:
    """Функция добавляет в датафрейм фичи на основе разности
    значений пар параметров температур и давления.
    :param data: Датафрейм с данными по скважине
    :return: Обновленный датафрейм
    """
    try:
        data['delta_p1'] = (data['Давление забойное'] - data['Давление над буферной задвижкой ФА']).clip(lower=0)
    except Exception as e:
        print(e)
    try:
        data['delta_p2'] = (data['Давление над буферной задвижкой ФА'] - (data['Давление на трубке Вентури'] - 1)).clip(lower=0)
    except Exception as e:
        print(e)
    try:
        data['delta_p3'] = ((data['Давление на трубке Вентури'] - 1) - data['Давление']).clip(lower=0)
    except Exception as e:
        print(e)
    try:
        data['delta_p4'] = (data['Давление над буферной задвижкой ФА'] - data['Давление']).clip(lower=0)
    except Exception as e:
        print(e)
    try:
        data['delta_p5'] = (data['Давление забойное'] - data['Давление']).clip(lower=0)
    except Exception as e:
        print(e)
    try:
        data['delta_t1'] = data['Температура забойная'] - data['Температура на выкидной линии']
    except Exception as e:
        print(e)
    try:
        data['delta_t2'] = data['Температура забойная'] - data['Температура на трубке Вентури']
    except Exception as e:
        print(e)

    # добавил фрагмент из add_quadratic_features
    try:
        data['pzab_sq']                = data['Давление забойное'] ** 2
    except Exception as e:
        print(e)
    try:
        data['stuts_sq_delta_p4_sqrt'] = (data['Процент открытия штуцера']) ** 2 * data['delta_p4'] ** (1 / 2)
    except Exception as e:
        print(e)

    return data


def add_stat_features(data: pd.DataFrame, parameters: list) -> pd.DataFrame:
    """Функция добавляет в датафрейм фичи на основе математических преобразований
    исходных параметров.
    :param data: Датафрейм с данными по скважине
    :param parameters: Список параметров для преобразований
    :return: Обновленный датафрейм
    """
    for param in parameters:
        if param in data.columns:
            data[f'{param}_log'] = np.log(data[param].clip(lower=1e-10))
            data[f'{param}_exp'] = np.exp(data[param].clip(upper=709)).clip(upper=3.4e+38)
            data[f'{param}_sq'] = np.square(data[param]).clip(upper=3.4e+38)
            data[f'{param}_sqrt'] = np.sqrt(data[param].clip(lower=0))
        else:
            print(f'Warning: {param} not found in column names.')
    return data


def add_temporal_features(data: pd.DataFrame) -> pd.DataFrame:
    """Функция добавляет в датафрейм фичи на основе временных характеристик.
    :param data: Датафрейм с данными по скважине
    :return: Обновленный датафрейм
    """
    data['dayofweek'] = data['timestamp'].dt.dayofweek
    data['week'] = data['timestamp'].dt.isocalendar().week.astype(int)
    data['month'] = data['timestamp'].dt.month
    data['quarter'] = data['timestamp'].dt.quarter
    data['year'] = data['timestamp'].dt.year
    return data


def add_bins(data: pd.DataFrame) -> pd.DataFrame:
    """Функция добавляет в датафрейм новый параметр,
    разбивая исходные значения штуцера на 5 интервалов.
    :param data: Датафрейм с данными по скважине
    :return: Обновленный датафрейм
    """
    data['Процент открытия штуцера_bin'] = pd.cut(
        data['Процент открытия штуцера'].values,
        bins=[-1, 20, 40, 60, 80, 100],
        labels=[1, 2, 3, 4, 5]
    ).astype(int)
    return data


def calculate_bins(data: pd.DataFrame) -> pd.DataFrame:
    """Функция возвращает бины для данных, в которых содержались пропуски.
    :param data:
    :return:
    """
    return pd.cut(
        data['Процент открытия штуцера'].dropna().values,
        bins=[-1, 20, 40, 60, 80, 100],
        labels=[1, 2, 3, 4, 5]
    ).astype(int)


def add_mtpl_sum_features(data: pd.DataFrame) -> pd.DataFrame:
    """Функция добавляет в датафрейм фичи на основе суммирования
    и перемножения пар параметров.
    :param data: Датафрейм с данными по скважине
    :return: Обновленный датафрейм
    """
    data['t_sum'] = data['Температура забойная'] + (
            data['Температура на выкидной линии'].max() - data['Температура на выкидной линии'])
    data['delta_p_sum'] = data['delta_p4'] + data['delta_p5']
    data['pzab_shtutser'] = data['Давление забойное'] * (data['Процент открытия штуцера'])
    data['tzab_shtutser'] = data['Температура забойная'] * (data['Процент открытия штуцера'])
    data['tlin_shtutser'] = data['Температура на выкидной линии'] * (data['Процент открытия штуцера'])
    return data


def add_quadratic_features(data: pd.DataFrame) -> pd.DataFrame:
    data['pzab_sq'] = data['Давление забойное'] ** 2
    data['stuts_sq_delta_p4_sqrt'] = (data['Процент открытия штуцера']) ** 2 * data['delta_p4'] ** (1 / 2)
    return data


def add_rolling_deltas(data: pd.DataFrame, parameters: list) -> pd.DataFrame:
    for param in parameters:
        if param in data.columns:
            data['rolling'] = data[param].rolling(window=3).mean().bfill()
            data[f'{param}_delta'] = data[param] - data['rolling']
    return data.drop('rolling', axis='columns')


def select_period(data: pd.DataFrame, start_date: str, end_date: str):
    data = data[(data['timestamp'] >= start_date) & (data['timestamp'] <= end_date)]
    return data


def fill_na(data: pd.DataFrame) -> pd.DataFrame:
    data = data.ffill()
    data = data.bfill()
    return data


def filter_target_values(data: pd.DataFrame) -> pd.DataFrame:
    """Функция фильтрует датасет на основе значений таргетов.
    Верхняя граница для каждого таргета определена заказчиком.
    :param data: Датафрейм с данными по скважине
    :return: Отфильтрованный датафрейм
    """
    # вот тут добавить код исключить скважины простоя


    data = data[data['Расход по газу Вентури'] <= 200]
    data = data[data['Расход по конденсату Вентури'] <= 20]
    data = data[data['Расход по воде Вентури'] <= 20] # 40
    data.index = [i for i in range(len(data))]
    return data


def select_test_periods(data: pd.DataFrame) -> pd.DataFrame:
    """Функция находит даты значительной вариативности штуцера
    и возвращает датафрейм, содержащий только эти периоды.
    :param data: Датафрейм с данными по скважине
    :return: Отфильтрованный датафрейм
    """
    data['date'] = data['timestamp'].dt.date
    variance = data.groupby('date')['Процент открытия штуцера'].transform('var')
    data = data[variance > 50]
    return data


def read_test_dates() -> dict:
    """Функция считывает даты тестов на скважинах из файла
    и возвращает словарь, где названию скважины соответствует список дат.
    :return: Словарь со списками дат проведения тестов по скважинам
    """
    dateparse = lambda x: datetime.strptime(x, '%d.%m.%Y')
    path = Path().cwd() / 'data' / 'даты тестов.xlsx'
    if not os.path.isfile(path):
        return {}
    data = pd.read_excel(path, header=None, names=['group', 'well', 'dates_1', 'dates_2'],
                         parse_dates=True, date_parser=dateparse)
    data['well'] = data['well'].apply(lambda x: x.split('-')[1])
    dates = dict()
    for well, date in data[['well', 'dates_1']].values:
        dates[well] = [date]
    data.dropna(inplace=True)
    for well, date in data[['well', 'dates_2']].values:
        dates[well].extend([date])
    return dates


'''
ВИЗУАЛИЗАЦИЯ
'''
def total_separator_analyses(
                            pkl__val_actuals:dict,
                            pkl__val_predictions:dict,
                            model_log_dir: Path,
                            tech_line_id:int
                        ):
    df_val = pd.concat(pkl__val_actuals['Расход по газу'])
    df_pred = pd.concat(pkl__val_predictions['Расход по газу'])
    # df_val  = df_val.drop_duplicates(keep='first')
    # df_pred = df_pred.drop_duplicates(keep='first')
    df_val = df_val.sort_values(by=['well_id', 'timestamp'])
    df_pred = df_pred.sort_values(by=['well_id', 'timestamp'])

    df_val = df_val.groupby('timestamp').agg({'Расход по газу': 'sum', 'well_id': ['nunique', 'count']})
    df_val.columns = ["_".join(x) for x in df_val.columns.ravel()]
    df_val = df_val.rename(columns={'Расход по газу_sum': 'Расход по газу (факт)'})

    df_pred_base = df_pred.copy()
    df_pred = df_pred.groupby('timestamp').agg({'Расход по газу': 'sum', 'well_id': ['nunique', 'count']})
    df_pred.columns = ["_".join(x) for x in df_pred.columns.ravel()]
    df_pred = df_pred.rename(columns={'Расход по газу_sum': 'Расход по газу (модель)'})

    total_error = pd.concat([df_val[['Расход по газу (факт)']], df_pred[['Расход по газу (модель)']]], axis=1)
    total_error['mae'] = (total_error['Расход по газу (факт)'] - total_error['Расход по газу (модель)']).abs()
    mae = np.round(total_error['mae'].mean())
    mape = np.round(mean_absolute_percentage_error(df_val['Расход по газу (факт)'], df_pred['Расход по газу (модель)']),
                    2)

    x = pd.Series(df_pred_base['timestamp'].sort_values().unique())
    df_pred_base = df_pred_base.pivot(index='well_id', columns='timestamp', values='Расход по газу').fillna(0)
    wells_list = df_pred_base.index.to_list()
    y = np.array(df_pred_base)

    ax = plt.figure(figsize=(30, 10), dpi=300)

    # plt.scatter(x, df_pred['Расход по газу (модель)'], label='Модель', color='green', s=3)
    plt.plot(x, df_pred['Расход по газу (модель)'], label='Модель', color='green')
    plt.stackplot(x, y, labels=wells_list, alpha=0.7)

    # plt.scatter(x, df_val['Расход по газу (факт)'],   label='Факт',   color='gray',  s=1)
    plt.plot(x, df_val['Расход по газу (факт)'], label='Факт', color='gray')

    plt.title(f'Сепаратор тотал ТЛ № {tech_line_id} (Ошибка: mae {mae}, mape {mape}%)')

    # adding Label to the x-axis
    plt.xlabel('Timeline')
    plt.tight_layout()

    plt.tight_layout()
    filename = str(model_log_dir / f'Fact-Predict_Total__tl-{tech_line_id}.png')
    # adding legend to the curve
    plt.legend()
    plt.savefig(filename, dpi=300)
    #plt.show()
