import os
from dotenv import load_dotenv

from dependency_injector import containers, providers
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS

from csv_loader.config import InfluxDBConfig


class ConfigContainer(containers.DeclarativeContainer):
    influxdb_config = providers.Factory(
        InfluxDBConfig,
        DB_ORG=os.getenv('DB_ORG'),
        DB_URL=os.getenv('DB_URL'),
        DB_TOKEN=os.getenv('DB_TOKEN')
    )
