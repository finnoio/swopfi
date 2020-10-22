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
#test2 = pw.Address(seed = str("asdasd1asda2s21412522123"))
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

invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(99990847) },{"type": "integer", "value":int(99990847)}], [{ "amount": 100000000, "assetId": assetId1 }], txFee=1000000)
print(invoke)
wait_for_resource_available(invoke["id"],1000)


invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(9998430608) },{"type": "integer", "value":int(9998432608)}], [{ "amount": 10000000000, "assetId": assetId1 }], txFee=1000000)
print(invoke)
wait_for_resource_available(invoke["id"],1000)

invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(10099909998) },{"type": "integer", "value":int(10099988998)}], [{ "amount": 10098421455, "assetId": assetId2 }], txFee=1000000)
print(invoke)
wait_for_resource_available(invoke["id"],1000)

replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [], [
    {"amount": 500000090002
, "assetId": assetId1},{"amount": 500000000000, "assetId": assetId2}], txFee=1000000)
print(replanish)
statusreplanish = wait_for_resource_available(
    replanish["id"], 1000)

# !!! Change shareAssetId !!!
shareAssetId = "3uJrLU4bvswn2e5ob7FPYPn8JMpuYGMs7CnsbwAvPMyw"
withdraw = moneySeed.invokeScript(test2.address, "withdraw", [], [
    {"amount": 499999999736, "assetId": shareAssetId}], txFee=1000000)
print(withdraw)
statusreplanish = wait_for_resource_available(
    withdraw["id"], 1000)

