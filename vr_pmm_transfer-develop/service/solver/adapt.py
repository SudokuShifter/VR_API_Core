from typing import Any, List, Tuple

import pandas as pd
from core.logger import get_logger
from handlers.error_handler import error_handler
from models.adapt import AdaptTask, AdaptTaskSolution
from solver.choke import Choke, adapt_choke_coef, init_choke
from solver.solver_protocol import SolverProtocol

__all__ = ["AdaptSolver"]

logger = get_logger("AdaptSolver")


class AdaptSolver(SolverProtocol):
    """Сервис решения задачи расчета кривой адаптации штуцера."""

    def __init__(self):
        self.messages = []

    @error_handler(logger, (Exception,),
                   "Задача адаптации выполнена успешно.")
    async def solve_task(
            self,
            task: AdaptTask) -> tuple[AdaptTaskSolution, list[Any]]:
        """Решение задачи расчета кривой адаптации штуцера."""
        await self.check_input_data(task)

        task_data = pd.DataFrame(task.dict(
            exclude={"d_tube", "gamma_gas", "gamma_wat", "gamma_gc"}))

        task_data = await self.calc_gc_and_wct(task_data)

        choke = init_choke(
            d_up=task.d_tube,
            gamma_gas=task.gamma_gas,
            gamma_wat=task.gamma_wat,
            gamma_oil=task.gamma_gc,
            wct=task_data["wct_timed"].iloc[0],
            rp=task_data["gas_condensate_factor_timed"].iloc[0],
            d_choke_percent=task.d_choke_percent_timed[0],
        )

        d_choke, c_choke = await self.adapt_choke(task_data, choke)
        d_choke, c_choke = await self.fiter_adaptation(
            d_choke, c_choke)
        return AdaptTaskSolution(
            d_choke_percent_adapt=d_choke,
            c_choke_adapt=c_choke,
        ), self.messages

    @staticmethod
    async def adapt_choke(
            task_data: pd.DataFrame,
            choke: Choke,
    ) -> Tuple[List[float], List[float]]:
        """
        Адаптирует параметры штуцера на основе данных о потоках в скважине.

        Parameters:
        - task_data (AdaptTask): Исходные данные для расчета задачи
        - choke (Type[Choke]): Экземпляр класса Choke

        Returns:
        Tuple[List[float], List[float]]: Кортеж, содержащий два списка:
        - значения диаметра штуцера (d_choke_adapt).
        - значения адаптационного коэффициента от диаметра штуцера (c_choke_adapt).

        Использует данные о давлении, температуре, дебите(объемном расходе) газа, конденсата и воды для адаптации
        параметров штуцера с учетом газовых свойств.
        """

        def calc_c_choke(row, choke) -> float:
            """
            Рассчитывает коэффициент адаптации к газу.

            Parameters:
             - row: Строка Dataframe
             - choke (Type[Choke]): Модель штуцера
            Returns:
            - c_choke (float): Адаптационный коэффициент штуцера
            """
            p_out = row["p_out_timed"]
            p_in = row["p_buf_timed"]
            t_out = row["t_out_timed"]
            t_in = row["t_buf_timed"]
            q_gas = row["q_gas_timed"]
            d_choke_percent = row["d_choke_percent_timed"]
            wct = row["wct_timed"]
            gas_condensate_factor = row["gas_condensate_factor_timed"]

            choke.create_choke(d_choke_percent, wct, gas_condensate_factor)
            c_choke = adapt_choke_coef(
                choke.choke,
                p_in,
                t_in,
                p_out,
                t_out,
                q_gas,
                gas_condensate_factor,
                wct,
            )
            return max(0, c_choke)

        percent_cond = task_data["d_choke_percent_timed"] > 0.0
        flow_cond = (task_data["q_gc_timed"] + task_data[
            "q_wat_timed"]) > 0.0
        wct_cond = task_data["wct_timed"] <= 1

        filtered_data = task_data[percent_cond & flow_cond & wct_cond]

        d_choke = list(filtered_data["d_choke_percent_timed"])
        c_choke = list(
            filtered_data.apply(lambda row: calc_c_choke(row, choke),
                                axis=1, result_type="expand"))
        return d_choke, c_choke

    @staticmethod
    async def fiter_adaptation(
            d_choke_adapt: List[float],
            c_choke_adapt: List[float],
    ) -> Tuple[List[float], List[float]]:
        """
        Сортирует данные и удаляет дубликаты.

        Parameters:
        - d_choke_adapt (List[float]): Список значений диаметра штуцера.
        - c_choke_adapt (List[float]): Список значений коэффициента адаптации.

        Returns:
        - d_choke_adapt (List[float]): Список отфильтрованных значений диаметра штуцера.
        - c_choke_adapt (List[float]): Список отфильтрованных значений коэффициента адаптации.
        """

        df = pd.DataFrame({"d": d_choke_adapt, "c": c_choke_adapt})
        df = df.sort_values(by="d", ascending=True)
        df = df.loc[df["d"] > df["d"].shift(fill_value=0)]
        return df["d"].to_list(), df["c"].to_list()

    async def check_input_data(self, task: AdaptTask) -> None:
        """
        Проверяет входные данные задачи адаптации штуцера.

        Parameters:
        - task (AdaptTask): Задача адаптации штуцера.

        Raises:
        - None

        Warns:
        - logger.warning: Если калибровка штуцера будет выполнена не в
        полном диапазоне.
        """
        lower_d_choke_bound = 0
        upper_d_choke_bound = 100
        d_choke_percent = task.d_choke_percent_timed
        min_d_choke = min(d_choke_percent)
        max_d_choke = max(d_choke_percent)
        if min_d_choke > lower_d_choke_bound or max_d_choke < upper_d_choke_bound:
            message = (f"Калибровка штуцера будет выполнена не в "
                       f"полном диапазоне: {min_d_choke}% - {max_d_choke}%.")
            self.messages.append(message)
            logger.warn(message)

    @staticmethod
    async def calc_gc_and_wct(task: pd.DataFrame):
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

        task["gas_condensate_factor_timed"] = task["q_gas_timed"] / task[
            "q_gc_timed"]
        task["wct_timed"] = task["q_wat_timed"] / (
                task["q_gc_timed"] + task["q_wat_timed"]
        )
        return task
