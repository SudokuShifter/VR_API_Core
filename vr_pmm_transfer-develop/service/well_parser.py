from typing import Union, Dict
from pathlib import Path
import plotly.graph_objects as go
import plotly.io as pio
import pandas as pd
import polars as pl


def read_mapping_data(path: str, index: int) -> pd.DataFrame:
    """
    Чтение данных маппинга из Excel файла.

    Параметры:
    - path (str): Путь к Excel файлу.
    - index (int): Индекс листа для чтения.

    Возвращает:
    - pd.DataFrame: Данные маппинга.
    """
    data = pd.read_excel(
        path,
        sheet_name=index,
        header=None,
        names=["object", "id", "parameter_name", "file_name", "units"],
    )
    data["object"] = data["object"].ffill()
    return data


def reconstruct_path(file_name: str) -> str:
    """
    Восстановление пути для указанного имени файла.

    Параметры:
    - file_name (str): Имя файла.

    Возвращает:
    - str: Восстановленный путь.
    """
    path = Path("Kaggle/InterpolatedData") / f"{file_name}.csv"
    return str(path)


def read_wells_data(mapping: pd.DataFrame) -> pd.DataFrame:
    """
    Чтение данных скважины на основе маппинга.

    Параметры:
    - mapping (pd.DataFrame): Данные маппинга.

    Возвращает:
    - pd.DataFrame: Данные скважины.
    """
    data = pd.DataFrame()
    for param, file in mapping[["parameter_name", "file_name"]].values:
        path = reconstruct_path(file)
        print(path)
        if "timestamp" in data.columns:
            df = (
                pl.scan_csv(
                    path,
                    has_header=False,
                    new_columns=["timestamp", "value"],
                    dtypes=[pl.Utf8, pl.Float64],
                    null_values=["Bad"],
                    try_parse_dates=False,
                )
                .collect()
                .to_pandas()
            )
        else:
            df = (
                pl.scan_csv(
                    path,
                    has_header=False,
                    new_columns=["timestamp", "value"],
                    dtypes=[pl.Utf8, pl.Float64],
                    null_values=["Bad"],
                    try_parse_dates=False,
                )
                .with_columns(pl.col("timestamp").str.strptime(pl.Datetime, format="%d-%b-%y %H:%M:%S"))
                .collect()
                .to_pandas()
            )
            data["timestamp"] = df["timestamp"].values
        data[param] = df["value"].values
    return data


def create_dataset(mapping: pd.DataFrame, well_ids=[]) -> pd.DataFrame:
    """
    Создание набора данных на основе маппинга и идентификаторов скважин.

    Параметры:
    - mapping (pd.DataFrame): Данные маппинга.
    - well_ids (List[str]): Список идентификаторов скважин.

    Возвращает:
    - pd.DataFrame: Созданный набор данных.
    """
    shared_data = read_wells_data(mapping.head(10))

    wells_ids = well_ids or mapping.iloc[10:, :]["object"].unique()

    data = dict()

    for well in wells_ids:
        try:
            well_mapping = mapping[mapping["object"] == well]
            well_data = read_wells_data(well_mapping)

            new_cols = shared_data.columns[1:]
            well_data[new_cols] = shared_data.iloc[:, 1:]

            data[well] = well_data
            print(f"Successfully loaded data: {well}")

        except Exception as e:
            print(f"Failed to load data: {well}. {e}")

    return data


def add_rolling_deltas(data: pd.DataFrame, parameters: list) -> pd.DataFrame:
    """
    Добавление скользящих дельт в набор данных для указанных параметров.

    Параметры:
    - data (pd.DataFrame): Входные данные.
    - parameters (List[str]): Список параметров для скользящих дельт.

    Возвращает:
    - pd.DataFrame: Набор данных со скользящими дельтами.
    """
    for param in parameters:
        data["rolling"] = data[param].rolling(window=3).mean().bfill()
        data[f"{param}_delta"] = data[param] - data["rolling"]
    return data.drop("rolling", axis="columns")


def select_period(data: pd.DataFrame, start_date: str, end_date: str):
    """
    Выбор периода из набора данных на основе начальной и конечной дат.

    Параметры:
    - data (pd.DataFrame): Входные данные.
    - start_date (str): Начальная дата.
    - end_date (str): Конечная дата.

    Возвращает:
    - pd.DataFrame: Набор данных для выбранного периода.
    """
    data = data[(data["timestamp"] >= start_date) & (data["timestamp"] <= end_date)]
    return data


def fill_na(data: pd.DataFrame) -> pd.DataFrame:
    """
    Заполнение пропущенных значений в наборе данных.

    Параметры:
    - data (pd.DataFrame): Входные данные.

    Возвращает:
    - pd.DataFrame: Набор данных с заполненными пропущенными значениями.
    """
    data = data.ffill()
    data = data.bfill()
    return data


def save_to_html(file_name: str, data: Union[pd.DataFrame, Dict]) -> None:
    """
    Сохранение графика по DataFrame в html.

    Параметры:
    - file_name (str): Название файла.
    - data (Union[pd.DataFrame, Dict]): Данные.
    """
    fig = go.Figure()

    if isinstance(data, pd.DataFrame):
        for col in data.columns:
            if 'timestamp' not in col:
                fig.add_trace(
                    go.Scatter(x=data['timestamp'], y=data[col], name=col),
                )
    elif isinstance(data, Dict):
        for col in data:
            if col != 'timestamp':
                fig.add_trace(
                    go.Scatter(x=data['timestamp'], y=data[col], name=col),
                )

    pio.write_html(fig, f'test_result/{file_name}.html')
