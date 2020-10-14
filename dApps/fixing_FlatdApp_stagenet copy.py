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


test2 = pw.Address(seed = "1234567891234")
moneySeed = pw.Address(seed = "mutual essence merry loop margin morning involve vicious air post table faculty primary idea buffalo")

# transfer = moneySeed.sendWaves(test2,int(10**7))
# wait_for_resource_available(transfer["id"],1000)
# print(transfer)

# setScript = test2.setScript(script,txFee=1400000)
# wait_for_resource_available(setScript["id"],1000)
# print(setScript)

# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": int(100*1e8), "assetId": None },{"amount": int(100*1e8), "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)

# data = [{
#         'type':'string', 
#         'key': 'owner', 
#         'value':'3PEFtmZXKR8rPiG3Qmrk4ZkWCh45DX8mEnW'
#         }]
# dataTx = test2.dataTransaction(data)
# print(dataTx)

# replanish = moneySeed.invokeScript(test2.address, "replenishment", [], [
#     {"amount": 10000, "assetId": None},{"amount": 10000, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)

# withdraw = moneySeed.invokeScript(test2.address, "withdraw", [], [
#     {"amount": 10000, "assetId": "43WFxusv292EziGspQ7mHxH7sfBu8osguMyt4f7RCHhX"}], txFee=1000000)
# print(withdraw)
# statusreplanish = wait_for_resource_available(
#     withdraw["id"], 1000)



setScript = test2.setScript(script,txFee=1400000)
wait_for_resource_available(setScript["id"],1000)
print(setScript)

invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(1.1*1954934208) },{"type": "integer", "value":int(0.999999*1954934208)}], [{ "amount": 5000000000, "assetId": None }], txFee=1000000)
print(invoke)

# invoke = test2.invokeScript("3N6KuDDYL9ZpT5CQ1HuzW9G1UfGRXizWbqp", "withdraw", [], [{ "amount": 10000, "assetId": None }], txFee=1000000)
# print(invoke)




# issueTx = moneySeed.issueAsset("Token3","",int(100000000*1e8),8)
# print(issueTx)

# data = [{
#         'type':'integer', 
#         'key': 'comissionScaleDelimiter', 
#         'value': 10000
#         }]
# dataTx = test2.dataTransaction(data)
# print(dataTx)


# myToken = pw.Asset('CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK')
# moneySeed.sendAsset(pw.Address(address='3MSR6AFbR4pc3XpnQy62khNFRbricnwcrfJ'), myToken, int(100000*1e8),txFee = 1000000)
# tx = moneySeed.sendWaves(pw.Address(address='3MSR6AFbR4pc3XpnQy62khNFRbricnwcrfJ'), int(100000*1e8),txFee = 1000000)
# print(tx)