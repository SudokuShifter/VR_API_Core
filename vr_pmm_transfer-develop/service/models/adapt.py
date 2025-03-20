from datetime import datetime
from typing import List

import numpy as np
import pandas as pd
from models import limits
from models.base import BaseModel
from pydantic import Field, validator


class AdaptTask(BaseModel):
    """
    Постановка задачи расчета адаптации штуцера.

    Цель:
        * Вычислить данные кривой адаптации штуцера
    """

    gamma_gas: float = Field(
        ...,
        description="Относительная плотность газа, кг/м3",
        gt=limits.Fluid.GammaGas.MIN,
        lt=limits.Fluid.GammaGas.MAX,
    )
    gamma_gc: float = Field(
        ...,
        description="Относительная плотность газоконденсата, кг/м3",
        gt=limits.Fluid.GammaOil.MIN,
        lt=limits.Fluid.GammaOil.MAX,
    )
    gamma_wat: float = Field(
        ...,
        description="Относительная плотность воды кг/м3",
        gt=limits.Fluid.GammaWater.MIN,
        lt=limits.Fluid.GammaWater.MAX,
    )
    d_tube: float = Field(
        ...,
        description="Диаметр трубы выше по потоку, м",
        gt=0,
    )
    d_choke_percent_timed: List[float] = Field(
        ...,
        description="Значения процента открытия штуцера по времени, %",
    )
    p_out_timed: List[float] = Field(
        ...,
        description="Значения давления на манифольде от времени, Па",
    )
    p_buf_timed: List[float] = Field(
        ...,
        description="Значения буферного давления от времени, Па",
    )
    t_buf_timed: List[float] = Field(
        ...,
        description="Значения буферной температуры от времени, K",
    )
    t_out_timed: List[float] = Field(
        ...,
        description="Значения температуры на выкидной линии от времени, K",
    )
    q_gc_timed: List[float] = Field(
        ...,
        description="Значения расхода по конденсату Вентури от времени, м3/с",
    )
    q_gas_timed: List[float] = Field(
        ...,
        description="Значения расхода по газу Вентури от времени, м3/с",
    )
    q_wat_timed: List[float] = Field(
        ...,
        description="Значения расхода по воде Вентури от времени, м3/с",
    )
    timestamp: List[datetime] = Field(
        ...,
        description="Значения временных интервалов",
    )

    @classmethod
    def create_adapt_task(cls, adapt_well_data: pd.DataFrame):
        return cls(
            gamma_gas=limits.DEFAULT_GAS_GAMMA,
            gamma_gc=limits.DEFAULT_OIL_GAMMA,
            gamma_wat=limits.DEFAULT_WATER_GAMMA,
            d_tube=limits.UPSTREAM_CHANNEL_DIAMETER,
            d_choke_percent_timed=adapt_well_data[
                "Процент открытия штуцера"].tolist(),
            gas_condensate_factor_timed=[
                q_gas * limits.kSM3_to_SM3_COEF / q_gk if all(
                    [q_gk, q_gas]) else 0
                for q_gk, q_gas in zip(
                    adapt_well_data["Расход по конденсату Вентури"],
                    adapt_well_data["Расход по газу Вентури"]
                )
            ],
            wct_timed=[
                q_wat / (q_gk + q_wat) if all([q_gk, q_wat]) else 0
                for q_gk, q_wat in zip(
                    adapt_well_data["Расход по конденсату Вентури"],
                    adapt_well_data["Расход по воде Вентури"],
                )
            ],
            p_buf_timed=adapt_well_data[
                "Давление над буферной задвижкой ФА"]
            .apply(lambda value: value * 100000 + 101325)
            .tolist(),
            p_out_timed=adapt_well_data["Давление"].apply(
                lambda value: value * 100000 + 101325).tolist(),
            t_buf_timed=adapt_well_data["Температура"].apply(
                lambda value: value + 273.15).tolist(),
            t_out_timed=adapt_well_data[
                "Температура на выкидной линии"].apply(
                lambda value: value + 273.15).tolist(),
            q_gc_timed=adapt_well_data[
                "Расход по конденсату Вентури"].apply(
                lambda value: value / 3600).tolist(),
            q_gas_timed=adapt_well_data["Расход по газу Вентури"].apply(
                lambda value: value * 1000 / 3600).tolist(),
            q_wat_timed=adapt_well_data["Расход по воде Вентури"].apply(
                lambda value: value / 3600).tolist(),
            timestamp=adapt_well_data["timestamp"].astype(str).tolist(),
        )

    @validator("d_choke_percent_timed", each_item=True)
    def check_d_choke_percent_timed(cls, value):
        if value is None:
            raise ValueError(
                "d_choke_percent_timed has undefined values")
        if value < 0:
            raise ValueError("Prices must be greater than 0")
        return value

    @validator("p_out_timed", each_item=True)
    def check_p_out_timed(cls, value):
        if value is None:
            raise ValueError(
                "p_out_timed has undefined values")
        if value <= 0:
            raise ValueError(
                "p_out_timed has values lower or equal 0")
        return value

    @validator("p_buf_timed")
    def check_p_buf_timed(cls, value, values):
        if any(p_buf is None for p_buf in value):
            raise ValueError(
                "p_buf_timed has undefined values")
        if any(p_buf <= 0 for p_buf in value):
            raise ValueError(
                "p_buf_timed has values lower or equal 0")
        try:
            if any(p_buf < p_out for p_buf, p_out in
                   zip(value, values["p_out_timed"])):
                raise ValueError("p_buf lower than p_out")
        except KeyError:
            pass
        return value

    @validator("t_buf_timed", each_item=True)
    def check_t_buf_timed(cls, value):
        if value is None:
            raise ValueError(
                "t_buf_timed has undefined values")
        if value <= 0:
            raise ValueError(
                "t_buf_timed has values lower or equal 0")
        return value

    @validator("t_out_timed", each_item=True)
    def check_t_out_timed(cls, value):
        if value is None:
            raise ValueError(
                "t_out_timed has undefined values")
        if value <= 0:
            raise ValueError(
                "t_out_timed has values lower or equal 0")
        return value

    @validator("q_gc_timed", each_item=True)
    def check_q_gc_timed(cls, value):
        if value is None:
            raise ValueError("q_gc_timed has undefined values")
        if value < 0:
            raise ValueError(
                "q_gc_timed has values lower than 0")
        return value

    @validator("q_gas_timed", each_item=True)
    def check_q_gas_timed(cls, value):
        if value is None:
            raise ValueError(
                "q_gas_timed has undefined values")
        if value < 0:
            raise ValueError(
                "q_gas_timed has values lower than 0")
        return value

    @validator("q_wat_timed", each_item=True)
    def check_q_wat_timed(cls, value):
        if value is None:
            raise ValueError(
                "q_wat_timed has undefined values")
        if value < 0:
            raise ValueError(
                "q_wat_timed has values lower than 0")
        return value

    @validator("timestamp", each_item=True)
    def check_timestamp(cls, value):
        if value is None:
            raise ValueError("timestamp has undefined values")
        return value

    class Config:
        SIZE_OF_ARRAY = 11
        schema_extra = {
            "examples": {
                "valid_request": {
                    "summary": "Correct data to create project",
                    "description": "Валидные данные запроса",
                    "value": {
                        "gamma_gas": 0.7,
                        "gamma_gc": 0.86,
                        "gamma_wat": 1.1,
                        "d_tube": 0.3048,
                        "d_choke_percent_timed": np.arange(
                            0,
                            10 * SIZE_OF_ARRAY,
                            10
                        ).tolist(),
                        "p_out_timed": np.arange(
                            101325,
                            101325 * (SIZE_OF_ARRAY + 1),
                            step=101325,
                        ).tolist(),
                        "p_buf_timed": np.arange(
                            101325 * 10,
                            101325 * 10 * (SIZE_OF_ARRAY + 1),
                            step=101325 * 10
                        ).tolist(),
                        "t_buf_timed": np.arange(
                            273.15 + 80,
                            273.15 + 80 + SIZE_OF_ARRAY * 10,
                            step=10
                        ).tolist(),
                        "t_out_timed": np.arange(
                            273.15 + 70,
                            273.15 + 70 + + SIZE_OF_ARRAY * 10,
                            step=10
                        ).tolist(),
                        "q_gas_timed": np.arange(
                            100 / 86400 * 8000,
                            (100 + SIZE_OF_ARRAY * 10) / 86400 * 8000,
                            step=11 / 86400 * 8000
                        ).tolist(),
                        "q_gc_timed": np.arange(10 / 86400, 21 / 86400,
                                                step=1 / 86400).tolist(),
                        "q_wat_timed": np.arange(100 / 86400 * 8000,
                                                 210 / 86400 * 8000,
                                                 step=11 / 86400 * 8000).tolist(),
                        "timestamp": ["2023-11-30T16:30:00",
                                      "2023-11-30T16:30:30",
                                      "2023-11-30T16:31:00",
                                      "2023-11-30T16:31:30",
                                      "2023-11-30T16:32:00",
                                      "2023-11-30T16:32:30",
                                      "2023-11-30T16:33:00",
                                      "2023-11-30T16:33:30",
                                      "2023-11-30T16:34:00",
                                      "2023-11-30T16:34:30",
                                      "2023-11-30T16:35:00"]
                    }
                },
            }
        }
        responses = {
            422: {
                "description": "Invalid response",
                "content": {
                    "application/json": {
                        "example": {"success": False,
                                    "message": ["string"]}
                    }
                },
            },
        }


class AdaptTaskSolution(BaseModel):
    """Результат решения задачи расчета адаптации штуцера."""

    d_choke_percent_adapt: List[float] = Field(
        np.arange(0, 100, 20).tolist(),
        description="Список процентов открытия штуцера, %",
    )
    c_choke_adapt: List[float] = Field(
        np.arange(0, 1.0, 0.2).tolist(),
        description="Список значений коэффициентов адаптации",
    )
