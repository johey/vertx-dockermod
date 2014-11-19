#!/usr/bin/env python

import requests

url = 'http://app0126.proxmox.swe1.unibet.com:8080/'
params = '''
{ 
  "action": "create-container", 
  "body": {
  "Hostname": "TESTNODE",
  "User": "",
  "Memory": 0,
  "MemorySwap": 0,
  "AttachedStdin": true,
  "AttachStdout": true,
  "AttachStderr": true,
  "PortSpecs": [
    "80",
    "22"
  ],
  "Privileged": false,
  "Tty": true,
  "OpenStdin": true,
  "StdinOnce": false,
  "Env": null,
  "Cmd": [
    "\/bin\/ping",
    "8.8.8.8"
  ],
  "Dns": null,
  "Image": "ubuntu",
  "Volumes": {

  },
  "VolumesFrom": "",
  "WorkingDir": "",
  "HostConfig": {
    "PublishAllPorts":true
  }
}
}
'''

def nc():
	container = requests.post(url, data=params).json()
	print(container['dockerInstance'] + " " + container['Response']['Body']['Id'])
	start = requests.post(url, data='{"action": "start-container", "id": "' + container['Response']['Body']['Id'] + '"}')
	print(start.json())

nc()

