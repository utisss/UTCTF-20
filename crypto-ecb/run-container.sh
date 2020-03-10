#!/bin/bash

sudo docker build -t ecb .
sudo docker run -d --rm --name ecb -p 9003:9000 ecb
