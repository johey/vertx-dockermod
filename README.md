# deblox.Docker

Cluster your docker instances! DockerMod runs on each host which has a docker daemon running, and the DockerMod instances then self-cluster to create a messagebus where docker instances can be controller from.

# Under heavy development.
Far from anywhere!

## Installation
This module should be run on every host that runs docker daemon.
DockerMod automatically clusters and using the webui on any one node should
grant instance control anywhere in the cluster.

## Json Messages
DockerMod speaks json over messagebus. The HttpService accepts json documents via POST.


## Running (dev mode)

### Tests
./gradlew test -i

### Daemon
./gradlew runMod -i

### FatJar cluster
 /opt/jdk1.7.0_72/bin/java -jar docker-1.0.0-final-fat.jar -conf conf.json  -cluster  -cluster-host app0126.proxmox.swe1.unibet.com

### Some Curl Tests

curl -X   POST --data '{"action": "create-unibet-container", "template": "test", "instances": 20}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool


### Register DockerMod Instance
announce a new DockerMod instance in the cluster, used when adding a docker daemon to the pool.

Request
```
{
  "action": "register",
  "hostname": "sthmaclt009.local"
}
```

No Response

### Unregister DockerMod Instance
removes the DockerMod instance from the pool, running instances are left running within docker daemon.

Request
```
{
  "action": "unregister",
  "hostname": "sthmaclt009.local"
}
```

No Response



### Create Container

Create a new container either from template or by specifying OS image to use. New instances are automatically subscribed to a queue
matching the Id of the container.


Request
```
{
    "action": "create-unibet-container",
    "template": "sometemplatename", // if present, load a template
    "image": ubuntu, // ignored if template is present, specified OS image to use
    "instances": 2 // number of instances to boot round-robin in the cluster
}
```

Curl Request
```
curl -X   POST --data '{"action": "create-container", "template": "test", "instances": 2}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool

curl -X   POST --data '{"action": "create-container", "image": "ubuntu"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

Response if instances specified

```
{
    "containers": [
        {
            "Content-Length": "90",
            "Content-Type": "application/json",
            "Date": "Tue, 18 Nov 2014 11:43:59 GMT",
            "Response": {
                "Body": {
                    "Id": "adda8bf806b8e37c99fb8c477203e950bfcedabaefebd5b2d285f55cbd73da94",
                    "Warnings": null
                }
            },
            "dockerInstance": "unisthlt035.unibet.com",
            "statusCode": 201
        },
        {
            "Content-Length": "90",
            "Content-Type": "application/json",
            "Date": "Tue, 18 Nov 2014 11:43:59 GMT",
            "Response": {
                "Body": {
                    "Id": "dc1fc364cefe5d232b8c632fbc335cdeaab79a4f8fd6eefa57dff2e6185379ba",
                    "Warnings": null
                }
            },
            "dockerInstance": "unisthlt035.unibet.com",
            "statusCode": 201
        }
    ]
}
```

Response if instances NOT specified
```
{
    "Content-Length": "90",
    "Content-Type": "application/json",
    "Date": "Tue, 18 Nov 2014 11:47:32 GMT",
    "Response": {
        "Body": {
            "Id": "949c89ef28bef7accf6258a5502830ae71c33d98a9ffddbe46a80430324de630",
            "Warnings": null
        }
    },
    "dockerInstance": "unisthlt035.unibet.com",
    "statusCode": 201
}
```

### Start Container
start a previously created container

Request
```
{
    "action": "start-container",
    "id": "f1e3db7261a9f477577c46ba4c033a46678aafabd8c866c1dad70255e35a9ead"
}
```

Response
```
{
    "Date": "Mon, 17 Nov 2014 15:28:05 GMT",
    "Response": "",
    "dockerInstance": "app0127.proxmox.swe1.unibet.com",
    "statusCode": 204
}
```

### List Images
returns a list of images on 'this' DockerMod instance. These are images which have been downloaded to this instance at some point.

Request
```
{
  "action": "list-images"
}
```

Response

```
{
  "Response-Code": 200,
  "Content-Type": "application/json",
  "Date": "Wed, 12 Nov 2014 09:15:04 GMT",
  "Content-Length": "1836",
  "Response": {
    "Body": [{
      "Created": 1414108504,
      "Id": "277eb430490785bab9c3c08241f40a3f7181c2809ec6226a308800a5337acc3f",
      "ParentId": "8f118367086c581e2cc93f7a680ebc1195be66ed311014a044a9908ab9832a35",
      "RepoTags": ["ubuntu:14.10", "ubuntu:utopic"],
      "Size": 0,
      "VirtualSize": 215607768
    }, {
      "Created": 1414108439,
      "Id": "5506de2b643be1e6febbf3b8a240760c6843244c41e12aa2f60ccbb7153d17f5",
      "ParentId": "22093c35d77bb609b9257ffb2640845ec05018e3d96cb939f68d0e19127f1723",
      "RepoTags": ["ubuntu:14.04.1", "ubuntu:latest", "ubuntu:14.04", "ubuntu:trusty"],
      "Size": 0,
      "VirtualSize": 197766079
    }, {
      "Created": 1414108405,
      "Id": "0b310e6bf058ce4983727d04ddd4e7152ee977c548d14c5049a30e4944118c25",
      "ParentId": "30868777f2756735efc907c763555f869472d48ea92156c3c4b09ac22ec4adc6",
      "RepoTags": ["ubuntu:12.04.5", "ubuntu:12.04", "ubuntu:precise"],
      "Size": 0,
      "VirtualSize": 116068936
    }, {
      "Created": 1403128455,
      "Id": "c5881f11ded97fd2252adf93268114329e985624c5d7bb86e439a36109d1124e",
      "ParentId": "5796a7edb16bffa3408e0f00b1b8dc0fa4651ac88b68eee5a01b088bedb9c54a",
      "RepoTags": ["ubuntu:quantal", "ubuntu:12.10"],
      "Size": 70975627,
      "VirtualSize": 172064416
    }, {
      "Created": 1403128435,
      "Id": "463ff6be4238c14f5b88898f17b47a9cf494f9a9be7b6170c3e852568d2b0432",
      "ParentId": "47dd6d11a49fc66a304bb679d545e64335cfb1f12dacf76c89e1cbe50af5574d",
      "RepoTags": ["ubuntu:13.04", "ubuntu:raring"],
      "Size": 70819643,
      "VirtualSize": 169359875
    }, {
      "Created": 1403128415,
      "Id": "195eb90b534950d334188c3627f860fbdf898e224d8a0a11ec54ff453175e081",
      "ParentId": "209ea56fda6dc2fb013e4d1e40cb678b2af91d1b54a71529f7df0bd867adc961",
      "RepoTags": ["ubuntu:13.10", "ubuntu:saucy"],
      "Size": 4260002,
      "VirtualSize": 184564423
    }, {
      "Created": 1398108230,
      "Id": "3db9c44f45209632d6050b35958829c3a2aa256d81b9a7be45b362ff85c54710",
      "ParentId": "6cfa4d1f33fb861d4d114f43b25abd0ac737509268065cdfd69d544a59c85ab8",
      "RepoTags": ["ubuntu:10.04", "ubuntu:lucid"],
      "Size": 182964289,
      "VirtualSize": 182964289
    }]
  },
  "dockerInstance": "sthmaclt009.local",
  "statusCode": 200
}
```

### List Containers
lists all running and stopped containers on this / a docker daemon instance. not the entire cluster!

Request
```
{
  "action": "list-containers"
}
```

Response
```
{
   "statusCode":200,
   "Content-Type":"application/json",
   "Date":"Sat, 15 Nov 2014 09:11:27 GMT",
   "Transfer-Encoding":"chunked",
   "Response":{
      "Body":[
         {
            "Command":"/bin/ping 8.8.8.8",
            "Created":1416042687,
            "Id":"35077fddff8f6e3b37a5db558f517401591d86ac9d2bd214383f8dfd9c58dd61",
            "Image":"ubuntu:14.04",
            "Names":[
               "/ecstatic_brown"
            ],
            "Ports":[

            ],
            "Status":""
         },
         {
            "Command":"/bin/ping 8.8.8.8",
            "Created":1416042687,
            "Id":"0265cf31ffae88e2aee2840a6ed17327ebdbf0e4e86b24a079e5bf9dcf8a7b61",
            "Image":"ubuntu:14.04",
            "Names":[
               "/romantic_ptolemy"
            ],
            "Ports":[
               {
                  "IP":"0.0.0.0",
                  "PrivatePort":22,
                  "PublicPort":49192,
                  "Type":"tcp"
               },
               {
                  "IP":"0.0.0.0",
                  "PrivatePort":80,
                  "PublicPort":49191,
                  "Type":"tcp"
               }
            ],
            "Status":"Up Less than a second"
         },
         {
            "Command":"/bin/ping 8.8.8.8",
            "Created":1416042686,
            "Id":"9f70454093d5a54822d7b2e2cf3ceeb2dc30e3d51725329a0054d7d57182a48d",
            "Image":"ubuntu:14.04",
            "Names":[
               "/mad_lumiere"
            ],
            "Ports":[
               {
                  "PrivatePort":80,
                  "Type":"tcp"
               },
               {
                  "PrivatePort":22,
                  "Type":"udp"
               }
            ],
            "Status":"Up Less than a second"
         }
      ]
   }
```

### Inspect Container
get all the details about a container, ports and everything!

Request
```
{
    "action": "inspect-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```

Curl Request
```
curl -X  POST --data '{"action": "inspect-container", "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

Response
```
{
    "Content-Length": "1526",
    "Content-Type": "application/json",
    "Date": "Tue, 18 Nov 2014 11:54:04 GMT",
    "Response": {
        "Body": {
            "Args": [
                "8.8.8.8"
            ],
            "Config": {
                "AttachStderr": true,
                "AttachStdin": false,
                "AttachStdout": true,
                "Cmd": [
                    "/bin/ping",
                    "8.8.8.8"
                ],
                "CpuShares": 0,
                "Cpuset": "",
                "Domainname": "",
                "Entrypoint": null,
                "Env": [
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                ],
                "ExposedPorts": {
                    "22/tcp": {},
                    "80/tcp": {}
                },
                "Hostname": "TESTNODE",
                "Image": "ubuntu",
                "Memory": 0,
                "MemorySwap": 0,
                "NetworkDisabled": false,
                "OnBuild": null,
                "OpenStdin": true,
                "PortSpecs": null,
                "SecurityOpt": null,
                "StdinOnce": false,
                "Tty": true,
                "User": "",
                "Volumes": null,
                "WorkingDir": ""
            },
            "Created": "2014-11-18T11:53:05.805794722Z",
            "Driver": "devicemapper",
            "ExecDriver": "native-0.2",
            "HostConfig": {
                "Binds": null,
                "CapAdd": null,
                "CapDrop": null,
                "ContainerIDFile": "",
                "Devices": null,
                "Dns": null,
                "DnsSearch": null,
                "ExtraHosts": null,
                "Links": null,
                "LxcConf": null,
                "NetworkMode": "",
                "PortBindings": null,
                "Privileged": false,
                "PublishAllPorts": true,
                "RestartPolicy": {
                    "MaximumRetryCount": 0,
                    "Name": ""
                },
                "VolumesFrom": null
            },
            "HostnamePath": "",
            "HostsPath": "",
            "Id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f",
            "Image": "5506de2b643be1e6febbf3b8a240760c6843244c41e12aa2f60ccbb7153d17f5",
            "MountLabel": "",
            "Name": "/backstabbing_mestorf",
            "NetworkSettings": {
                "Bridge": "",
                "Gateway": "",
                "IPAddress": "",
                "IPPrefixLen": 0,
                "MacAddress": "",
                "PortMapping": null,
                "Ports": null
            },
            "Path": "/bin/ping",
            "ProcessLabel": "",
            "ResolvConfPath": "",
            "State": {
                "ExitCode": 0,
                "FinishedAt": "0001-01-01T00:00:00Z",
                "Paused": false,
                "Pid": 0,
                "Restarting": false,
                "Running": false,
                "StartedAt": "0001-01-01T00:00:00Z"
            },
            "Volumes": null,
            "VolumesRW": null
        }
    },
    "dockerInstance": "app0126.proxmox.swe1.unibet.com",
    "statusCode": 200
}
```

### Create Raw Container

Usefule if you want to post raw Docker API container spec to the servers

Request
```
{
    "action": "create-raw-container",
    "body": json document as per docker API specs
}
```

Response
```
See Create Container
```

### Delete Container

Request
```
{
    "action": "delete-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```
Curl Request
```
curl -X   POST --data '{"action": "delete-container", "id": "0d0ff6977ed1767a01526d76e414349ef87840abfe81400c6e4d9ec8116111d4"}' localhost:8080   | python -mjson.tool
```

Response
```
{
    "Date": "Tue, 18 Nov 2014 15:48:57 GMT",
    "Response": "",
    "dockerInstance": "unisthlt033.unibet.com",
    "statusCode": 204
}
```

### Stop Container

Request
```
{
    "action": "stop-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```

Curl Request
```
 curl -X  POST --data '{"action": "stop-container", "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

### Restart Container

Request
```
{
    "action": "restart-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```


## Laws
* When a dockermod instance starts up, it sends/publishes a message to the cluster action:register. hostname:FQDN.
* some node on the cluster takes that message and adds the hostname to the list of known docks which is a shared object

### UI

The UI Mod Must
* remember the Docker Server hostname where a desired instance is hosted for targetted communication
* determine elegitilibty of a request / instance combination based on if the user has ownership rights of that instance
* talk to auth-mod-mgr
* never broadcast messages.
* request new instances to topic deblox.docker
* manipulate specific instsances to topic deblox.docker.FQDN