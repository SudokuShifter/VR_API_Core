from typing import List, Optional

import numpy as np
from models.base import BaseModel
from pydantic import Field, validator


class ValidateTask(BaseModel):
    """
    Постановка задачи расчета валидации.

    Цель:
        * Вычислить медиану обводненности и газоконденсатного фактора
    """

    q_gas_timed: List[float] = Field(
        ...,
        description="Дебит газа, м3/с",
    )
    q_gc_timed: List[float] = Field(
        ...,
        description="Дебит газоконденсата, м3/с",
    )
    q_wat_timed: List[float] = Field(
        ...,
        description="Дебит воды, м3/с",
    )

    @classmethod
    def create_validate_task(cls, well_data):
        return cls(
            q_gas_timed=well_data["Расход по газу Вентури"].apply(
                lambda value: value * 1000 / 3600).tolist(),
            q_gc_timed=well_data["Расход по конденсату Вентури"].apply(
                lambda value: value / 3600).tolist(),
            q_wat_timed=well_data["Расход по воде Вентури"].apply(
                lambda value: value / 3600).tolist(),
        )

    @validator("q_gas_timed")
    def check_q_gas_timed(cls, value):
        if any(q_gas is None for q_gas in value):
            raise ValueError("q_gas_timed has undefined values")
        if any(q_gas < 0 for q_gas in value):
            raise ValueError("q_gas_timed has values lower than 0")
        return value

    @validator("q_gc_timed")
    def check_q_gc_timed(cls, value):
        if any(q_gc is None for q_gc in value):
            raise ValueError("q_gc_timed has undefined values")
        if any(q_gc < 0 for q_gc in value):
            raise ValueError("q_gc_timed has values lower than 0")
        return value

    @validator("q_wat_timed")
    def check_q_wat_timed(cls, value):
        if any(q_wat is None for q_wat in value):
            raise ValueError("q_wat_timed has undefined values")
        if any(q_wat < 0 for q_wat in value):
            raise ValueError("q_wat_timed has values lower than 0")
        return value

    class Config:
        schema_extra = {
            "examples": {
                "valid_request": {
                    "summary": "Correct data to create project",
                    "description": "Валидные данные запроса",
                    "value": {
                        "q_gas_timed": list(
                            np.arange(100 / 86400 * 8000,
                                      150 / 86400 * 8000,
                                      step=10 / 86400 * 8000)),
                        "q_gc_timed": list(
                            np.arange(10 / 86400, 15 / 86400,
                                      step=1 / 86400)),
                        "q_wat_timed": list(
                            np.arange(10 / 86400, 15 / 86400,
                                      step=1 / 86400)),
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


class ValidateTaskSolution(BaseModel):
    """Результаты задачи валидации."""

    wct: Optional[float] = Field(
        ...,
        description="Осредненные значения обводненности, д.ед.",
    )
    gas_condensate_factor: Optional[float] = Field(
        ...,
        description="Осредненные значения газоконденсатного фактора, м3/м3",
    )
