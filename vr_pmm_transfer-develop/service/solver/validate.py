from typing import Tuple, List

import pandas as pd
import numpy as np
from core.logger import get_logger
from handlers.error_handler import error_handler
from models.validate import ValidateTask, ValidateTaskSolution
from solver.solver_protocol import SolverProtocol

__all__ = ["ValidateSolver"]

logger = get_logger("ValidateSolver")


class ValidateSolver(SolverProtocol):
    """
    Решает задачу валидации.

    Методы:
    - solve_task: Решает задачу валидации и возвращает
    соответствующее решение.
    """

    def __init__(self):
        self.messages = []

    @error_handler(logger, (Exception,),
                   "Задача валидации выполнена успешно.")
    async def solve_task(
            self,
            task: ValidateTask,
    ) -> Tuple[ValidateTaskSolution, List[str]]:
        """
        Решает задачу валидации, вычисляя медианные значения.

        Параметры:
        - task (ValidateTask): Задача валидации.

        Возвращает:
        ValidateTaskSolution: Решение задачи валидации с медианными
        значениями.
        """
        dataframe = await self.calc_gc_and_wct(task)

        return ValidateTaskSolution(
            wct=dataframe["Обводненность"].median(),
            gas_condensate_factor=dataframe["Газовый фактор"].median(),
        ), self.messages

    @staticmethod
    async def calc_gc_and_wct(task: ValidateTask):
        """
        Расчет значений обводненности и газоконденсатного фактора.

        Параметры:
        - task (ValidateTask): Задача валидации, содержащая данные для
        расчета.

        Возвращает:
        pd.DataFrame: DataFrame
        со значениями для обводненности и газоконденсатного
        фактора.
        """
        df = pd.DataFrame(
            {
                "Расход по воде Вентури": task.q_wat_timed,
                "Расход по конденсату Вентури": task.q_gc_timed,
                "Расход по газу Вентури": task.q_gas_timed,
            }
        )

        gc_flow_vent = df["Расход по конденсату Вентури"]
        wat_flow_vent = df["Расход по воде Вентури"]

        df["Газовый фактор"] = np.where(
            (gc_flow_vent == 0),
            0,
            df["Расход по газу Вентури"] / gc_flow_vent)
        df["Обводненность"] = np.where(
            gc_flow_vent + wat_flow_vent == 0,
            0,
            wat_flow_vent / (gc_flow_vent + wat_flow_vent))
        return df
