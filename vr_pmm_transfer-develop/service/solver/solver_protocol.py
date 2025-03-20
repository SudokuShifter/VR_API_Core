from abc import abstractmethod

from models import SOLUTION_TYPE, TASK_TYPE

__all__ = ["SolverProtocol"]


class SolverProtocol:
    """
    Протокол сервиса решения задач
    """

    @abstractmethod
    async def solve_task(self, task: TASK_TYPE) -> SOLUTION_TYPE:
        """
        Решение расчетных задач
        :param prepared_task:
        :type prepared_task:
        :param task:
        :type task:
        :return:
        :rtype:
        """
        raise NotImplementedError
