from core.logger import get_logger
from fastapi import APIRouter, Depends, Body
from models import ValidateTask
from models.request import SolutionResponse
from solver import BaseSolver, get_base_solver_service

logger = get_logger("server")
validate_router = APIRouter()


@validate_router.post(
    "/calc_validate_task",
    response_model=SolutionResponse,
    responses=ValidateTask.Config.responses,
)
async def calc_validate_task(
        task: ValidateTask = Body(
            ...,
            examples=ValidateTask.Config.schema_extra["examples"]
        ),
        _solver: BaseSolver = Depends(get_base_solver_service),
) -> SolutionResponse:
    """Эндпоинт запуска решения задачи валидации."""
    solution, messages = await _solver.solve_task(task)
    return SolutionResponse(success=True,
                            message=messages,
                            solution=solution.dict())
