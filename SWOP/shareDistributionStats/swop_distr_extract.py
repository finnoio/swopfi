import pandas as pd
import requests
import json
import time

NODE_URL = "https://nodes.wavesnodes.com"


def wait_for_resource_available(request):
    status_code = 0
    while status_code != 200:
        time.sleep(0.1)
        response = requests.get(NODE_URL + request)
        print(NODE_URL + request)
        status_code = response.status_code
    return json.loads(str(response.text))

def get_distr_by_height(asset_id,height,dict_distribution):
    distr = wait_for_resource_available("/assets/" + asset_id + "/distribution/" + str(height) + "/limit/1000")
    distribution_by_addres = {height : distr["items"]}
    hasNext = distr["hasNext"]
    print(hasNext)
    while hasNext:
        distr = wait_for_resource_available("/assets/" + asset_id + "/distribution/" + str(height) + "/limit/1000?after=" + hasNext)
        distribution_by_addres.update({height : distr["items"]})
    return distribution_by_addres

def get_asset_name(asset_id_A,asset_id_B):
    asset_id_A_name = "WAVES" if asset_id_A == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_A)[0]["name"]
    asset_id_B_name = "WAVES" if asset_id_B == "WAVES" else wait_for_resource_available("/assets/details?id=" + asset_id_B)[0]["name"]
    return asset_id_A_name, asset_id_B_name

pools = [
        "3PK7Xe5BiedRyxHLuMQx5ey9riUQqvUths2"
    ]

if __name__ == "__main__":        
    for pool in pools:
        asset_id_A =  wait_for_resource_available("/addresses/data/" + pool + "/A_asset_id")["value"]
        asset_id_B = wait_for_resource_available("/addresses/data/" + pool + "/B_asset_id")["value"]
        asset_id_A_name, asset_id_B_name = get_asset_name(asset_id_A,asset_id_B)
        share_asset_id = wait_for_resource_available("/addresses/data/" + pool+ "/share_asset_id")["value"]
        height_finish = wait_for_resource_available("/blocks/height")["height"]
        height_issue = int(height_finish)-10 #wait_for_resource_available("/assets/details?id=" + share_asset_id)[0]["issueHeight"]
        dict_distribution = {}
        for height in range(height_issue,height_finish):
            print(height)
            balance_distribution_at_height = get_distr_by_height(share_asset_id,height,dict_distribution)
            dict_distribution.update(balance_distribution_at_height)
            #print(dict_distribution)
        file_name = asset_id_A_name + "_" + asset_id_B_name + "_assset_distribution.csv"
        with open(file_name, 'w') as file:
            file.write(json.dumps(dict_distribution))