import logging
from logging.handlers import RotatingFileHandler
from pathlib import Path

BASE_PATH = Path(__file__).parent.parent.parent
DEFAULT_LOG_PATH = BASE_PATH / "logs"
LOG_FILE_NAME = "out.log"
DEFAULT_LOG_PATH.mkdir(exist_ok=True)
ENCODING = "utf-8"


class LastLogValuesHandler(logging.Handler):
    def __init__(self):
        super().__init__()
        self.last_log_value = None

    def emit(self, record):
        log_value = self.format(record)
        self.last_log_value = log_value


def get_logger(logger_name, log_file_path=None, max_bytes=5000000,
               backup_count=10):
    """Initialize logger."""
    if log_file_path is None:
        log_file_path = DEFAULT_LOG_PATH / LOG_FILE_NAME
    logging.basicConfig()
    logger = logging.getLogger(logger_name)
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        "%(asctime)s - %(levelname)s - %(message)s")
    file_handler = RotatingFileHandler(
        log_file_path,
        maxBytes=max_bytes,
        backupCount=backup_count,
        encoding=ENCODING,
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)
    console_handler = logging.StreamHandler()
    last_logs_handler = LastLogValuesHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    logger.addHandler(last_logs_handler)
    return logger
