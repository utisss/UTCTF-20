#!/bin/bash

echo "utflag{what_is_a_datagram_angkjangkjankcm}" > /home/dns/flag.txt
chown root:root /home/dns/flag.txt
chmod 644 /home/dns/flag.txt

while [ true ]; do
	su -l dns -c "cd / && ./pwnable" &
	sleep 5
done;
