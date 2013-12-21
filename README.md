# deblox.Docker

Cluster your docker instances!

# Under heavy development.
Far from functional!

## Installation
this module should be run on every host that hosts your docker instances.
dockermod automatically clusters and using the webui on any one node should
grant instance control anywhere in the cluster.

## Json Messages
dockermod speaks json over messagebus.

### New Container
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