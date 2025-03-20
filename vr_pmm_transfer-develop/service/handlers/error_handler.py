from functools import wraps
from http import HTTPStatus
from logging import Logger
from typing import Any, Callable, Tuple, Type

from fastapi import HTTPException


def error_handler(
        logger: Logger,
        handle_errors: Tuple[Type[Exception]],
        on_success: str,
) -> Callable:
    """
    Декоратор для обработки ошибок в асинхронных функциях.

    Parameters:
        logger (Logger): Экземпляр логгера для записи ошибок и сообщений об успехе.
        handle_errors (Tuple[Type[Exception]]): Кортеж типов исключений,
        которые нужно обрабатывать.
        on_success (str): Сообщение об успехе для записи в лог при успешном
        выполнении функции.

    Returns:
        Callable: Обернутая функция.
    """

    def wrapped(func: Callable) -> Callable:
        @wraps(func)
        async def inner(*args, **kwargs) -> Any:
            """
            Внутренняя функция, оборачивающая оригинальную асинхронную функцию.

            Parameters:
                *args: Позиционные аргументы, передаваемые в обернутую функцию.
                **kwargs: Именованные аргументы, передаваемые в обернутую функцию.

            Returns:
                Any: Результат обернутой функции.

            Raises:
                HTTPException: Если происходит одна из указанных ошибок во
                время выполнения.
            """
            try:
                results, messages = await func(*args, **kwargs)
                messages.append(on_success)
                logger.info(on_success)
                return results, messages
            except handle_errors as exc:
                error_message = str(exc)
                logger.error(error_message)
                raise HTTPException(
                    status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                    detail=error_message,
                )

        return inner

    return wrapped
