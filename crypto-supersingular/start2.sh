#!/bin/sh

while [ true ]; do
	kill -9 $(ps aux | grep "server.sage" | awk {'print $2'})
	sleep 12
done;

