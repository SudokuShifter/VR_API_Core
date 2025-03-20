"""Функции для расчета коэффициента адаптации в зависимости от дебита газа."""

from copy import deepcopy

import pmf.equipment.choke as ch
import scipy.optimize as opt


def adapt_func_choke(
    c_choke: float,
    choke: ch.Choke,
    p_in: float,
    t_in: float,
    p_out: float,
    t_out: float,
    q_gas: float,
    rp: float,
    wct: float,
    coef_type: str,
) -> float:
    """
    Адаптация штуцера.

    Parameters:
     - c_choke (float): Адаптационный коэффициент штуцера
     - choke (Type[Choke]): Объект штуцера
     - p_in (float): Давление на входе в штуцер, Па абс.
     - t_in (float): Температура на входе в штуцер, К
     - p_out (float): Давление на выходе из штуцера, Па абс.
     - t_out (float): Температура на выходе из штуцера, К
     - q_gas (float): Дебит газа, м3/с
     - wct (float): Обводненность, д. ед.
     - rp (float): Газовый фактор, м3/м3
     - coef_type (str): Коэфициент адаптации

    Return:
     - (float):Ошибка в расчетном q_gas, м3/с
    """
    _choke = deepcopy(choke)

    coef_mapping = {
        "internal": dict(c_choke=c_choke, multiplier=1),
        "external": dict(c_choke=1, multiplier=c_choke),
    }

    coefs = coef_mapping.get(coef_type, None)
    if coefs is None:
        raise KeyError("Адаптационная функция указанного типа не существует.")
    q_liq_calc = _choke.calc_qliq(
        p_in=p_in,
        t_in=t_in,
        p_out=p_out,
        t_out=t_out,
        wct=wct,
        c_choke=coefs.get("c_choke"),
        explicit=True,
    ) * coefs.get("multiplier")

    q_gas_calc = q_liq_calc * (1 - wct) * rp

    return q_gas - q_gas_calc


def adapt_choke_coef(
    choke: ch.Choke,
    p_in: float,
    t_in: float,
    p_out: float,
    t_out: float,
    q_gas: float,
    rp: float,
    wct: float,
    coef_type: str = "internal",
) -> float:
    """
    Адаптирует штуцер и возвращает адаптационный коэффициент.

    Parameters:
     - args (tuple): Кортеж значений для адаптации

    Return:
     - (float): Адаптационный коэффициент штуцера
    """
    args = (
        choke,
        p_in,
        t_in,
        p_out,
        t_out,
        q_gas,
        rp,
        wct,
        coef_type,
    )
    try:
        c_ch = opt.brentq(
            adapt_func_choke,
            a=1e-7,
            b=5.0,
            args=args,
            xtol=1e-7,
        )
    except ValueError:
        c_ch = opt.minimize_scalar(
            lambda c_choke, args, adapt_func: abs(adapt_func(c_choke, *args)),
            method="bounded",
            bounds=(1e-7, 5),
            args=(args, adapt_func_choke),
        )
        c_ch = c_ch.x
    return c_ch
