# samsara-kubernetes

Kubernetes setup for Samsara Analytics

## Description

This project contains the setup of Samsara on Kubernetes.

### Kebernetes on Vagrant

Install Kubernetes as described here: [here](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/getting-started-guides/vagrant.md)

Then run:

```bash
export KUBE_HOME=/your/kubernetes/home
export PATH=$KUBE_HOME/cluster:$PATH

export KUBERNETES_PROVIDER=vagrant
export NUM_MINIONS=4
 
# start cluster
kube-up.sh

# Then to start samsara run
./start-all.sh
 
# here some other general kubenrnetes commands

# list of minions
kubectl.sh get minions
 
# list of pods
kubectl.sh get pods
 
# list of services
kubectl.sh get services
 
# list of replication controllers
kubectl.sh get rc
 
```

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
