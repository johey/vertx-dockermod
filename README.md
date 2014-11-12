# deblox.Docker

Cluster your docker instances! DockerMod runs on each host which has a docker daemon running, and the DockerMod instances then self-cluster to create a messagebus where docker instances can be controller from.

# Under heavy development.
Far from anywhere!

## Installation
This module should be run on every host that runs docker daemon.
DockerMod automatically clusters and using the webui on any one node should
grant instance control anywhere in the cluster.

## Json Messages
DockerMod speaks json over messagebus.


## Running (dev mode)

### Tests
./gradlew test -i

### Daemon
./gradlew runMod -i

### FatJar cluster
 /opt/jdk1.7.0_72/bin/java -jar docker-1.0.0-final-fat.jar -conf conf.json  -cluster   -cluster-host app0126.proxmox.swe1.unibet.com

### Register DockerMod Instance
announce a new DockerMod instance to the cluster, used when adding a docker daemon to the pool.

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
new containers are requested with messages like:

Request
```
{
    "action": "create-container",
    "image": "ubuntu"
}
```

Response
```
{
    "Id": "f1e3db7261a9f477577c46ba4c033a46678aafabd8c866c1dad70255e35a9ead"
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


{"action":"inspect-container","id":"foo"}

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