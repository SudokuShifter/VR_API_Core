import asyncio
from typing import AsyncGenerator, Tuple

import pandas as pd
import pytest
from httpx import AsyncClient, Timeout
import well_parser
from models.base import Columns, MAPPING_FILE_PATH


@pytest.fixture(scope="session")
def event_loop():
    policy = asyncio.get_event_loop_policy()
    loop = policy.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="session")
async def ac() -> AsyncGenerator[AsyncClient, None]:
    """
    Асинхронный клиент для работы с эндпоинтами
    """
    timeout = Timeout(10.0, read=None)
    async with AsyncClient(base_url="http://0.0.0.0:8081", verify=False, timeout=timeout) as ac:
        yield ac


@pytest.fixture(scope="session")
async def well_name():
    """
    Имя скважины, на которой проводятся тесты
    """
    yield "Скважина ЛА-554 "


@pytest.fixture(scope="session")
async def well_data(well_name: str, mapping_file_path: str, from_and_to: Tuple):
    """
    Well_data для работы с тестами
    """
    mapping_data = well_parser.read_mapping_data(mapping_file_path, 0)
    well_dataset = well_parser.create_dataset(mapping_data, [well_name])
    well_data = well_dataset[well_name]
    well_data = well_parser.fill_na(well_data)
    # well_data = well_data.filter(items=[col.value for col in Columns])
    well_data[Columns.timestamp.value] = pd.to_datetime(well_data[Columns.timestamp.value])
    well_data = well_parser.select_period(
        well_data, start_date=pd.to_datetime(from_and_to[0]), end_date=pd.to_datetime(from_and_to[1])
    )
    yield well_data[::30]


@pytest.fixture(scope="session")
async def from_and_to():
    """
    С какого по какой день фильтровать well_data
    """
    yield "01/30/2023 20:00:00", "09/30/2023 20:00:00"


@pytest.fixture(scope="session")
async def mapping_file_path():
    yield MAPPING_FILE_PATH
