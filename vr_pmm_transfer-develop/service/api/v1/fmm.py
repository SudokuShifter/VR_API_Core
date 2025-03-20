from core.logger import get_logger
from fastapi import APIRouter, Depends, Body
from models import FMMTask, FMMTaskSolution
from models.request import SolutionResponse
from solver import BaseSolver, get_base_solver_service

logger = get_logger("server")
fmm_router = APIRouter()


@fmm_router.post(
    "/calc_fmm_task",
    response_model=SolutionResponse,
    responses=FMMTask.Config.responses,
)
async def calc_fmm_task(
        task: FMMTask = Body(
            ...,
            examples=FMMTask.Config.schema_extra['examples']
        ),
        _solver: BaseSolver = Depends(get_base_solver_service),
) -> SolutionResponse:
    """Эндпоинт запуска решения задачи расчета расхода на штуцере"""
    solution, messages = await _solver.solve_task(task)
    return SolutionResponse(success=True,
                            message=messages,
                            solution=solution.dict())
