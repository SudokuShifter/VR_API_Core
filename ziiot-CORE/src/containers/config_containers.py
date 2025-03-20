import os

from dotenv import load_dotenv
from dependency_injector import (
    containers,
    providers
)

from influx_api.config import (
    InfluxDBConfig,
    RequestModelConfig,
    RequestObjectConfig
)


load_dotenv()


class ConfigContainer(containers.DeclarativeContainer):
    influxdb_config = providers.Factory(
        InfluxDBConfig,
        DB_ORG=os.getenv('DB_ORG'),
        DB_URL=os.getenv('DB_URL'),
        DB_TOKEN=os.getenv('DB_TOKEN'),
        DB_BUCKET_NAME=os.getenv('DB_BUCKET_NAME'),
    )

class RequestModelContainer(containers.DeclarativeContainer):
    FULL_BUCKET_NAME = os.getenv('FULL_BUCKET_NAME')
    request_model_manager = providers.Factory(
        RequestModelConfig,
        FULL_DATA_BY_TAG=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: -5y)'
                          '|> filter(fn: (r) => r._measurement == "{:0}" and r["{:1}"] == "{:2}")',
        DATA_FOR_RANGE_BY_TAG=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: {:0}, stop: {:1})'
                          '|> filter(fn: (r) => r._measurement == "{:2}" and r["{:3}"] == "{:4}")',
        DATA_BEFORE_DATE=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: -5y, stop: {:0})'
                          '|> filter(fn: (r) => r._measurement == "{:1}" and r["{:2}"] == "{:3}")',
        DATA_AFTER_DATE=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: {:0})'
                          '|> filter(fn: (r) => r._measurement == "{:1}" and r["{:2}"] == "{:3}")',
        WRITE_IN_TAG_BY_DATE='',
        OBJECTS_BY_MODEL_ID=f'from(bucket: "{FULL_BUCKET_NAME}") '
                            '|> range(start: -5y)'
                            '|> filter(fn: (r) => r._measurement == "{:0}")',
        DATA_BY_DATE=f'from(bucket: "{FULL_BUCKET_NAME}") '''
                     '|> range(start: {:0})'
                     '|> filter(fn: (r) => r._measurement == "{:0}" and r["{:2}"] == "{:3}")'
                     '|> hourSelection(start: 0, stop: 23)'
    )


class RequestObjectContainer(containers.DeclarativeContainer):
    FULL_BUCKET_NAME = os.getenv('FULL_BUCKET_NAME')
    request_object_manager = providers.Factory(
        RequestObjectConfig,
        TOTAL_QUERY=f'from(bucket: "{FULL_BUCKET_NAME}") '
                    '|> range(start: -5y)'
                    '|> filter(fn: (r) => r._measurement == "{:0}")'
                    '|> count()',
        DATA_QUERY=f'from(bucket: "{FULL_BUCKET_NAME}") '
                    '|> range(start: -5y)'
                    '|> filter(fn: (r) => r._measurement == "{:0}")'
                    '|> limit(n: 10, offset: 0)'
    )