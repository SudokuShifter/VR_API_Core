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
        DATA_FOR_VALIDATE=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: {:0}, stop: {:1})'
                          '|> filter(fn: (r) => r._measurement == "{:2}")'
                          '|> filter(fn: (r) => r.name_ind == "Расход по воде Вентури" or '
                              'r.name_ind == "Расход по газу Вентури" or '
                              'r.name_ind == "Расход по конденсату Вентури")',
        DATA_FOR_ADAPT_BY_RANGE=f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: {:0}, stop: {:1})'
                          '|> filter(fn: (r) => r._measurement == "{:2}" or r._measurement == "ТЛ1 Манифольд")'
                          '|> filter(fn: (r) => r.name_ind == "Расход по воде Вентури" or '
                              'r.name_ind == "Расход по газу Вентури" or '
                              'r.name_ind == "Расход по конденсату Вентури" or '
                              'r.name_ind == "Давление над буферной задвижкой ФА" or '
                              'r.name_ind == "Процент открытия штуцера" or '
                              'r.name_ind == "Температура на выкидной линии" or '
                              'r.name_ind == "Давление" or '
                              'r.name_ind == "Температура на трубке Вентури")',
        DATA_FOR_FMM_BY_TIME_POINT =f'from(bucket: "{FULL_BUCKET_NAME}") '
                          '|> range(start: {:0}, stop: {:1})'
                          '|> filter(fn: (r) => r._measurement == "{:2}" or r._measurement == "ТЛ1 Манифольд")'
                          '|> filter(fn: (r) => r.name_ind == "Расход по воде Вентури" or ' 
                              'r.name_ind == "Расход по газу Вентури" or ' 
                              'r.name_ind == "Расход по конденсату Вентури" or ' 
                              'r.name_ind == "Давление над буферной задвижкой ФА" or ' 
                              'r.name_ind == "Процент открытия штуцера" or ' 
                              'r.name_ind == "Температура на выкидной линии" or ' 
                              'r.name_ind == "Давление" or ' 
                              'r.name_ind == "Температура на трубке Вентури")'
                          '|> sort(columns: ["_time"], desc: false)'
                          '|> limit(n: 1)'
    )

