#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '{"action": "create-container", "template": "test"}'

def nc():
	container = requests.post(url, data=params).json()
	inspect = requests.post(url, data='{"action": "inspect-container", "id": "' + container['Response']['Body']['Id'] + '"}').json();
	print(inspect)

nc()

