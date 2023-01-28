#!/bin/bash

# Start 1 network node, then terminate it
tsp java my_server.DatabaseNode -tcpport 9000 -record 1:1
sleep 1
java my_server.DatabaseClient -gateway localhost:9000 -operation terminate
