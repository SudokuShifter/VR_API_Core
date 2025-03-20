from functools import lru_cache

from core.logger import get_logger
from models import SOLUTION_TYPE, TASK_TYPE, AdaptTask, FMMTask, \
    ValidateTask
from solver.adapt import AdaptSolver
from solver.fmm import FMMSolver
from solver.solver_protocol import SolverProtocol
from solver.validate import ValidateSolver

__all__ = ["BaseSolver", "get_base_solver_service"]

logger = get_logger("BaseSolver")


class BaseSolver(SolverProtocol):
    """Сервис решения задач."""

    def __init__(self):
        self._fmm_solver = FMMSolver
        self._adapt_solver = AdaptSolver
        self._validate_solver = ValidateSolver

    async def solve_task(self, task: TASK_TYPE) -> SOLUTION_TYPE:
        """Решение расчетных задач."""
        solver_by_task = {
            FMMTask.__name__: self._fmm_solver,
            AdaptTask.__name__: self._adapt_solver,
            ValidateTask.__name__: self._validate_solver,
        }

        solver = solver_by_task.get(type(task).__name__)()
        if solver is None:
            raise TypeError(f"Отсутствует решатель задачи типа {type(task).__name__}.")

        solution = await solver.solve_task(task)
        return solution


@lru_cache()
def get_base_solver_service() -> BaseSolver:
    return BaseSolver()
