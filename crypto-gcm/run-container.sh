#!/bin/bash

sudo docker build -t gcm .
sudo docker run -d --rm --name gcm -p 9004:9000 gcm
