#!/bin/bash

sudo docker build -t zurk .
sudo docker run -d --rm --name zurk -p 9003:9000 zurk
sudo docker cp zurk:/pwnable .
