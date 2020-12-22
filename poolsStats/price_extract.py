import pandas as pd
import requests
import json
import time
from calculations_flat import find_how_many_you_getV2

NODE_URL = "https://nodes.wavesnodes.com"

pools = [
        "3PACj2DLTw3uUhsUmT98zHU5M4hPufbHKav",
        "3P8FVZgAJUAq32UEZtTw84qS4zLqEREiEiP",
        "3PPH7x7iqobW5ziyiRCic19rQqKr6nPYaK1",
        "3P2V63Xd6BviDkeMzxhUw2SJyojByRz8a8m",
        "3PMDFxmG9uXAbuQgiNogZCBQASvCHt1Mdar",
        "3P6DLdJTP2EySq9MFdJu6beUevrQd2sVVBh",
        "3PK7Xe5BiedRyxHLuMQx5ey9riUQqvUths2",#USDN/EURO
        "3PHaNgomBkrvEL2QnuJarQVJa71wjw9qiqG" #WAVES/USDN
    ]


price_amount_dict = {
        "WAVES/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "WAVES"},
        "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS"},
        "WAVES/8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS":
            {"price" : "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS", 
             "amount": "WAVES"},
        "34N9YcEETLWn93qYQ64EsP1x89tSruJU44RrEMSXXEPJ/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "34N9YcEETLWn93qYQ64EsP1x89tSruJU44RrEMSXXEPJ"},
        "4LHHvYGNKJUg5hj65aGD5vgScvCBmLpdRFtjokvCjSL8/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "4LHHvYGNKJUg5hj65aGD5vgScvCBmLpdRFtjokvCjSL8"},
        "WAVES/DUk2YTxhRoAqMJLus4G2b3fR8hMHVh6eiyFx5r29VR6t":
            {"price" : "DUk2YTxhRoAqMJLus4G2b3fR8hMHVh6eiyFx5r29VR6t", 
             "amount": "WAVES"},
        "6nSpVyNH7yM69eg446wrQR94ipbbcmZMU1ENPwanC97g/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "6nSpVyNH7yM69eg446wrQR94ipbbcmZMU1ENPwanC97g"},
        "DHgwrRvVyqJsepd32YbBqUeDH4GJ1N984X8QoekjgH8J/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p":
            {"price" : "DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", 
             "amount": "DHgwrRvVyqJsepd32YbBqUeDH4GJ1N984X8QoekjgH8J"},
    }


def wait_for_resource_available(request):
    status_code = 0
    while status_code != 200:
        time.sleep(0.1)
        response = requests.get(NODE_URL + request)
        print(NODE_URL + request)
        status_code = response.status_code
    return json.loads(str(response.text))

def extract_price_amount(asset_id_A,asset_id_B,price_amount_dict):
    key_price_amount_1 = asset_id_A + "/" + asset_id_B
    key_price_amount_2 = asset_id_A + "/" + asset_id_B
    if key_price_amount_1 in price_amount_dict:
        return price_amount_dict[key_price_amount_1]
    elif key_price_amount_2 in price_amount_dict:
        return price_amount_dict[key_price_amount_2]

def find_A_balance_B_balance(chage_state_data):
    # Определяем, в каком элемента списка содержится информация о количестве ассете А и Б
    if chage_state_data[0]["key"] == "A_asset_balance": 
        return chage_state_data[0]["value"],chage_state_data[1]["value"] 
    else:
        return chage_state_data[1]["value"],chage_state_data[0]["value"]

def get_asset_name(asset_id_A,asset_id_B):
    asset_id_A_name = "WAVES" if asset_id_A == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_A)[0]["name"]
    asset_id_B_name = "WAVES" if asset_id_B == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_B)[0]["name"]
    return asset_id_A_name, asset_id_B_name

def get_asset_digits(asset_id_A,asset_id_B):
    asset_id_A_digits = 8 if asset_id_A == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_A)[0]["decimals"]
    asset_id_B_digits = 8 if asset_id_B == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_B)[0]["decimals"]
    return asset_id_A_digits, asset_id_B_digits

def add_price_column(df,price_amount):
    price_name,amount_name = get_asset_name(price_amount["price"],price_amount["amount"])
    price_digits,amount_digits =  get_asset_digits(price_amount["price"],price_amount["amount"])
    if amount_name == "USDT" and price_name == "USD-N":
        alpha = 0.50
        betta = 0.46
        x_pay = 1000000
        price_column =  df.apply(lambda row: find_how_many_you_getV2(row["USDT"],row["USD-N"],row["USDT"] + x_pay ,alpha,betta)/x_pay, axis=1)
    else:
        price_column = (df[price_name]/10**int(price_digits))/(df[amount_name]/10**int(amount_digits))
    df["price"] = price_column
    return df

def add_exchanges_info_to_data_frame(df,tx_batch):
    for tx in tx_batch[0]:
        if tx["type"] == 16 and tx["call"]["function"] == "exchange"  and str(tx["stateChanges"]["data"][0]["value"]).isnumeric() and str(tx["stateChanges"]["data"][1]["value"]).isnumeric() :
            a_amount,b_amount = find_A_balance_B_balance(tx["stateChanges"]["data"])
            columns = list(df)
            values = [tx["height"],a_amount,b_amount]
            zipped = zip(columns, values)
            value_dictionary = dict(zipped)
            df = df.append(value_dictionary,ignore_index=True)
    return df
            
def aggreagte_same_height(df):
    columns = list(df)
    df[columns[1]] = df[columns[1]].astype(int) 
    df[columns[2]] = df[columns[2]].astype(int) 
    df = df.groupby('height').agg({columns[1]:'mean',columns[2]:'mean'}).reset_index()
    return df

def extract_amount_info(pool,df,price_amount):
    transactions = wait_for_resource_available("/transactions/address/" + pool + "/limit/1000")
    df = add_exchanges_info_to_data_frame(df,transactions)
    while True: 
        transactions = wait_for_resource_available("/transactions/address/" + pool + "/limit/1000?after=" + transactions[0][-1]["id"])
        if len(transactions[0]) > 0:
            df = add_exchanges_info_to_data_frame(df,transactions)
            df = add_price_column(df,price_amount)
        else:
            break
    df = aggreagte_same_height(df)
    df = add_price_column(df,price_amount)
    return df

def interpolate_prev_values(df):
    height_min = int(df["height"].min())
    height_max = int(df["height"].max())
    height_dataFrame = pd.Series([x for x in range(height_min,height_max+1)]).to_frame().rename(columns={0: "height"}).sort_values(by=['height'], ascending=False)
    return pd.merge(height_dataFrame, df, how='left', on=['height']).fillna(method='backfill')

if __name__ == "__main__":        
    for pool in pools:
        asset_id_A =  wait_for_resource_available("/addresses/data/" + pool + "/A_asset_id")["value"]
        asset_id_B = wait_for_resource_available("/addresses/data/" + pool + "/B_asset_id")["value"]
        asset_id_A_name, asset_id_B_name = get_asset_name(asset_id_A,asset_id_B)
        asset_id_A_digits, asset_id_B_digits = get_asset_digits(asset_id_A,asset_id_B)
        df = pd.DataFrame(columns=["height",asset_id_A_name,asset_id_B_name]) # создаем датафрейм, в котором будет всё хранить
        price_amount = extract_price_amount(asset_id_A,asset_id_B,price_amount_dict)
        df = extract_amount_info(pool,df,price_amount)
        df = interpolate_prev_values(df)
        df[asset_id_A_name] = df[asset_id_A_name]/10**asset_id_A_digits
        df[asset_id_B_name] = df[asset_id_B_name]/10**asset_id_B_digits
        df.to_csv("prices_" + asset_id_A_name + "_" + asset_id_B_name + ".csv", index = False, header=True)
        print(df)

