"""Скрипт для обучения и валидации моделей на скважинах 1-го и 2-го листа с валидацией
суммарного прогнозируемого объема по сууммарному расходу на сепараторе.
Формирует сводные таблицы в формате Excel с метриками качества
и значимостью признаков по всем скважинам. По каждой скважине в отдельности
формируется набор графиков и файлы с метриками качества в формате Excel и json.
В строке 44 задается имя вложенной директории, в которую сохраняются
результаты всех экспериментов (переменная DEST_PATH).
В строке 50 задается имя поддиректории для сохранения текущего эксперимента (переменная subdir).
В цикле for well_id, value in dataset.items() необходимо выбрать нужные преобразования
исходных данных и далее в цикле for target in TARGETS определить список
используемых параметров (переменная features).
Для каждой скважины строятся свои модели XGBoost.
Временной ряд делится на train и test на основе дат, указанных в файле "settings.py"
(TRAIN_START, TRAIN_END, TEST_START, TEST_END).
По тестовым дням, список которых определен в файле "даты тестов.xlsx",
метрики считаются отдельно - по той модели, у которой дата теста приходится на валидационный отрезок.
Суммарный прогноз моделей по всем скважинам одного листа экселя сопоставляется
с показателем на сепараторе с этого же листа, поэтому запускать расчет
необходимо для каждого листа экселя по отдельности: или map_1 и dataset_1 или map_2 и dataset_2.
"""

from pathlib import Path
from collections import defaultdict
import numpy as np
np.random.seed(24)
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

from utils import (
    read_mapping_data, add_features,
    add_quadratic_features, fill_na,
    create_dataset,
    features_preprocessing,
    total_separator_analyses
)
from model_utils import run_experiment
from settings import (
    MAPPING_FILE, TARGETS,
    SQ_SQRT_FEATURES, SELECTED_FEATURES,
    TEST_START, TEST_END,
    use_only_high_var_days,
    BASE_FEATURES,
    SYNTETIC_FEATURES,
    TECH_LINE_NUMBER
)

import warnings
warnings.filterwarnings('ignore')
import os.path

MODEL_PATH  = Path('models') # Директория с моделями


TECH_LINE = TECH_LINE_NUMBER # номер технологической линии

if TECH_LINE == 1:
    map_1     = read_mapping_data(MAPPING_FILE, 0)
    # Датасеты по всем скважинам с 1-го и 2-го листа экселя "Data_2022-2023.xlsx"
    dataset_1 = create_dataset(map_1)
    dataset   = dataset_1

elif TECH_LINE == 2:
    map_2     = read_mapping_data(MAPPING_FILE, 1)
    dataset_2 = create_dataset(map_2)
    dataset   = dataset_2

else:
    exit()

# убираем пробелы из ключей
dataset = {key.strip(): value for key, value in dataset.items()}

#dataset = dataset_2
print(f'{len(dataset)} wells found in a dataset.')

scores_dict      = dict()
importances_dict = dict()

for target in TARGETS:
    scores_dict[target]      = defaultdict(list)
    importances_dict[target] = pd.DataFrame()

val_predictions = {'Расход по газу': [], 'Расход по конденсату': [], 'Расход по воде': []}
val_actuals     = {'Расход по газу': [], 'Расход по конденсату': [], 'Расход по воде': []}

pkl__val_predictions = {'Расход по газу': [], 'Расход по конденсату': [], 'Расход по воде': []}
pkl__val_actuals     = {'Расход по газу': [], 'Расход по конденсату': [], 'Расход по воде': []}

# wells_list = dataset.keys()
#dataset = dataset['Скважина ЛА-506']


# сохранение подготовленной выборки

# for well in dataset.keys():
#     df_excel = dataset[well]
#     print(well)
#     df_excel.to_csv(f'data/prepared_data/tl_{TECH_LINE}/{well}.csv', sep=';', index=False)
#
# exit()
    # слишком большйо файл для загрузки
    # filename = "tl1_1.xlsx"
    # if os.path.isfile(f'data/prepared_data/{filename}'):
    #     with pd.ExcelWriter(f'data/prepared_data/{filename}') as writer:
    #         df_excel.to_excel(writer, sheet_name=well)
    # else:
    #     df_excel.to_excel(f'data/prepared_data/{filename}', sheet_name=well)

# import pickle
#
# with open('dataset_tl1.pickle', 'wb') as handle:
#     pickle.dump(dataset, handle, protocol=pickle.HIGHEST_PROTOCOL)



# добавляем синтетические фичи
for wellname in dataset.keys():
    print(wellname)
    dataset[wellname] = add_features(dataset[wellname])
#exit()


for well_id, value in dataset.items():
    # if well_id != 'Скважина ЛА-522':
    #     continue


    print(f'Started processing {well_id}')
    data = value.copy()

    data = features_preprocessing(dataset, data, well_id, base_features = BASE_FEATURES.copy(), syntetic_features=SYNTETIC_FEATURES.copy())
    data = fill_na(data)  # Заполнение пропусков предыдущими значениями.
    # добавить в features
    # data = add_features(data)  # Разности давлений и температур
    # data = add_quadratic_features(data)  # Экспертно сформулированные фичи
    #exit()
    # убираем записи скважин простоя

    data = data[data['Процент открытия штуцера'] > 0]
    print("До удаления дублей:", data.shape[0])
    data = data.drop_duplicates(keep='first')
    print("После удаления дублей:", data.shape[0])
    result = dict()


    for target in TARGETS:
        filter_col = []
        if len(BASE_FEATURES) > 0 or len(SYNTETIC_FEATURES):
            filter_col = data.filter(like="Скважина", axis=1).columns.to_list()

        features = SELECTED_FEATURES + SQ_SQRT_FEATURES + filter_col
        print(f'Target: {target}')
        print(features)
        # создаем директорию для сохранения всех картинок и фото
        wellid_fullname     = f'{well_id.strip()}_ТЛ-{TECH_LINE}'
        model_data_dir      = MODEL_PATH / 'type'     / target / 'model_data'   / wellid_fullname
        model_config_dir    = MODEL_PATH / 'type'    / target / 'model_config' / wellid_fullname
        model_name_dir      = MODEL_PATH / 'type'   / target / 'model_name'   / wellid_fullname
        model_log_dir       = MODEL_PATH / 'type'  / target / 'model_log'

        model_data_dir      = Path(str(model_data_dir).replace(' ', '_'))
        model_config_dir    = Path(str(model_config_dir).replace(' ', '_'))
        model_name_dir      = Path(str(model_name_dir).replace(' ', '_'))
        model_log_dir       = Path(str(model_log_dir).replace(' ', '_'))
        if not model_data_dir.exists():
            model_data_dir.mkdir(parents=True, exist_ok=True)
        if not model_config_dir.exists():
            model_config_dir.mkdir(parents=True, exist_ok=True)
        if not model_name_dir.exists():
            model_name_dir.mkdir(parents=True, exist_ok=True)
        if not model_log_dir.exists():
            model_log_dir.mkdir(parents=True, exist_ok=True)



        mae_score, mape_score, importances, val_prediction, val_actual = run_experiment(
            data, features, target, model_data_dir, model_config_dir, model_name_dir , well_id, \
            use_only_high_var_days=use_only_high_var_days,
            tech_line_id = TECH_LINE)

        result[target] = {'features': features, 'mae_scores': mae_score, 'mape_scores': mape_score}

        scores_dict[target]['well'].extend([well_id])
        scores_dict[target]['MAE'].extend([mae_score])
        scores_dict[target]['MAPE'].extend([mape_score])

        importances_dict[target]['features'] = importances['features']
        importances_dict[target][f'{well_id}_importance'] = importances['importance']

        param = target[:-8]
        if val_prediction.shape[0] > 0 :
            val_predictions[param].append(val_prediction[param])
            pkl__val_predictions[param].append(val_prediction)

        if val_actual.shape[0] > 0 :
            val_actuals[param].append(val_actual[param])
            pkl__val_actuals[param].append(val_actual)

    results_df = pd.DataFrame()
    results_df = []
    for target in result.keys():
        print(result[target]['mae_scores'])
        results_df.append({f'{target}_MAE': result[target]['mae_scores'],f'{target}_MAPE': result[target]['mape_scores'] })
        # results_df[f'{target}_MAE'] = result[target]['mae_scores']
        # results_df[f'{target}_MAPE'] = result[target]['mape_scores']
    results_df = pd.DataFrame(results_df, index=[well_id])

    file_path = str(model_data_dir / f'scores_{well_id}.xlsx')
    results_df.to_excel(file_path)

    #break # делаем пока только по 1 скважины

#exit()
# Сохранение метрик и значимости признаков по всем скважинам в 3 экселя (сводная таблица).

# import pickle
# with open('results/val_predictions-p4_sqrt20220801-20230430__июнь-июль-ШтуцерНЕ0___TL1.pickle', 'wb') as handle:
#     pickle.dump(pkl__val_predictions, handle, protocol=pickle.HIGHEST_PROTOCOL)
#
# with open('results/val_actuals-p4_sqrt20220801-20230430__июнь-июль-ШтуцерНЕ0___TL1.pickle', 'wb') as handle:
#     pickle.dump(pkl__val_actuals, handle, protocol=pickle.HIGHEST_PROTOCOL)




# =========================== #
# Собираем результаты в общий файл #



for target in TARGETS:
    agg_df = pd.DataFrame()
    for key, values in scores_dict[target].items():
        agg_df[key] = values
    file_path = str(model_log_dir / f'{target}_all_scores__tl-{TECH_LINE}.xlsx')
    agg_df.to_excel(file_path, index=False)
    print(f'Saved all metrics: {target}')

    file_path = str(model_log_dir / f'{target}_all_importances__tl-{TECH_LINE}.xlsx')
    importances_dict[target].to_excel(file_path, index=False)
    print(f'Saved all importances: {target}')

# Обработка сводного файла с метриками только по тестовым дням.
filename = model_log_dir / f'test_day_scores.txt'
if filename.exists():
    with open(str(filename), 'r') as f:
        content = f.readlines()
        content = [text.strip().split(',') for text in content]
        content = pd.DataFrame(content, columns=['target', 'well', 'date', 'MAE'])
        content.sort_values(by=['target', 'well'], inplace=True)
        filename = str(model_log_dir / f'test_day_scores.xlsx')
        content.to_excel(filename, index=False)
        print('Saved test days scores.')


# сохранение общего отчета
total_separator_analyses(pkl__val_actuals, pkl__val_predictions, model_log_dir, TECH_LINE)
exit()


# Обработка сводного файла с суммарными прогнозами.
# for total_param in ('Расход по газу', 'Расход по конденсату', 'Расход по воде'):
#     filename = str(model_log_dir / f'val_predictions_{total_param}.txt')
#     with open(filename, 'r') as f:
#         content = f.readlines()
#         content = [text.strip().split(',') for text in content]
#         content = pd.DataFrame(content, columns=[
#             'well_id', 'actual_total_train', 'pred_total_train', 'actual_total_val', 'pred_total_val'])
#         filename = str(model_log_dir / f'{total_param}_totals.xlsx')
#         content.to_excel(filename, index=False)
#         print(f'Saved total for {total_param}')