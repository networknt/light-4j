---
date: 2017-03-18T20:37:48-04:00
title: Minikube Installation
---

As light-4j API/service is aiming to deployed with Docker containers and
Kubernetes is the most popular container orchestration tool. It is very hard
to setup multiple nodes cluster on laptop development environment but there
is a minikube can be used on laptop to create one node cluster on your laptop.

## Install on Mac.


### Install xhype
Before installing Minikube, you must install [xhype](https://github.com/zchee/docker-machine-driver-xhyve#install) 
driver first. The detailed instructions are in the above link and here is the
commands to be executed.

```
brew install docker-machine-driver-xhyve
sudo chown root:wheel $(brew --prefix)/opt/docker-machine-driver-xhyve/bin/docker-machine-driver-xhyve
sudo chmod u+s $(brew --prefix)/opt/docker-machine-driver-xhyve/bin/docker-machine-driver-xhyve
```

### Install minikube

The general installation guide is in the [README](https://github.com/kubernetes/minikube) 
and the latest instruction is always specific for the latest release. The current
release instruction can be found [here](https://github.com/kubernetes/minikube/releases)

```
curl -Lo minikube https://storage.googleapis.com/minikube/releases/v0.17.1/minikube-darwin-amd64 && chmod +x minikube && sudo mv minikube /usr/local/bin/
```

### Install kubectl

```
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/darwin/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
```


### Start minikube

```
minikube start
```

## Install on Linux


