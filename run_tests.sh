tsp -K  # to numerate task from 0 
tsp -S 50 # max 50 tasks in parallel
echo ""
echo "============================================================== compile"
echo ""

javac my_server/NodeConnectionHandler.java my_server/DatabaseNode.java
javac my_server/DatabaseClient.java

echo ""
echo "============================================================== running tests: "
echo ""

# bash tests/script-1-0.sh
# bash tests/script-1-1_1.sh
# bash tests/script-1-1_2.sh
# bash tests/script-1-1_3.sh
# bash tests/script-1-1_4.sh
# bash tests/script-2-0_1.sh
# bash tests/script-2-1_1.sh
# bash tests/script-2-1_2.sh
# bash tests/script-2-1_2.sh
# bash tests/script-3-0_1.sh
# bash tests/script-3-0_1.sh
# bash tests/script-3-1_1.sh
# bash tests/script-7-1.sh
# bash tests/script-7-2.sh
bash tests/script-7-p.sh

# sleep 1
# echo ""
# echo "=============================================================="
# echo ""
# tsp
# 
# echo ""
# echo "============================================================== Output 1:"
# echo ""
# tsp -c 0
# 
# echo ""
# echo "============================================================== Output 2:"
# echo ""
# tsp -c 1
# 
# echo ""
# echo "============================================================== Output 3:"
# echo ""
# tsp -c 2
