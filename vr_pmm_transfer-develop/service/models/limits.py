"""Модуль с границами параметров для внешних сервисов и константами."""

UPSTREAM_CHANNEL_DIAMETER = 0.0254 * 12  # диаметр канала выше по потоку
DEFAULT_GAS_GAMMA = 0.6271  # удельная плотность газа
DEFAULT_WATER_GAMMA = 1  # удельная плотность воды (значение по умолчанию 1)
DEFAULT_OIL_GAMMA = 0.761
kSM3_to_SM3_COEF = 1000
WINDOW = 30 * 24


class Fluid:
    """Флюид."""

    class Qliq:
        """Дебит жидкости, м3/с."""

        MIN = 0
        MAX = 20000 / 86400

    class Wct:
        """Обводненность, д.ед."""

        MIN = 0
        MAX = 1

    class Rp:
        """Газовый фактор, м3/м3."""

        MIN = 0

    class GammaGas:
        """Удельная плотность газа по воздуху, д.ед."""

        MIN = 0.4
        MAX = 2

    class GammaOil:
        """Удельная плотность нефти по воде, д.ед."""

        MIN = 0.6
        MAX = 1.1

    class GammaWater:
        """Удельная плотность воды по воде, д.ед."""

        MIN = 0.95
        MAX = 1.5

    class Pb:
        """Давление насыщения, Па."""

        MIN = 0
        MAX = 101325000

    class Tres:
        """Температура пласта, К."""

        MIN = 273.15
        MAX = 673.15

    class Muob:
        """Вязкость нефти при давлении насыщения, сПз."""

        MIN = 0.0001
        MAX = 300

    class Mugb:
        """Вязкость газа при давлении насыщения, сПз."""

        MIN = 0.0001
        MAX = 300

    class Bgb:
        """Объемный коэффициент газа при давлении насыщения."""

        MIN = 0.972
        MAX = 3

    class Bob:
        """Объемный коэффициент нефти при давлении насыщения."""

        MIN = 0.972
        MAX = 3

    class Rsb:
        """Газосодержание при давлении насыщения, м3/м3."""

        MIN = 0

    class Salinity:
        """Соленость воды, ppm."""

        MIN = 0

    class OilCorrelations:
        """Корреляции для свойств нефти."""

        class Pb:
            """Корреляции для давления насыщения."""

            VALUES = ["standing"]

        class Rs:
            """Корреляции для газосодержания."""

            VALUES = ["standing"]

        class Rho:
            """Корреляции для плотности нефти."""

            VALUES = ["standing"]

        class B:
            """Корреляции для объемного коэффициента нефти."""

            VALUES = ["standing"]

        class Mu:
            """Корреляции для вязкости нефти."""

            VALUES = ["beggs"]

        class Compr:
            """Корреляции для сжимаемости нефти."""

            VALUES = ["vasquez"]

    class GasCorrelations:
        """Корреляции для свойств газа."""

        class Ppc:
            """Корреляции для псевдокритического давления."""

            VALUES = ["standing"]

        class Tpc:
            """Корреляции для псевдокритической температуры."""

            VALUES = ["standing"]

        class Z:
            """Корреляции для z-фактора."""

            VALUES = ["dranchuk", "kareem"]

        class Mu:
            """Корреляции для вязкости газа."""

            VALUES = ["lee"]

    class WaterCorrelations:
        """Корреляции для свойств воды."""

        class B:
            """Корреляции для объемного коэффициента воды."""

            VALUES = ["mccain"]

        class Compr:
            """Корреляции для сжимаемости воды."""

            VALUES = ["kriel"]

        class Rho:
            """Корреляции для плотности воды."""

            VALUES = ["standing", "iapws"]

        class Mu:
            """Корреляции для вязкости воды."""

            VALUES = ["mccain", "iapws"]


class Pipeline:
    """Трубопровод."""

    class BottomDepth:
        """Глубина спуска."""

        MIN = 0

    class D:
        """Внутренний диаметр, м."""

        MIN = 0

    class Roughness:
        """Шероховатость, м."""

        MIN = 0

    class HydrCorr:
        """Гидравлическая корреляция."""

        VALUES = ["ansari", "beggsbrill", "dunsros", "gray", "hagedornbrown"]


class Pfl:
    """Линейное давление, Па."""

    MIN = 0
    MAX = 3 * 10132500


class Tfl:
    """Линейная температура, К."""

    MIN = 253.15
    MAX = 673.15


class Choke:
    """Штуцер."""

    class D:
        """Диаметр штуцера, м."""

        MIN = 0

    class Correlation:
        """Корреляции для расчета перепада давления штуцера."""

        VALUES = ["perkins"]
