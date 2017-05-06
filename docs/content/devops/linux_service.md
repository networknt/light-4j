---
date: 2017-01-07T08:03:47-05:00
title: Running Light-4J Application as Linux Service
---

Light-4J application can be easily started as Unix/Linux services using either init.d or systemd given it
is packaged as a fatjar. 

## Installation as an init.d service (System V)

If your application is packaged as a fatjar, and you’re not using a custom script, then your application 
can be used as an init.d service. Simply symlink the jar to init.d to support the standard start, stop, 
restart and status commands.

The script supports the following features:

* Starts the services as the user that owns the jar file
* Tracks application’s PID using /var/run/<appname>/<appname>.pid
* Writes console logs to /var/log/<appname>.log

Assuming that you have a Light Java application installed in /var/myapp, to install the application as an 
init.d service simply create a symlink:

```
sudo ln -s /var/myapp/myapp.jar /etc/init.d/myapp
```

Once installed, you can start and stop the service in the usual way. For example, on a Debian based system:

```
service myapp start
```

You can also flag the application to start automatically using your standard operating system tools. For 
example, on Debian:

```
update-rc.d myapp defaults <priority>
```

### Securing an init.d service

When executed as root, as is the case when root is being used to start an init.d service, the default 
executable script will run the application as the user which owns the jar file. You should never run 
a Light Java application as root so your application’s jar file should never be owned by root. Instead, 
create a specific user to run your application and use chown to make it the owner of the jar file. For 
example:

```
chown bootapp:bootapp your-app.jar
```

In this case, the default executable script will run the application as the bootapp user.

To reduce the chances of the application’s user account being compromised, you should consider preventing 
it from using a login shell. Set the account’s shell to /usr/sbin/nologin, for example.

You should also take steps to prevent the modification of your application’s jar file. Firstly, configure 
its permissions so that it cannot be written and can only be read or executed by its owner:

```
chmod 500 your-app.jar
```

Secondly, you should also take steps to limit the damage if your application or the account that’s running 
it is compromised. If an attacker does gain access, they could make the jar file writable and change its 
contents. One way to protect against this is to make it immutable using chattr:

```
sudo chattr +i your-app.jar
```

This will prevent any user, including root, from modifying the jar.

f root is used to control the application’s service and you use a .conf file to customize its startup, 
the .conf file will be read and evaluated by the root user. It should be secured accordingly. Use chmod 
so that the file can only be read by the owner and use chown to make root the owner:

```
chmod 400 your-app.conf
sudo chown root:root your-app.conf
```

