import pywaves as pw
import random
import requests
import time
from dAppScript import dAppScript
import json

NODE_URL = "https://nodes.wavesnodes.com"

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

pw.setNode(node=NODE_URL, chain_id="W")

test2=pw.Address(seed="puzzle evolve elbow position flag basic phrase click dry style clarify remove breeze trade flush")
assetId1 = "8rHCFkUBXESpofv7Yw2PeSsKaj3sx8uwMbyXFmvbEhFe"#waves
assetId2 = "8ryPDzHAvr1VW5LnmFTaUxVaenu9o65nkxBA2rKm1KiW"#usdn
amount1 = int(100*1e8)
amount2 = int(327*1e6)

moneySeed = pw.Address(seed = "beauty argue shell wing person now flower range soft renew same chalk iron tiger moment")

# transfer = moneySeed.sendWaves(test2,int(1400000))
# wait_for_resource_available(transfer["id"],1000)
# print(transfer)

print(script)

setScript = test2.setScript(script,txFee=1400000)
print(setScript)
wait_for_resource_available(setScript["id"],1000)


# fund = moneySeed.invokeScript(test2.address, "fund", [], [
#     {"amount": amount1, "assetId": assetId1 },{"amount": amount2, "assetId": assetId2}], txFee=100900000)
# print(fund)
# statusFund = wait_for_resource_available(
#     fund["id"], 100)
# print(fund)


# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(9990004894) },{"type": "integer", "value":int(9990004894)}], [{ "amount": 10000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)


# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(933131743117) },{"type": "integer", "value":int(933131743117)}], [{ "amount": 1000000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(113800618343) },{"type": "integer", "value":int(113862618343)}], [{ "amount": 100000000000, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK" }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# replanish = moneySeed.invokeScript(test2.address, "replenishment", [], [
#     {"amount": 10896199381657
# , "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi"},{"amount": 9156878251989, "assetId": "CdNeFRKeotuA9pnS2AaEAKPUGVPT56e5FXebDuGU8XMK"}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)




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



# setScript = test2.setScript(script,txFee=1400000)
# wait_for_resource_available(setScript["id"],1000)
# print(setScript)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(9993004894) },{"type": "integer", "value":int(9993004894)}], [{ "amount": 10000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
# print(invoke)

# invoke = test2.invokeScript("3N6KuDDYL9ZpT5CQ1HuzW9G1UfGRXizWbqp", "withdraw", [], [{ "amount": 10000, "assetId": None }], txFee=1000000)
# print(invoke)




# issueTx = moneySeed.issueAsset("Token4","",int(100000000*1e8),8)
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