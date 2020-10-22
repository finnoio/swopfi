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

##if true then  throw(toString(invariantCalc(amountTokenA-amountToSendEstimated,amountTokenB + tokenReceiveAmount)) + " "+toString(invariant)) else

assetId1 = "EbgcoV8AoM7sPFav3tueLcQyLicz255Et1hZMfZxaJn2"
assetId2 = "4zhPJWeFuPGHfQBsVctk5wqNCRoNVqGtT4YnvKJX6tVN"

test2 = pw.Address(seed = str(random.randint(1, 100000000000000000000)) + "a")
moneySeed = pw.Address(seed = "mutual essence merry loop margin morning involve vicious air post table faculty primary idea buffalo")

transfer = moneySeed.sendWaves(test2,int(10**8))
wait_for_resource_available(transfer["id"],1000)
print(transfer)

setScript = test2.setScript(script,txFee=1400000)
#print(setScript)
wait_for_resource_available(setScript["id"],1000)


fund = moneySeed.invokeScript(test2.address, "fund", [], [
    {"amount": int(500000*1e6), "assetId": assetId1 },{"amount": int(500000*1e6), "assetId": assetId2}], txFee=100900000)
print(fund)
statusFund = wait_for_resource_available(
    fund["id"], 100)
print(fund)

invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(99704601236) },{"type": "integer", "value":int(99704681236)}], [{ "amount": 100000000000, "assetId": assetId1 }], txFee=1000000)
print(invoke)
wait_for_resource_available(invoke["id"],1000)




invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(933131743117) },{"type": "integer", "value":int(933131743117)}], [{ "amount": 1000000000000, "assetId": "qTtranpN3eE8UDZ5kehxvHHtUggXCMTyANGv3RtvaKi" }], txFee=1000000)
print(invoke)
wait_for_resource_available(invoke["id"],1000)

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




# issueTx = moneySeed.issueAsset("myUSDT","",int(100000000000*1e6),6)
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