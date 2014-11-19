#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '{"action": "list-containers", "all": true}'

def nc():
	response = requests.post(url, data=params)
	for node in response.json()['containers']:
 		for container in node['Response']['Body']:
			print(container['Id'])
			start = requests.post(url, data='{"action": "delete-container", "id": "' + container['Id'] + '"}')
			print(start.json())

nc()

