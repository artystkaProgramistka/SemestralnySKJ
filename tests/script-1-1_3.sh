#!/bin/bash

# Start 1 network node, then find a value using it
tsp java my_server.DatabaseNode -tcpport 9000 -record 1:1 &
sleep 1
java my_server.DatabaseClient -gateway localhost:9000 -operation find-key 1
java my_server.DatabaseClient -gateway localhost:9000 -operation terminate
