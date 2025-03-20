from enum import Enum

import orjson
from pydantic import BaseModel as PydanticBaseModel


class Columns(Enum):
    timestamp = 'timestamp'
    d_choke = "Процент открытия штуцера"
    q_gas = "Расход по газу Вентури"
    q_gk = "Расход по конденсату Вентури"
    q_wat = "Расход по воде Вентури"
    t_out = "Температура на выкидной линии"
    t_buf = "Температура"
    p_buf = "Давление над буферной задвижкой ФА"
    p_vent = "Давление"


MAPPING_FILE_PATH = r"Kaggle/Data_2022-2023-tl1.xlsx"


def orjson_dumps(v, *, default):
    return orjson.dumps(
        v,
        default=default,
        option=orjson.OPT_NON_STR_KEYS | orjson.OPT_SERIALIZE_NUMPY,
    ).decode()


class BaseModel(PydanticBaseModel):
    class Config:
        json_loads = orjson.loads
        json_dumps = orjson_dumps

    def get_iterable_fields(self):
        return [field for field in self.__fields__ if isinstance(getattr(self, field), list)]


class BaseTask(BaseModel):
    """
    Постановка задачи расчета

    Цель:
        * ...
    """


class BaseTaskSolution(BaseModel):
    """
    Решение задачи
    """
