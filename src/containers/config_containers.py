import os

from dotenv import load_dotenv
from dependency_injector import (
    containers,
    providers
)

from csv_loader.config import InfluxDBConfig


load_dotenv()


class ConfigContainer(containers.DeclarativeContainer):
    influxdb_config = providers.Factory(
        InfluxDBConfig,
        DB_ORG=os.getenv('DB_ORG'),
        DB_URL=os.getenv('DB_URL'),
        DB_TOKEN=os.getenv('DB_TOKEN'),
        DB_BUCKET_NAME=os.getenv('DB_BUCKET_NAME'),
    )
