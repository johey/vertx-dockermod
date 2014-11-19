curl -X POST --data '{"action": "create-container", "image": "ubuntu"}' app0126.proxmox.swe1.unibet.com:8080 | python -m json.tool

curl -X POST --data '{"action": "create-container", "template": "test"}' app0126.proxmox.swe1.unibet.com:8080 | python -m json.tool

 curl -X  POST --data '{"action": "inspect-container", "id": "a67a2e8e7e309887b19c5f0fc11eda8704b5efa9b3d3f11ff272ff215d2ae406"}' app0126.proxmox.swe1.unibet.com:8080   | python -mjson.tool
