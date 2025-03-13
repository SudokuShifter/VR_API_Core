from pydantic import Field
from pydantic_settings import BaseSettings



class InfluxDBConfig(BaseSettings):
    DB_ORG: str = Field(...)
    DB_URL: str = Field(...)
    DB_TOKEN: str = Field(...)
    DB_BUCKET_NAME: str = Field(...)


class RequestDBConfig(BaseSettings):
    FULL_DATA_BY_TAG: str = Field(...)
    DATA_FOR_RANGE_BY_TAG: str = Field(...)
    DATA_BEFORE_DATE: str = Field(...)
    DATA_AFTER_DATE: str = Field(...)
    WRITE_IN_TAG_BY_DATE: str = Field(...)
