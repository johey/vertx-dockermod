#!/bin/bash
export PATH=$PATH:/opt/vert.x-2.1M2/bin/
cd /opt/deblox-docker/

vertx runmod com.deblox~docker~1.0.0-final -cluster -conf conf.json
