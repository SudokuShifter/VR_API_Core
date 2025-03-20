from typing import Optional, Tuple, Any

from core.logger import get_logger
from handlers.error_handler import error_handler
from models.fmm import FMMTask, FMMTaskSolution
from scipy.interpolate import interp1d
from solver.choke import Choke, init_choke
from solver.solver_protocol import SolverProtocol

__all__ = ["FMMSolver"]

logger = get_logger("FMMSolver")


class FMMSolver(SolverProtocol):
    """Сервис решения задачи расчета расхода на штуцере."""

    def __init__(self):
        self.messages = []

    @error_handler(logger, (Exception,),
                   "Задача физ-мат расчета выполнена успешно.")
    async def solve_task(
            self,
            task: FMMTask,
    ) -> tuple[FMMTaskSolution, list[Any]]:
        """Решение задачи расчета расхода на штуцере."""
        await self.check_input_data(task)

        if task.d_choke_percent == 0:
            return FMMTaskSolution(
                q_gas=0,
                q_gc=0,
                q_wat=0,
                error_gas=0,
                error_gc=0,
                error_wat=0,
            ), self.messages

        choke = init_choke(
            d_up=task.d_tube,
            gamma_gas=task.gamma_gas,
            gamma_wat=task.gamma_wat,
            gamma_oil=task.gamma_gc,
            wct=task.wct,
            rp=task.gas_condensate_factor,
            d_choke_percent=task.d_choke_percent,
        )
        q_gas, q_gc, q_wat = await self.process_well_data(task, choke)

        return FMMTaskSolution(
            q_gas=q_gas,
            q_gc=q_gc,
            q_wat=q_wat,
            error_gas=await self.calculate_deviation_percent(q_gas,
                                                             task.q_gas),
            error_gc=await self.calculate_deviation_percent(q_gc,
                                                            task.q_gc),
            error_wat=await self.calculate_deviation_percent(q_wat,
                                                             task.q_wat),
        ), self.messages

    async def process_well_data(
            self,
            task: FMMTask,
            choke: Choke,
    ) -> Tuple[float, float, float]:
        """
        Обрабатывает данные скважины и возвращает словарь для сохранения результатов в JSON.

        Parameters:
        - task (FMMTask): Исходные данные для расчета задачи
        - choke (Type[Choke]): Экземпляр класса Choke

        Returns:
        Tuple[List[float], List[float]]: Кортеж, содержащий два списка:
        - значения диаметра штуцера (d_choke_adapt).
        - значения адаптационного коэффициента от диаметра штуцера (c_choke_adapt).
        - значения адаптационного коэффициента от диаметра штуцера (c_choke_adapt).
        """

        linear_cof = interp1d(task.d_choke_percent_adapt,
                              task.c_choke_adapt, kind="linear",
                              fill_value="extrapolate")
        choke.create_choke(task.d_choke_percent,
                           task.wct,
                           task.gas_condensate_factor)

        c_choke = linear_cof(task.d_choke_percent)
        if c_choke <= 0:
            self.messages.append(
                "При расчете дебитов получен отрицательный либо равный "
                "нулю коэффициент адаптации, проверьте входные данные."
            )
            return 0, 0, 0
        q_liq_calc = choke.choke.calc_qliq(
            p_in=task.p_buf,
            t_in=task.t_buf,
            p_out=task.p_out,
            t_out=task.t_out,
            wct=task.wct,
            c_choke=c_choke,
            explicit=True,
        )
        q_gas_calc = q_liq_calc * (
                1 - task.wct) * task.gas_condensate_factor
        q_wat_calc = q_liq_calc * task.wct
        q_gc_calc = q_liq_calc * (1 - task.wct)
        return q_gas_calc, q_gc_calc, q_wat_calc

    @staticmethod
    async def calculate_deviation_percent(
            calc_value: float,
            fact_value: float,
    ) -> Optional[float]:
        """
        Рассчитывает относительное отклонение в процентах.

        Parameters:
        - calc_value (float): Рассчитанное значение.
        - fact_value (float): Фактическое значение.

        Returns:
            float: Относительное отклонение, %
        """
        if all(value is not None for value in
               (calc_value, fact_value)):
            if fact_value == 0:
                return 0
            return abs(calc_value - fact_value) / fact_value * 100
        return None

    async def check_input_data(self, task: FMMTask) -> None:
        """
        Проверяет входные данные задачи физ-мат расчета.

        Parameters:
        - task (FMMTask): Задача адаптации штуцера.

        Raises:
        - None

        Warns:
        - logger.warning: Если не указаны расходы воды, газоконденсата и
        газа, то расчет ошибки не будет выполнен.
        """
        missing_data = []

        if task.q_gc is None:
            missing_data.append("расход по конденсату (q_gc)")

        if task.q_gas is None:
            missing_data.append("расход по газу (q_gas)")

        if task.q_wat is None:
            missing_data.append("расход по воде (q_wat)")

        if missing_data:
            error_message = (
                f"Не указаны следующие расходы: {', '.join(missing_data)}. "
                "Оценка ошибки расчета не выполнена."
            )
            self.messages.append(error_message)
            logger.warning(error_message)
