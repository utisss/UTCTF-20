#!/bin/bash

sudo docker build -t dns .
sudo docker run -d --rm --name dns -p 54:9000/udp dns
sudo docker cp dns:/pwnable .
