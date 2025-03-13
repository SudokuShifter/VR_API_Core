import os

from dotenv import load_dotenv
from dependency_injector import (
    containers,
    providers
)

from influx_api.config import InfluxDBConfig, RequestDBConfig


load_dotenv()


class ConfigContainer(containers.DeclarativeContainer):
    influxdb_config = providers.Factory(
        InfluxDBConfig,
        DB_ORG=os.getenv('DB_ORG'),
        DB_URL=os.getenv('DB_URL'),
        DB_TOKEN=os.getenv('DB_TOKEN'),
        DB_BUCKET_NAME=os.getenv('DB_BUCKET_NAME'),
    )

class RequestContainer(containers.DeclarativeContainer):
    request_manager = providers.Factory(
        RequestDBConfig,
        FULL_DATA_BY_TAG='from(bucket: "test/autogen") '
                          '|> range(start: -5y)'
                          '|> filter(fn: (r) => r._measurement == "indicator" and r.ind_tag == "{:0}")',
        DATA_FOR_RANGE_BY_TAG='from(bucket: "test/autogen") '
                          '|> range(start: {:0}, stop: {:1})'
                          '|> filter(fn: (r) => r._measurement == "indicator" and r.ind_tag == "{:2}")',
        DATA_BEFORE_DATE='from(bucket: "test/autogen") '
                          '|> range(start: -5y, stop: {:0})'
                          '|> filter(fn: (r) => r._measurement == "indicator" and r.ind_tag == "{:1}")',
        DATA_AFTER_DATE='from(bucket: "test/autogen") '
                          '|> range(start: {:0})'
                          '|> filter(fn: (r) => r._measurement == "indicator" and r.ind_tag == "{:1}")',
        WRITE_IN_TAG_BY_DATE=''
    )


