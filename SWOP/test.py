import pywaves as pw
import random
import requests
import time
from swopContract import dAppScript
import json

'''
init pool share - не нужнен

ассет вместо pool  

цена shareT 

'''

NODE_URL = "http://testnet-htz-nbg1-1.wavesnodes.com"

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

pw.setNode(node=NODE_URL, chain_id="T")

moneySeed2 = pw.Address(seed = "scorpion early squirrel place father firm nothing warrior robot secret life blur fragile barrel task")
moneySeed =  pw.Address(seed = "mutual essence merry loop margin morning involve vicious air post table faculty primary idea buffalo") # 3MqhxbxHEMtp2Rwy1gUb6cPSJHQepdap1Yp
poolSeed = pw.Address(seed = "poolSeed")#3N17FKivzcFibMasSDgZWoRto7YBq1ZoHHu
swopSeed = pw.Address(seed = "swop6")
votingSeed = pw.Address(seed = "vote") # 3N5F5DCmWNzT8ZPe1tBfiXaVYzWJriaJZ9E
assetIdShare = "6hM1PRMT3BBYAazn6R882YGiMuUaECpgPM5tTHeFVFC"

# transfer = moneySeed.sendWaves(swopSeed,int(100000000), txFee= 1400000)
# print(transfer)
# wait_for_resource_available(transfer["id"],1000)

# setScript = swopSeed.setScript(script,txFee=1400000)
# print(setScript)
# wait_for_resource_available(setScript["id"],1000)

# init = moneySeed.invokeScript(swopSeed.address, "init", [], [], txFee=int(100500000))
# print(init)
# statusInit = wait_for_resource_available(
#     init["id"], 100)

# initPool = moneySeed.invokeScript(swopSeed.address, "initPoolShareFarming", [{"type": "string", "value": poolSeed.address }], [], txFee=int(500000))
# print(initPool)
# statusInit = wait_for_resource_available(
#     initPool["id"], 100)

# lockShare = moneySeed.invokeScript(swopSeed.address, "lockShareTokens", [{"type": "string", "value": poolSeed.address }], [{ "amount":  int(5000 ), "assetId": assetIdShare }], txFee=int(500000))
# print(lockShare)
# statusInit = wait_for_resource_available(
#     lockShare["id"], 100)

# claim = moneySeed.invokeScript(swopSeed.address, "claim", [{"type": "string", "value": poolSeed.address },{"type": "integer", "value": 190000000*2 }], [], txFee=int(500000))
# print(claim)
# statusInit = wait_for_resource_available(
#     claim["id"], 100)

# withdraw = moneySeed.invokeScript(swopSeed.address, "withdrawShareTokens", [{"type": "string", "value": poolSeed.address },{"type": "integer", "value": 5000 }], [], txFee=int(500000))
# print(withdraw)
# statusInit = wait_for_resource_available(
#     withdraw["id"], 100)



#/// second depositor start \\\\#

# transfer = moneySeed.sendAsset(moneySeed2, pw.Asset(assetId = assetIdShare), int(10*1e8))
# print(transfer)
# statusInit = wait_for_resource_available(
#     transfer["id"], 100)

# lockShare = moneySeed2.invokeScript(swopSeed.address, "lockShareTokens", [{"type": "string", "value": poolSeed.address }], [{ "amount":  int(5000 ), "assetId": assetIdShare }], txFee=int(900000))
# print(lockShare)
# statusInit = wait_for_resource_available(
#     lockShare["id"], 100)

# claim = moneySeed2.invokeScript(swopSeed.address, "claim", [{"type": "string", "value": poolSeed.address }], [], txFee=int(900000))
# print(claim)
# statusInit = wait_for_resource_available(
#     claim["id"], 100)

#/// second depositor end \\\\#




#/// voting start \\\\#

# transfer = moneySeed.sendWaves(votingSeed,int(10000000), txFee= 1400000)
# print(transfer)
# wait_for_resource_available(transfer["id"],1000)

# data = votingSeed.dataTransaction([{
#         'type':'integer', 
#         'key': poolSeed.address + "_current_reward", 
#         'value': 190000000
#         },
#         {
#         'type':'integer', 
#         'key': poolSeed.address + "_reward_update_height", 
#         'value': 0
#         },
#         {
#         'type':'integer', 
#         'key': poolSeed.address + "_previous_reward", 
#         'value': 0
#         }])
# print(data)
# wait_for_resource_available(data["id"],1000)

#/// voting end \\\\#


#/// pool start \\\\#

# data = poolSeed.dataTransaction([{
#         'type':'string', 
#         'key': 'share_asset_id', 
#         'value': "6hM1PRMT3BBYAazn6R882YGiMuUaECpgPM5tTHeFVFC"
#         }])
# print(data)
# wait_for_resource_available(data["id"],1000)
# print(data)


#/// pool end \\\\#







# transfer = moneySeed.sendWaves(test2,int(4000000), txFee= 1400000)
# wait_for_resource_available(transfer["id"],1000)
# print(transfer)

# setScript = test2.setScript(script,txFee=1400000)
# print(setScript)
# wait_for_resource_available(setScript["id"],1000)

# init = moneySeed.invokeScript(test2.address, "init", [], [
#     {"amount": int(154042733762), "assetId": assetId1 },{"amount": int(240056607717), "assetId": assetId2}], txFee=int(1e8*1.5))
# print(init)
# statusInit = wait_for_resource_available(
#     init["id"], 100)

# data = test2.dataTransaction([{
#         'type':'integer', 
#         'key': 'share_asset_supply', 
#         'value': 196530473735
#         }])
# wait_for_resource_available(data["id"],1000)
# print(data)

# invoke = moneySeed.invokeScript(test2.address, "replenishWithOneToken", [{"type": "integer", "value": int(2995966938) },{"type": "integer", "value":int(3024856069-1)}], [{ "amount":  int(5000000000 ), "assetId": assetId1 }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchange", [{"type": "integer", "value": int(2844476) },{"type": "integer", "value":int(2854230)}], [{ "amount": 5000500 , "assetId": assetId2 }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)


# replanish = moneySeed.invokeScript(test2.address, "replenishWithTwoTokens", [{"type": "integer", "value": int(10) }], [
#     {"amount": int(0.0005*1e6), "assetId": assetId1},
#     {"amount": int(0.000663*1e6), "assetId": assetId2}], txFee=1000000)
# print(replanish)
# statusreplanish = wait_for_resource_available(
#     replanish["id"], 1000)






# invoke = moneySeed.invokeScript(test2.address, "replanishmentWithOneToken", [{"type": "integer", "value": int(249826581) },{"type": "integer", "value":int(249823612)}], [{ "amount":  int(500000000), "assetId": assetId1 }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(99997291) },{"type": "integer", "value":int(99997291)}], [{ "amount": 100000000, "assetId": assetId1 }], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "shutdown", [], [], txFee=1000000)
# print(invoke)
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(966) },{"type": "integer", "value":int(966)}], [{ "amount": 1000, "assetId": assetId1 }], txFee=1000000)
# print(invoke)                                                                                 
# wait_for_resource_available(invoke["id"],1000)

# invoke = moneySeed.invokeScript(test2.address, "exchanger", [{"type": "integer", "value": int(123080811) },{"type": "integer", "value":int(123080811)}], [{ "amount": 123122512, "assetId": assetId1 }], txFee=1000000)
# print(invoke)                                                                                                                                                                                                                                             
# wait_for_resource_available(invoke["id"],1000)

# replanish = moneySeed.invokeScript(test2.address, "replenishmentWithTwoToken", [], [
#     {"amount": 500006067795, "assetId": assetId1},
#     {"amount": 500000000000, "assetId": assetId2}], txFee=1000000)
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



# moneySeedNode.sendAsset(moneySeed, pw_assetId1, int(10000000*1e8))
# moneySeedNode.sendAsset(moneySeed, pw_assetId2, int(10000000*1e8))

# print(moneySeed.issueAsset( name = "USDNmy",
#                                 description = "This is my first token",
#                                 quantity = 10000000000000000,
#                                 decimals = 6 ))