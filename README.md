# DockerMod
(c) deBlox

A masterless, auto-discoverying clustering solution for Docker. No centralalized anything, utilizing VertX.io eventbus for distributed event processing.

DockerMod clusters all your Docker instances! It runs on each host which has a Docker daemon and self-cluster to create a distributed messagebus where docker instances can be controller from.

## Features

* All nodes are equal, no central super-nodes
* Zero persistent state information
* REST API
* Per container-id EventBus endpoints
* Clusterwide EventBus queries and enquiries
* Templated containers
* Fairly even distribution of bulk container creations


## Configuration and Defaults

#### Dockerd
Dockerd should be configured to listen on a tcp socket via the -H flag in /etc/defaults/docker or similar. e.g. -H 0.0.0.0:5555.

#### Dockermod Config
The config file is passed as an argument -conf "filename" when running either as a mod or in embedded mode. 

##### conf.json

```json
{
  "main": "com.deblox.docker.DockerMod",
  "services": ["com.deblox.docker.services.ContainerTrackingService", "com.deblox.docker.services.HttpService"],
  "clusterAddress": "deblox",
  "dockerHost": "localhost",
  "dockerPort": 5555,
  "announceInterval": 1250,
  "trackingServiceInterval": 10000,
  "taskTimeout": 2500
}
```

##### cluster.xml

Cluster discovery is configured via hazelcasts cluster.xml, in fatjar ( embedded ) mode, this file is built into the resources of the artifact, if running DockerMod as a vertx module, the cluster.xml needs to be placed within the $VERTX_HOME conf directory.

Cluster partitioning, members and ports are all configured via cluster.xml, see hazelcasts documentation for more details.

#### Templates

Containers can be described in templates which follow the Docker API create-container specs.

## Installation and Running

DockerMod only needs to be started on each host that runs a Docker daemon. It can either be launched as a vertx module or run as a fatjar.

**runmod**

```shell
vertx runmod com.deblox~dockermod~1.0.0.0-final -conf /some/conf.json -cluster -cluster-host myhostname
```

**fatjar**

```shell
java -jar docker-1.0.0-final-fat.jar -conf conf.json  -cluster  -cluster-host myhostname
```

## Json Messages

DockerMod speaks JSON over the EventBus and its HttpService.

## Running in dev mode

### Tests
./gradlew test -i

### Daemon
./gradlew runMod -i


## Json / REST messages

All requests can be done over EventBus or REST, all REST requests are directed to ANY DockerMod instance on the "/" context. 

Any event which is directed to a specific existing container in the cluster needs to be sent to a queue with the same name as the Id of the container. Using the REST API takes care of when to publish and when to send messages and how to route them, so the REST API is the preferred mechanism.

### Curl Examples

Create 4 containers based on a template

```shell
curl -X POST --data '{"action": "create-container", "template": "test", "instances": 4}' app0126.proxmox.swe1.unibet.com:8080 | python -m json.tool
```

Create a standard ubuntu container

```shell
curl -X POST --data '{"action": "create-container", "image": "ubuntu"}' app0126.proxmox.swe1.unibet.com:8080 | python -m json.tool
```


### Register DockerMod Instance

Announce a new DockerMod instance in the cluster, used when adding a Docker daemon to the pool.

Request

```json
{
  "action": "register",
  "hostname": "sthmaclt009.local"
}
```

No Response

### Unregister DockerMod Instance
removes the DockerMod instance from the pool, running instances are left running within docker daemon.

Request to `clusterAddress` or REST

```json
{
  "action": "unregister",
  "hostname": "sthmaclt009.local"
}
```

No Response

### Create Container

Create a new container either from a template or by specifying base image to use. 

When creating a new container, a new EventBus endpoint is created within the cluster with theId of the created container as the final endpoint. This endpoint is then used for directing requests to the DockerMod instance who "owns" that container. The HttpService's REST API takes care of routing messages to container endpoints whenever the 'id' field is present in a request.

When passing the instances argument with the request, multiple containers are requested in evenly distributed manner across the cluster, if the number of instances requested is greater than the cluster size, the remaining instances are spawned via the clusterAddress which would round-robin onto the various DockerMod instances. The response will contain a list of "containers" which contains the response from each DockerMod instance who participated in the request.

Templates are json documents which reside in the resources directory of the application, when template is specified, the json document is read and passed on to the Docker daemon in the create-container request. The template is the same syntax as the Docker create container json message in the Docker API specs.

Image is the base image to base the new container off, either template OR image must be specified.

Request

```json
{
    "action": "create-container",
    "template": "sometemplatename",
    "image": ubuntu,
    "instances": 2
}
```

Curl Request

```
curl -X   POST --data '{"action": "create-container", "template": "test", "instances": 2}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool

curl -X   POST --data '{"action": "create-container", "image": "ubuntu"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

Response if instances specified

```json
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

```json
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
Start a previously created container. This request needs to be sent to the EventBus endpoint with the same name as the container id. e.g the hash below. Or any REST API service. A response containing which instance your container is running on will be received.

Request

```json
{
    "action": "start-container",
    "id": "f1e3db7261a9f477577c46ba4c033a46678aafabd8c866c1dad70255e35a9ead"
}
```

Curl Request

```shell
curl -X POST --data '{"action": "start-container", "id": "b276fbe"}' app0126.proxmox.swe1.unibet.com:8080 | python -m json.tool
```

Response

```json
{
    "Date": "Mon, 17 Nov 2014 15:28:05 GMT",
    "Response": "",
    "dockerInstance": "app0127.proxmox.swe1.unibet.com",
    "statusCode": 204
}
```

### List Images
Returns a list of images on a specific DockerMod instance. These are images which have been downloaded to this instance at some point. 

TODO FIXME cluster-wide image requests.

Request

```json
{
  "action": "list-images"
}
```

Response

```json
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

lists all running and stopped containers on a specific or ALL Docker daemon instance(s). If "all" boolean is present in the request, a list fo all containers cluster-wide is received.

Request

```json
{
  "action": "list-containers",
  "all": true // query entire cluster
}
```

Curl Request

```shell
curl -X POST --data '{"action": "list-containers", "all": true}' app0126.proxmox.swe1.unibet.com:8080 | python -mjson.tool 
```

Response

```json
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
Get all the details about a container, ports mappings, and everything!

Request

```json
{
    "action": "inspect-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```

Curl Request

```shell
curl -X  POST --data '{"action": "inspect-container", "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

Response

```json
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

Usefull if you want to post raw Docker API container spec to the servers

Request

```json
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

```json
{
    "action": "delete-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```

Curl Request

```shell
curl -X   POST --data '{"action": "delete-container", "id": "0d0ff6977ed1767a01526d76e414349ef87840abfe81400c6e4d9ec8116111d4"}' localhost:8080   | python -mjson.tool
```

Response

```json
{
    "Date": "Tue, 18 Nov 2014 15:48:57 GMT",
    "Response": "",
    "dockerInstance": "unisthlt033.unibet.com",
    "statusCode": 204
}
```

### Stop Container
Stops the container

Request

```json
{
    "action": "stop-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```

Curl Request

```shell
 curl -X  POST --data '{"action": "stop-container", "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"}' app0126.proxmox.swe1.unibet.com:8080  | python -m json.tool
```

### Restart Container
Restarts the container

Request

```json
{
    "action": "restart-container",
    "id": "b276fbefc5c5d852a3c34a2999d8b26cb473e1a96ff611fe67f65f9cf635c70f"
}
```



### UI

The UI Mod Must:

* determine elegitilibty of a request / instance combination based on if the user has ownership rights of that instance
* talk to auth-mod-mgr
* never broadcast messages.
* request new instances to topic deblox.docker
* manipulate specific instsances to topic deblox.docker.FQDN