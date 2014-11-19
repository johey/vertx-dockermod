#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '{"action": "create-container", "template": "test", "instances": 20}'

def nc():
	response = requests.post(url, data=params)
	for container in response.json()['containers']:
		print(container['dockerInstance'] + " " + container['Response']['Body']['Id'])
		start = requests.post(url, data='{"action": "start-container", "id": "' + container['Response']['Body']['Id'] + '"}')
		print(start.json())

nc()

