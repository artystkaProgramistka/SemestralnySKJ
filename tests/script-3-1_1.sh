#!/bin/bash

# Start 3 network nodes, then terminate them
tsp java my_server.DatabaseNode -tcpport 9000 -record 1:1
sleep 1
tsp java my_server.DatabaseNode -tcpport 9001 -connect localhost:9000 -record 2:2
sleep 1
tsp java my_server.DatabaseNode -tcpport 9002 -connect localhost:9000 -connect localhost:9001 -record 3:3
sleep 1
java my_server.DatabaseClient -gateway localhost:9001 -operation get-min
java my_server.DatabaseClient -gateway localhost:9001 -operation get-max
sleep 1
java my_server.DatabaseClient -gateway localhost:9001 -operation set-value 1:4
java my_server.DatabaseClient -gateway localhost:9001 -operation get-max
sleep 1
java my_server.DatabaseClient -gateway localhost:9001 -operation set-value 3:1
java my_server.DatabaseClient -gateway localhost:9001 -operation get-min

java my_server.DatabaseClient -gateway localhost:9000 -operation terminate
java my_server.DatabaseClient -gateway localhost:9001 -operation terminate
java my_server.DatabaseClient -gateway localhost:9002 -operation terminate
