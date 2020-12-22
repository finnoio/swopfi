import numpy as np
import plotly.graph_objects as go
import math
#высчитваем инвариант
def invV1(x_i,y_i,alpha):
  skeweness = 1/2*(y_i/x_i+x_i/y_i)
  return (x_i+y_i)*(skeweness)**(-alpha) + (x_i*y_i)*(skeweness)**(alpha)

def invV2_cont(x_i,y_i,alpha,betta):
  skeweness = 1/2*(y_i/x_i+x_i/y_i)
  return (x_i+y_i)*(skeweness)**(-alpha) + 2*(x_i*y_i)**(1/2)*(skeweness - betta)**((alpha))


def invV2(x_i,y_i,alpha,betta):
  scale8 = 100000000
  scale12 = 1000000000000
  skeweness = math.floor(1/2*(int(y_i*scale12/x_i)+int(x_i*scale12/y_i))/10000)
  first_sum = math.floor((x_i+y_i)*scale8/math.ceil((skeweness/scale8)**(alpha)*scale8))
  second_sum_first_part = math.floor(round(x_i*y_i/scale8)**0.5*(scale8/10000))
  second_sum_second_part = math.floor(((skeweness-betta*scale8)/scale8)**alpha*scale8)
  second_sum = 2*math.floor((second_sum_first_part*second_sum_second_part)/scale8)
  return first_sum + second_sum


# зная о монотонности функции,воспользуемся методом половинного деления
def find_how_many_you_getV2(x_balance,y_balance,x_balance_new,alpha,betta):
  actual_invarian = invV2(x_balance,y_balance,alpha,betta)
  y_left = 1
  y_right = 100*y_balance 
  for _ in range(50):
    mean = (y_left + y_right)/2
    invariant_delta_in_left = actual_invarian - invV2(x_balance_new,y_left,alpha,betta)
    invariant_delta_in_right = actual_invarian - invV2(x_balance_new,y_right,alpha,betta)
    invariant_delta_in_mean = actual_invarian - invV2(x_balance_new,mean,alpha,betta)
    if invariant_delta_in_mean*invariant_delta_in_left < 0:
      y_left = y_left
      y_right = mean
    elif invariant_delta_in_mean*invariant_delta_in_right <0:
      y_left = mean
      y_right = y_right
    else:
      return y_balance -  mean
  return y_balance -  mean