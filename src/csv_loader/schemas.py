from datetime import datetime
from typing import Optional

from pydantic import Field

from schemas import CoreModel


class RequestDataWithDateRangeSchema(CoreModel):
    date_start: Optional[datetime] = Field(...,)
    date_end: Optional[datetime] = Field(...)


class RequestDataWithIDSchema(CoreModel):
    ind_id: str = Field(...)