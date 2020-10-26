import pywaves as pw
import random
import requests
import time
from dAppScript_flat import dAppScript
import json

NODE_URL = "https://nodes-stagenet.wavesnodes.com"

def wait_for_resource_available(id, timeout):
    status_code = 0
    status = 0
    while status_code != 200:
        time.sleep(1)
        response = requests.get(NODE_URL + "/transactions/info/" + id)
        status_code = response.status_code
        try:
            status = json.loads(str(response.content.decode('utf8')))[
                "applicationStatus"]
        except:
            status = "NotFound"
    return status

script = dAppScript()

pw.setNode(node="https://nodes-stagenet.wavesnodes.com", chain_id="S")


assetId1 = "EbgcoV8AoM7sPFav3tueLcQyLicz255Et1hZMfZxaJn2"
assetId2 = "4zhPJWeFuPGHfQBsVctk5wqNCRoNVqGtT4YnvKJX6tVN"

#test2 = pw.Address(seed = str(random.randint(1, 100000000000000000000)) + "a")
test2 = pw.Address(seed = str("i1"))
moneySeed = pw.Address(seed = "mutual essence merry loop margin morning involve vicious air post table faculty primary idea buffalo")

# transfer = moneySeed.sendWaves(test2,int(140000000))
# wait_for_resource_available(transfer["id"],1000)
# print(transfer)

# setScript = test2.setScript(script,txFee=1400000)
# # print(setScript)
# wait_for_resource_available(setScript["id"],1000)

# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": int(500000*1e6), "assetId": assetId1 },{"amount": int(500000*1e6), "assetId": assetId2}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(99997291) },{"type": "integer", "value":int(99997291)}], [{ "amount": 100000000, "assetId": assetId1 }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(966) },{"type": "integer", "value":int(966)}], [{ "amount": 1000, "assetId": assetId1 }], txFee=1000000)
# print(invoke)                                                                                 
# wait_for_resource_available(invoke["id"],1000)

invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(123080811) },{"type": "integer", "value":int(123080811)}], [{ "amount": 123122512, "assetId": assetId1 }], txFee=1000000)
print(invoke)                                                                                                                                                                                                                                             
wait_for_resource_available(invoke["id"],1000)

# replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [], [
#     {"amount": 500006067795
# , "assetId": assetId1},{"amount": 500000000000, "assetId": assetId2}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)

# !!! Change shareAssetId !!!
# shareAssetId = "CjxZ9r8DQsN7rdk5LSQ9AsZLcV6uoH9NqdavjeSx99Yt"
# withdraw = moneySeed.invokeScript(test2.address, "withdraw", [], [
#     {"amount": 499999999736, "assetId": shareAssetId}], txFee=1000000)
# print(withdraw)
# statusreplanish = wait_for_resource_available(
#     withdraw["id"], 1000)

