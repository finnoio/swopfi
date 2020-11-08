import pywaves as pw
import random
import requests
import time
from dAppScript import dAppScript
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

##if true then  throw(toString(invariantCalc(amountTokenA-amountToSendEstimated,amountTokenB + tokenReceiveAmount)) + " "+toString(invariant)) else
    
test2 = pw.Address(seed = "as1sas2222223222")
moneySeed = pw.Address(seed = "mutual essence merry loop margin morning involve vicious air post table faculty primary idea buffalo")
print(test2)
print(moneySeed)

# transfer = moneySeed.sendWaves(test2,int(10**8))
# wait_for_resource_available(transfer["id"],1000)
# print(transfer)

# setScript = test2.setScript(script,txFee=1400000)
# #print(setScript)
# wait_for_resource_available(setScript["id"],1000)


# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": int(10000000000), "assetId": "4zhPJWeFuPGHfQBsVctk5wqNCRoNVqGtT4YnvKJX6tVN"},{"amount": int(10000000000), "assetId":  None}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)

# replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [{"type": "integer", "value": int(10) }], [
#     {"amount": 100000
# , "assetId": "4zhPJWeFuPGHfQBsVctk5wqNCRoNVqGtT4YnvKJX6tVN"},{"amount": 100000-1, "assetId": None}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)



# replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [{"type": "integer", "value": int(0) }], [
#     {"amount": 1310000
# , "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi"},{"amount": 10000, "assetId":None}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)

# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": int(15000000000), "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" },{"amount": int(327000000), "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)

# replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [], [
#     {"amount": 50000000000
# , "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi"},{"amount": 1090000000, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)

# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": int(500000*1e8), "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" },{"amount": int(500000*1e8), "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)


# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(999000999)}], [{ "amount": 10000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)


# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(9069046301) }], [{ "amount": 1000000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(0) }], [{ "amount": 916889645095, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# replanish = moneySeed.invokeScript(test2.address, "replenishment", [], [
#     {"amount": 10001009248501
# , "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi"},{"amount": 10000000000000, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)

# withdraw = moneySeed.invokeScript(test2.address, "withdraw", [], [
#     {"amount": 10000000000199, "assetId": "9n42hrATgvKcVQcDV3o62ZBQ5RV7bJykWNGjcYLwBGSx"}], txFee=1000000)
# print(withdraw)
# statusreplanish = wait_for_resource_available(
#     withdraw["id"], 1000)


