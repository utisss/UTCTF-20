#!/bin/bash

sudo docker build -t stackoverflow .
sudo docker run -d --rm --name stackoverflow -p 9002:9000 stackoverflow
sudo docker cp stackoverflow:/pwnable .
