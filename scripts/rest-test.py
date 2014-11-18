#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '{"action": "create-container", "template": "test"}'

def nc():
	response = requests.post(url, data=params)
	print(response.json()['dockerInstance'] + " " + response.json()['Response']['Body']['Id'])
	start = requests.post(url, data='{"action": "start-container", "id": "' + response.json()['Response']['Body']['Id'] + '"}')
	print(start.json())

nc()

