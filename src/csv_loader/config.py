from pydantic import Field
from pydantic_settings import BaseSettings


class InfluxDBConfig(BaseSettings):
    DB_ORG: str = Field(...)
    DB_URL: str = Field(...)
    DB_TOKEN: str = Field(...)
