#!/bin/bash

# Start 7 network nodes, then allocate the resources 4 times
tsp java my_server.DatabaseNode -tcpport 9000 -record 1:8
sleep 1
tsp java my_server.DatabaseNode -tcpport 9001 -connect localhost:9000 -record 2:7
sleep 1
tsp java my_server.DatabaseNode -tcpport 9002 -connect localhost:9000 -connect localhost:9001 -record 3:6
sleep 1
tsp java my_server.DatabaseNode -tcpport 9003 -connect localhost:9001 -record 4:5
sleep 1
tsp java my_server.DatabaseNode -tcpport 9004 -connect localhost:9001 -connect localhost:9003 -record 5:4
sleep 1
tsp java my_server.DatabaseNode -tcpport 9005 -connect localhost:9002 -connect localhost:9004 -record 6:3
sleep 1
tsp java my_server.DatabaseNode -tcpport 9006 -connect localhost:9002 -connect localhost:9005 -connect localhost:9003 -record 7:1
sleep 1

java my_server.DatabaseClient -gateway localhost:9003 -operation get-max
java my_server.DatabaseClient -gateway localhost:9004 -operation get-min
sleep 1
java my_server.DatabaseClient -gateway localhost:9004 -operation find-key 7
java my_server.DatabaseClient -gateway localhost:9003 -operation find-key 1
sleep 1
java my_server.DatabaseClient -gateway localhost:9003 -operation set-value 7:7
java my_server.DatabaseClient -gateway localhost:9004 -operation set-value 1:1
sleep 1
java my_server.DatabaseClient -gateway localhost:9004 -operation get-max
java my_server.DatabaseClient -gateway localhost:9003 -operation get-min
sleep 1
java my_server.DatabaseClient -gateway localhost:9003 -operation find-key 7
java my_server.DatabaseClient -gateway localhost:9004 -operation find-key 1
sleep 1

java my_server.DatabaseClient -gateway localhost:9000 -operation terminate
java my_server.DatabaseClient -gateway localhost:9001 -operation terminate
java my_server.DatabaseClient -gateway localhost:9002 -operation terminate
java my_server.DatabaseClient -gateway localhost:9003 -operation terminate
java my_server.DatabaseClient -gateway localhost:9004 -operation terminate
java my_server.DatabaseClient -gateway localhost:9005 -operation terminate
java my_server.DatabaseClient -gateway localhost:9006 -operation terminate
