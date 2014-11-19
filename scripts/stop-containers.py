#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '{"action": "list-containers", "all": true}'

"""
"containers": [
        {
            "Content-Type": "application/json",
            "Date": "Wed, 19 Nov 2014 09:28:33 GMT",
            "Response": {
                "Body": [
                    {
                        "Command": "/bin/ping 8.8.8.8",
                        "Created": 1416389186,
                        "Id": "0b6c9b206083abf89b30bed70800a24e0985785f66ec0a36f3a1540b381123c3",
                        "Image": "ubuntu:14.04",
                        "Names": [
                            "/insane_davinci"
                        ],
                        "Ports": [
                            {
                                "IP": "0.0.0.0",
                                "PrivatePort": 80,
                                "PublicPort": 49313,
                                "Type": "tcp"
                            },
                            {
                                "IP": "0.0.0.0",
                                "PrivatePort": 22,
                                "PublicPort": 49314,
                                "Type": "tcp"
                            }
                        ],
                        "Status": "Up 2 minutes"
                    },

"""

def nc():
	response = requests.post(url, data=params)
	for node in response.json()['containers']:
 		for container in node['Response']['Body']:
			print(container['Id'])
			start = requests.post(url, data='{"action": "stop-container", "id": "' + container['Id'] + '"}')
			print(start.json())

nc()

