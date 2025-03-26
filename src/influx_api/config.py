from pydantic import Field
from pydantic_settings import BaseSettings



class InfluxDBConfig(BaseSettings):
    DB_ORG: str = Field(...)
    DB_URL: str = Field(...)
    DB_TOKEN: str = Field(...)
    DB_BUCKET_NAME: str = Field(...)


class RequestModelConfig(BaseSettings):
    DATA_FOR_ADAPT_BY_RANGE: str = Field(...)
    DATA_FOR_VALIDATE: str = Field(...)
    DATA_FOR_FMM_BY_TIME_POINT: str = Field(...)


class RequestObjectConfig(BaseSettings):
    TOTAL_QUERY: str = Field(...)
    DATA_QUERY: str = Field(...)