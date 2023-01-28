#!/bin/bash

# Start 2 network nodes, then terminate them
tsp java my_server.DatabaseNode -tcpport 9000 -record 1:1
sleep 1
tsp java my_server.DatabaseNode -tcpport 9001 -connect localhost:9000 -record 2:2
sleep 1
# nmap localhost -p 9000-9010

java my_server.DatabaseClient -gateway localhost:9000 -operation terminate
java my_server.DatabaseClient -gateway localhost:9001 -operation terminate
