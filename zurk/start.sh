#!/bin/bash

echo "utflag{wtf_i_h4d_n0_buffer_overflows}" > /home/zurk/flag.txt
chown root:root /home/zurk/flag.txt
chmod 644 /home/zurk/flag.txt

while [ true ]; do
	su -l zurk -c "socat -dd TCP4-LISTEN:9000,fork,reuseaddr EXEC:'/pwnable',pty,echo=0,raw,iexten=0"
done;
