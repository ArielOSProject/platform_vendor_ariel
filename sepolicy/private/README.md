Building the sepolicy

Starting with Android 15, make sepolicy is not working any more. When you change the policy, you can rebuild it with this command:
make clean-system_ext_sepolicy.cil && make system_ext_sepolicy.cil

Maybe there is a better command, but this one will trigger errors for policies defined in this folder if they are not correct.