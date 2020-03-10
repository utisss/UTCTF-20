#!/bin/sh

bash /home/sage/start2.sh &
while [ true ]; do
	socat -dd TCP4-LISTEN:9000,fork,reuseaddr,max-children=2 EXEC:'sage /home/sage/server.sage',pty,echo=0,raw,iexten=0
done;
