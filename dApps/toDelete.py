#@title

import numpy as np
import plotly.graph_objects as go
import math

def invV2(x_i,y_i,alpha,betta):
  scale8 = 100000000
  skeweness = 1/2*(int(y_i*scale8/x_i)+int(x_i*scale8/y_i))
  first_sum = math.floor((x_i+y_i)*scale8/math.ceil((skeweness/scale8)**(alpha)*scale8))
  second_sum_first_part = math.floor(round(x_i*y_i/scale8)**0.5*(scale8/10000))
  second_sum_second_part = math.floor(((skeweness-betta*scale8)/scale8)**alpha*scale8)
  second_sum = 2*math.floor((second_sum_first_part*second_sum_second_part)/scale8)
  return first_sum + second_sum

def find_how_many_you_getV2(x_balance,y_balance,x_balance_new,alpha,betta):
  actual_invarian = invV2(x_balance,y_balance,alpha,betta)
  y_left = 1
  y_right = 100*y_balance
  for i in range(50):
    mean = (y_left + y_right)/2
    #print(mean,actual_invarian,invV2(x_balance_new,mean,alpha,betta))
    invariant_left = invV2(x_balance_new,y_left,alpha,betta)
    invariant_rigth = invV2(x_balance_new,y_right,alpha,betta)
    invariant_mean = invV2(x_balance_new,mean,alpha,betta)
    invariant_delta_in_left = actual_invarian - invariant_left
    invariant_delta_in_right = actual_invarian - invariant_rigth
    invariant_delta_in_mean = actual_invarian - invariant_mean
    print(i)
    # print(actual_invarian-invariant_left,actual_invarian-invariant_rigth)
    # print((actual_invarian-invariant_left)*(actual_invarian-invariant_rigth))
    # print(f'{actual_invarian=}')
    # print(f'{y_left=}',actual_invarian-invV2(x_balance_new,y_left,alpha,betta))
    # print(f'{y_right=}',actual_invarian-invV2(x_balance_new,y_right,alpha,betta))
    # print(f'{mean=}',actual_invarian-invV2(x_balance_new,mean,alpha,betta))
    print(y_balance-y_left,y_balance-mean,y_balance-y_right)

    if invariant_delta_in_mean*invariant_delta_in_left < 0:
      y_left = y_left
      y_right = mean
    elif invariant_delta_in_mean*invariant_delta_in_right <0:
      y_left = mean
      y_right = y_right
    else:
      return y_balance - mean  
  return y_balance - mean

x_balance = 500100000000 #500k with 6 digits
y_balance = 499900032709
alpha_for_V2 = 0.50
betta_for_V2 = 0.46
you_pay_in_x = 10000000000

updated_X_amount = x_balance + you_pay_in_x
you_get_flat_v2 = find_how_many_you_getV2(x_balance,y_balance,updated_X_amount,alpha_for_V2,betta_for_V2)
print("invV2 = ",  invV2(updated_X_amount,y_balance-you_get_flat_v2,alpha_for_V2,betta_for_V2))

