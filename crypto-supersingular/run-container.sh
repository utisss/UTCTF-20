#!/bin/bash

sudo docker build -t super .
sudo docker run -d --rm --name super -p 9005:9000 super
