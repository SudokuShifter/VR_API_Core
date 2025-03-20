from solver.choke.choke_model import Choke


def init_choke(
        d_up: float,
        gamma_gas: float,
        gamma_wat: float,
        gamma_oil: float,
        d_choke_percent: float,
        wct: float,
        rp: float,
) -> Choke:
    """
    Создает экземпляр модели штуцера.

    Parameters:
    - d_up (float): Диаметр канала над штуцером, м
    - gamma_gas (float): Удельная масса газа, кг/м3
    - gamma_wat (float): Удельная масса воды, кг/м3
    - gamma_oil (float): Удельная масса нефти, кг/м3
    - d_choke_percent (float): Процент открытия штуцера, %
    - wct (float): Обводненость, д.ед.
    - rp (float): Газовый фактор, м3/м3
    канала к диаметру штуцера

    Returns:
    Type[Choke]: Экземпляр модели штуцера.
    """
    return Choke(
        gamma_gas=gamma_gas,
        gamma_wat=gamma_wat,
        gamma_oil=gamma_oil,
        wct=wct,
        rp=rp,
        d_up=d_up,
        d_choke_percent=d_choke_percent,
    )
