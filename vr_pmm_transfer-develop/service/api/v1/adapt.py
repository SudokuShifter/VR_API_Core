from core.logger import get_logger
from fastapi import APIRouter, Depends, Body
from models import AdaptTask, AdaptTaskSolution
from models.request import SolutionResponse
from solver import BaseSolver, get_base_solver_service

logger = get_logger("server")
adapt_router = APIRouter()


@adapt_router.post(
    "/calc_adapt_task",
    response_model=SolutionResponse,
    responses=AdaptTask.Config.responses,
)
async def calc_adapt_task(
        task: AdaptTask = Body(
            ...,
            examples=AdaptTask.Config.schema_extra['examples']
        ),
        _solver: BaseSolver = Depends(get_base_solver_service),
) -> SolutionResponse:
    """Эндпоинт запуска решения задачи расчета коэффициента адаптации."""

    solution, messages = await _solver.solve_task(task)
    return SolutionResponse(success=True,
                            message=messages,
                            solution=solution.dict())
