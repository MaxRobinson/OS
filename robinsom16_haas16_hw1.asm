# fibonacci sequence 
# The anser to the fib number you want
# will be in R2 at the end of execution
# Note: We are unable to print right now 
# since we do not have support for TRAP 
# from our CPU.

# We chose the starting number for the fib
# sequence to be 1. Thanks. 
# aka. 1,1,2,3,5,8 ....

SET R0 12	# fib number
SET R1 0	# value 1
SET R2 1	# value 2
SET R3 1	# temp
SET R4 1	# counter value



:loop
#"print R2" 
SET R3 0
ADD R3 R3 R2 	# x3 = x2
ADD R2 R2 R1	# x2 = x2 + x1
SET R1 0		# x1 = 0
ADD R1 R1 R3	# x1 = x3 + 0 since x1 = 0 
				# thus x1 = x3
SET R3 1		# x3 = 1
ADD R4 R4 R3	# x4 = x4 + x3 
				# thus x4 = x4 + 1
BNE R4 R0 loop	# loop till fib number

:end



