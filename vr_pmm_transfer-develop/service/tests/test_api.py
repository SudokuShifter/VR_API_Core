import random

from httpx import AsyncClient
import pandas as pd
import json

from models.validate import ValidateTask, ValidateTaskSolution
from models.adapt import AdaptTask, AdaptTaskSolution
from models.fmm import FMMTask, FMMTaskSolution
from models import limits
from models.base import Columns


class TestAPI:
    @staticmethod
    async def test_validate_service(ac: AsyncClient, well_data: pd.DataFrame) -> None:
        """
        Тест эндпоинта запуска решения задачи валидации
        """
        validate_task = ValidateTask.create_validate_task(well_data)
        response = await ac.post('/v1/validate/calc_validate_task', data=validate_task.json())
        assert response.status_code == 200
        assert response.json()['success'] == True

        validate_solution = ValidateTaskSolution(**response.json()['solution'])

        new_df = pd.DataFrame(validate_solution.dict())
        old_df = pd.read_json('tests/data/validate_solution.json')
        ValidateTaskSolution(**old_df.to_dict(orient='list'))

        assert list(new_df.columns) == list(old_df.columns)

        for col in new_df.columns:
            assert len(new_df[col]) == len(old_df[col])
            assert new_df[col][0] == old_df[col][0]
            assert new_df[col][len(new_df[col]) - 1] == old_df[col][len(old_df[col]) - 1]

    @staticmethod
    async def test_fmm_service(ac: AsyncClient, well_data: pd.DataFrame) -> None:
        """
        Тест эндпоинта запуска решения задачи валидации
        """
        timestamp_list, q_gas_fact_list, q_gas_calc_list = [], [], []

        validate_solution = ValidateTaskSolution(**pd.read_json('tests/data/validate_solution.json').to_dict(orient='list'))
        adaptation_solution = AdaptTaskSolution(**pd.read_json('tests/data/adapt_solution.json').to_dict(orient='list'))

        for timestamp, d_choke_percent, p_buf, p_vent, t_buf, t_out, q_gc, q_gas, q_wat, wct, rp in zip(
                well_data["timestamp"].astype(str).tolist(),
                well_data["Процент открытия штуцера"],
                well_data["Давление над буферной задвижкой ФА"],
                well_data["Давление"],
                well_data["Температура"],
                well_data["Температура на выкидной линии"],
                well_data["Расход по конденсату Вентури"],
                well_data["Расход по газу Вентури"],
                well_data["Расход по воде Вентури"],
                validate_solution.wct_median_timed,
                validate_solution.gas_condensate_factor_median_timed,
        ):
            if q_gc + q_wat > 0 and q_gc > 0:

                fmm_task = FMMTask.create_fmm_data(d_choke_percent, rp, wct, p_buf, p_vent, t_buf, t_out, q_gc, q_gas, q_wat, adaptation_solution)
                response = await ac.post('/v1/fmm/calc_fmm_task', data=fmm_task.json())
                assert response.status_code == 200

                timestamp_list.append(timestamp)
                if response.json()['success'] == True:
                    fmm_solution = FMMTaskSolution(**response.json()['solution'])
                    q_gas_fact_list.append(q_gas)
                    q_gas_calc_list.append((fmm_solution.q_gas * 86400)/(24 * 1000))
                else:
                    q_gas_fact_list.append(0)
                    q_gas_calc_list.append(0)
            else:
                timestamp_list.append(timestamp)
                q_gas_fact_list.append(0)
                q_gas_calc_list.append(0)

        new_df = pd.DataFrame({"timestamp": pd.to_datetime(timestamp_list), "Расход по газу Вентури, м3/час": q_gas_fact_list, "Расчетный расход по газу, м3/час": q_gas_calc_list})
        old_df = pd.read_json('tests/data/fmm_solution.json')

        assert list(new_df.columns) == list(old_df.columns)

        for col in new_df.columns:
            assert len(new_df[col]) == len(old_df[col])
            if col == Columns.timestamp.value:
                assert new_df[col][0] == old_df[col][0]
            else:
                assert round(new_df[col][0], 5) == round(old_df[col][0], 5)
                assert round(new_df[col][10000], 5) == round(old_df[col][10000], 5)

