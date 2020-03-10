#!/bin/sh

while [ true ]; do
	socat -dd TCP4-LISTEN:9000,fork,reuseaddr EXEC:'python3 /server.py',pty,echo=0,raw,iexten=0
done;
