import glob
import pandas as pd
import requests
import json
import time
from calculations_flat import find_how_many_you_getV2

NODE_URL = "https://nodes.wavesnodes.com"

csv_files = glob.glob('*.csv')
waves_usdn_csv = "prices_WAVES_USD-N.csv"

def wait_for_resource_available(request):
    status_code = 0
    while status_code != 200:
        time.sleep(0.1)
        response = requests.get(NODE_URL + request)
        print(NODE_URL + request)
        status_code = response.status_code
    return json.loads(str(response.text))

def get_asset_digits(asset_id_A,asset_id_B):
    asset_id_A_digits = 8 if asset_id_A == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_A)[0]["decimals"]
    asset_id_B_digits = 8 if asset_id_B == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_B)[0]["decimals"]
    return asset_id_A_digits, asset_id_B_digits

if __name__ == "__main__":  
     for file in csv_files:
        df = pd.read_csv(file)
        columns = list(df)
        print(columns)
        if "USD-N" in columns:
            amount_asset = columns[1] # в файлах amount ассет идёт второй колонкой
            df['cost_in_USDN'] = (df["USD-N"] + (df[amount_asset]*df["price"]).reset_index(drop=True))
        else:
            df_waves_usdn = pd.read_csv(waves_usdn_csv)
            # берём количество WAVES и умножем стоимость вейвз, актуальную в паре WAVES/USDN
            # умножаем на 2, предплоагая равеносто стоимости каждого актива в портфеле
            df_merged_with_usdn = pd.merge(df_waves_usdn, df, how='left', on=['height']).dropna()
            const_in_usdn = (2*df_merged_with_usdn["WAVES_y"]*df_merged_with_usdn["price_x"]).reset_index(drop=True)
            df['cost_in_USDN'] = (2*df_merged_with_usdn["WAVES_y"]*df_merged_with_usdn["price_x"]).reset_index(drop=True) 
            df = df.fillna(method='ffill')
        print(df)
        #df.to_csv(file, index = False, header=True)