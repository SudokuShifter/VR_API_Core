from typing import Union

from models.adapt import AdaptTask, AdaptTaskSolution
from models.base import BaseTask, BaseTaskSolution
from models.fmm import FMMTask, FMMTaskSolution
from models.validate import ValidateTask, ValidateTaskSolution

TASKS = [AdaptTask, FMMTask, ValidateTask]
# Типы возможных моделей задач
TASK_TYPE = Union[AdaptTask, FMMTask, ValidateTask]

SOLUTIONS = [AdaptTaskSolution, FMMTaskSolution, ValidateTaskSolution]
# Типы возможных моделей решения задач
SOLUTION_TYPE = Union[AdaptTaskSolution, FMMTaskSolution, ValidateTaskSolution]

__all__ = [
    "TASKS",
    "TASK_TYPE",
    "SOLUTIONS",
    "SOLUTION_TYPE",
    "BaseTask",
    "BaseTaskSolution",
    "AdaptTask",
    "AdaptTaskSolution",
    "FMMTask",
    "FMMTaskSolution",
    "ValidateTask",
    "ValidateTaskSolution",
]
