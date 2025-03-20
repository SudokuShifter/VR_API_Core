from functools import wraps
from typing import List, Optional

from models import limits
from models.base import BaseModel
from pydantic import Field, validator


def skip_if_d_choke_percent_zero(func):
    """
    Декоратор, который пропускает вызов функции, если d_choke_percent <= 0.

    Args:
    - func: Функция, которую нужно декорировать.

    Returns:
    - wrapper: Обернутая функция.
    """

    @wraps(func)
    def wrapper(cls, value, values):
        d_choke_percent = values.get("d_choke_percent", 0)
        if d_choke_percent <= 0:
            return value
        return func(cls, value, values)

    return wrapper


class FMMTask(BaseModel):
    """Модель для описания параметров задачи физ-мат расчета."""

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
    d_choke_percent: float = Field(
        ...,
        description="Процент открытия штуцера, %",
        gte=0,
        lte=100,
    )
    gas_condensate_factor: float = Field(
        ...,
        description="Газоконденсатный фактор, м3/м3",
    )
    wct: float = Field(
        ...,
        description="Обводненность, д.ед.",
    )
    p_out: float = Field(
        ...,
        description="Давление на манифольде, Па",
    )
    p_buf: float = Field(
        ...,
        description="Давление буферное, Па",
    )
    t_buf: float = Field(
        ...,
        description="Температура буферная, K",
    )
    t_out: float = Field(
        ...,
        description="Температура на выкидной линии, K",
    )
    q_gc: Optional[float] = Field(
        ...,
        description="Расход по конденсату Вентури, м3/с",
        gte=0,
    )
    q_gas: Optional[float] = Field(
        ...,
        description="Расход по газу Вентури, м3/с",
        gte=0,
    )
    q_wat: Optional[float] = Field(
        ...,
        description="Расход по воде Вентури, м3/с",
        gte=0,
    )
    d_choke_percent_adapt: List[float] = Field(
        default_factory=list,
        description="Список процентов открытия штуцера, %",
    )
    c_choke_adapt: List[float] = Field(
        default_factory=list,
        description="Список значений коэффициента адаптации",
    )

    @classmethod
    def create_fmm_data(cls, d_choke_percent, rp, wct, p_buf, p_vent,
                        t_buf, t_out, q_gc, q_gas, q_wat,
                        adaptation_solution):
        """Инициализирует начальные данные."""
        return cls(
            gamma_gas=limits.DEFAULT_GAS_GAMMA,
            gamma_gc=limits.DEFAULT_OIL_GAMMA,
            gamma_wat=limits.DEFAULT_WATER_GAMMA,
            d_tube=limits.UPSTREAM_CHANNEL_DIAMETER,
            d_choke_percent=d_choke_percent,
            gas_condensate_factor=rp,
            wct=wct,
            p_buf=p_buf * 100000 + 101325,
            p_out=p_vent * 100000 + 101325,
            t_buf=273.15 + t_buf,
            t_out=273.15 + t_out,
            q_gc=q_gc * 24 / 86400,
            q_gas=q_gas * 24 * 1000 / 86400,
            q_wat=q_wat * 24 / 86400,
            d_choke_percent_adapt=adaptation_solution.d_choke_percent_adapt,
            c_choke_adapt=adaptation_solution.c_choke_adapt,
        )

    @validator("p_buf", "p_out", always=True)
    @skip_if_d_choke_percent_zero
    def check_pressure(cls, value, values):
        """
        Валидатор для проверки давления с учетом значения d_choke_percent.

        Args:
        - cls: Класс модели, к которой применяется валидатор.
        - value: Значение давления, которое нужно проверить.
        - values: Словарь значений для других полей модели.

        Raises:
        - ValueError: Если значение давления меньше или равно нулю,
        или если оно меньше или равно значению поля p_out.

        Returns:
        - Значение давления, если валидация успешна.
        """
        if value <= 0:
            raise ValueError("must be positive")
        if "p_out" in values and value <= values["p_out"]:
            raise ValueError("p_buf is less or equal p_out")
        return value

    @validator("t_buf", "t_out", always=True)
    @skip_if_d_choke_percent_zero
    def check_temperature(cls, value, values):
        """
        Валидатор для проверки температуры с учетом значения d_choke_percent.

        Args:
        - cls: Класс модели, к которой применяется валидатор.
        - value: Значение температуры (t_buf), которое нужно проверить.
        - values: Словарь значений для других полей модели.

        Raises:
        - ValueError: Если значение температуры меньше или равно нулю.

        Returns:
        - Значение температуры, если валидация успешна.
        """
        if value <= 0:
            raise ValueError("must be positive")
        return value

    @validator("gas_condensate_factor", always=True)
    @skip_if_d_choke_percent_zero
    def check_gc(cls, value, values):
        """
        Валидатор для проверки газоконденсатного фактора с учетом значения d_choke_percent.

        Args:
        - cls: Класс модели, к которой применяется валидатор.
        - value: Значение газоконденсатного фактора, которое нужно проверить.
        - values: Словарь значений для других полей модели.

        Raises:
        - ValueError: Если значение газоконденсатного фактора меньше нуля.

        Returns:
        - Значение газоконденсатного фактора, если валидация успешна.
        """
        if value < 0:
            raise ValueError("must be positive")
        return value

    @validator("wct", always=True)
    @skip_if_d_choke_percent_zero
    def check_wct(cls, value, values):
        """
        Валидатор для проверки обводненности с учетом значения d_choke_percent.

        Args:
        - cls: Класс модели, к которой применяется валидатор.
        - value: Значение обводненности, которое нужно проверить.
        - values: Словарь значений для других полей модели.

        Raises:
        - ValueError: Если значение обводненности меньше 0 или больше 1.

        Returns:
        - Значение обводненности, если валидация успешна.
        """
        lower_bound = 0
        upper_bound = 1
        if value < lower_bound or value > upper_bound:
            raise ValueError(
                f"value is out of bounds [{lower_bound}, {upper_bound}]")
        return value

    @validator("d_choke_percent_adapt", each_item=True)
    @skip_if_d_choke_percent_zero
    def validate_d_choke_percent_adapt(cls, value, values):
        """
        Валидатор для процента раскрытия штуцера.

        Args:
            cls(FMMTask): Класс.
            value(float): Значение процентов открытия штуцера.

        Returns:
            value: Проверенное значение.

        Raises:
            ValueError: Если значение процента открытия штуцера None или
        меньше нуля.
        """
        if value is None:
            raise ValueError(
                "d_choke_percent_adapt has undefined values")
        if value < 0:
            raise ValueError(
                "d_choke_percent_adapt must be greater than 0")
        return value

    @validator("c_choke_adapt", each_item=True)
    @skip_if_d_choke_percent_zero
    def validate_c_choke_adapt(cls, value, values):
        """
        Валидатор для коэффициента адаптации штуцера.

        Args:
            cls(FMMTask): Класс.
            value(float): Значение коэффициента адаптации штуцера.

        Returns:
            value: Проверенное значение.

        Raises:
            ValueError: Если коэффициент адаптации штуцера None или
        меньше нуля.
        """
        if value is None:
            raise ValueError(
                "c_choke_adapt has undefined values")
        return value

    class Config:
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
                        "d_choke_percent": 50,
                        "gas_condensate_factor": 8000,
                        "wct": 0,
                        "p_out": 7092750,
                        "p_buf": 8106000,
                        "t_buf": 353.15,
                        "t_out": 323.15,
                        "d_choke_percent_adapt": list(range(0, 110, 10)),
                        "c_choke_adapt": [1] * 11,
                        "q_gas": 224.8,
                        "q_gc": 0.027,
                        "q_wat": 0,
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


class FMMTaskSolution(BaseModel):
    """Модель для описания решения задачи физ-мат расчета."""

    q_gas: float = Field(
        ...,
        description="Дебит газа, м3/с",
    )
    q_gc: float = Field(
        ...,
        description="Дебит газоконденсата, м3/с",
    )
    q_wat: float = Field(
        ...,
        description="Дебит воды, м3/с",
    )
    error_gas: Optional[float] = Field(
        ...,
        description="Относительная ошибка по газу, %",
    )
    error_gc: Optional[float] = Field(
        ...,
        description="Относительная ошибка по газоконденсату, %",
    )
    error_wat: Optional[float] = Field(
        ...,
        description="Относительная ошибка по воде, %",
    )
