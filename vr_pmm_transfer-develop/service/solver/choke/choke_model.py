import pmft.pvt.adapter as fl
from pmf.equipment.choke import Choke as ChokeModel

DC = 100.001


class Choke:
    """Модель штуцера."""

    def __init__(
            self,
            gamma_gas: float,
            gamma_wat: float,
            gamma_oil: float,
            d_up: float,
            d_choke_percent: float,
            wct: float,
            rp: float,
    ) -> None:
        """
        Инициализация модели штуцера.

        Параметры:
        - gamma_gas (float): Удельная плотность газа, кг/м3
        - gamma_wat (float): Удельная плотность воды, кг/м3
        - gamma_oil (float): Удельная плотность нефти, кг/м3
        - d_up (float): Диаметр канала выше по потоку, м
        - d_choke_percent (float): Процент открытия штуцера, %
        - wct (float): Обводненость, д.ед.
        - rp (float): Газовый фактор, м3/м3
        """
        self.choke = None
        self.fluid_data = None
        self.gamma_gas = gamma_gas
        self.gamma_wat = gamma_wat
        self.gamma_oil = gamma_oil
        self.d_up = d_up

        self.create_choke(d_choke_percent, wct, rp)

    def create_fluid_data(self, wct: float, rp: float) -> None:
        """
        Создание данных о жидкости на основе коэффициента обводнения и ГФ.

        Параметры:
        - wct (float): Обводненость, д.ед.
        - rp (float): Газовый фактор, м3/м3
        """
        self.fluid_data = {
            "q_fluid": 100 / 86400,
            "pvt_model_data": {
                "black_oil": {
                    "gamma_gas": self.gamma_gas,
                    "gamma_wat": self.gamma_wat,
                    "gamma_oil": self.gamma_oil,
                    "wct": wct,
                    "phase_ratio": {"type": "GOR", "value": rp},
                    "oil_correlations": {
                        "pb": "Standing",
                        "rs": "Standing",
                        "rho": "Standing",
                        "b": "Standing",
                        "mu": "Beggs",
                        "compr": "Vasquez",
                        "hc": "const",
                    },
                    "gas_correlations": {
                        "ppc": "Standing",
                        "tpc": "Standing",
                        "z": "Dranchuk",
                        "mu": "Lee",
                        "hc": "const",
                    },
                    "water_correlations": {
                        "b": "McCain",
                        "compr": "Kriel",
                        "rho": "Standing",
                        "mu": "McCain",
                        "hc": "const",
                    },
                    "table_model_data": None,
                    "use_table_model": False,
                }
            },
        }

    def create_choke(self, d_choke_percent: float, wct: float, rp: float) -> None:
        """
        Создание модели штуцера на основе данных о жидкости и параметров.

        Параметры:
        - d_choke_percent (float): Процент открытия штуцера, %
        - wct (float): Обводненость, д.ед.
        - rp (float): Газовый фактор, м3/м3
        """
        self.create_fluid_data(wct, rp)
        self.choke = ChokeModel(
            h_mes=0,
            d=d_choke_percent / DC * self.d_up,
            d_up=self.d_up,
            fluid=fl.FluidFlow(**self.fluid_data),
        )
