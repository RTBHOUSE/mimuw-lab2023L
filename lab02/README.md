# Database replication, load balancing and failover

In this laboratory we will implement replication, load balancing and failover for
PostgreSQL 14 database using *[Patroni](https://github.com/zalando/patroni)* project.

We will configure 3 PostgreSQL servers - a primary node and a two  standby replica. We
will also configure a HAproxy load balancer in front of PostgreSQL cluster. As a part of 
Patroni cluster we will set up etcd cluster with three nodes.
We will do some basic operations on the cluster to verify the correctness of replication and high availability features.


Roles:
etcd - store all metadata about PostgreSQL cluster, based on quorum, it makes decicion who is master.
PostgreSQL - three nodes with streaming replication, one master and two slaves. 
HAProxy - load balancer and additional matrix layer to forward SQL queries to proper node.
container - docker instance, example: demo-patroniX, demo-etcd
baseOS - it's a Virtual Machine example: st10Xvm10X.rtb-lab.pl

![Architecture](/lab02/patroni.png "source: https://github.com/zalando/patroni")

## 1. Prerequisites

Labolatory is based on docker environment. You just need to log into one of st10Xvm10X.rtb-lab.pl VM and follow commands in bash.

To have easy access for prepared files, clone the labs repository and install packages.

```bash
git clone https://github.com/RTBHOUSE/mimuw-lab2022Z.git
sudo apt install docker docker-compose postgresql-client
```

## 2. Setup new patroni cluster

Execute the below steps.

Add PostgreSQL repository, key and update the packages list:
```bash
cd lab02 
docker-compose up -d
```
Please confirm that all containers are up and running.

```bash
docker-compose ps
```
Output should be similar to 

```bash
    Name                   Command               State                                         Ports                                       
-------------------------------------------------------------------------------------------------------------------------------------------
demo-etcd1      /bin/sh /entrypoint.sh etcd      Up                                                                                        
demo-etcd2      /bin/sh /entrypoint.sh etcd      Up                                                                                        
demo-etcd3      /bin/sh /entrypoint.sh etcd      Up                                                                                        
demo-haproxy    /bin/sh /entrypoint.sh haproxy   Up      0.0.0.0:5000->5000/tcp,:::5000->5000/tcp, 0.0.0.0:5001->5001/tcp,:::5001->5001/tcp
demo-patroni1   /bin/sh /entrypoint.sh           Up                                                                                        
demo-patroni2   /bin/sh /entrypoint.sh           Up                                                                                        
demo-patroni3   /bin/sh /entrypoint.sh           Up                                                                                        
```
If the Output is different please raise your hand. 

## 3. patroni cluster 

On one of the patroni containers, demo-patroniX check cluster status.


```bash
docker exec -ti demo-patroni1 bash
postgres@patroni1:~$ patronictl list
```
Who is leader/master ? 

Now let's check patroni configuration

```bash
postgres@patroni1:~$ patronictl show-config
```

Before we you run next command please one again check who is leader. 

Imagine that current master needs to be shutdown for some maintenance reason. We need to use switchover command and delegate one of replica as new master. Keep in mind that we are still in the same container demo-patroniX.

```bash
patronictl failover
```

Patroni will ask you about new master. Example:

```bash
postgres@patroni1:~$ patronictl failover
Candidate ['patroni1', 'patroni3'] []: patroni1
Current cluster topology
+---------+----------+------------+--------+---------+----+-----------+
| Cluster |  Member  |    Host    |  Role  |  State  | TL | Lag in MB |
+---------+----------+------------+--------+---------+----+-----------+
|   demo  | patroni1 | 172.21.0.4 |        | running |  2 |         0 |
|   demo  | patroni2 | 172.21.0.6 | Leader | running |  2 |         0 |
|   demo  | patroni3 | 172.21.0.2 |        | running |  2 |         0 |
+---------+----------+------------+--------+---------+----+-----------+
Are you sure you want to failover cluster demo, demoting current master patroni2? [y/N]: y
2022-10-13 09:21:50.18813 Successfully failed over to "patroni1"
+---------+----------+------------+--------+---------+----+-----------+
| Cluster |  Member  |    Host    |  Role  |  State  | TL | Lag in MB |
+---------+----------+------------+--------+---------+----+-----------+
|   demo  | patroni1 | 172.21.0.4 | Leader | running |  2 |           |
|   demo  | patroni2 | 172.21.0.6 |        | stopped |    |   unknown |
|   demo  | patroni3 | 172.21.0.2 |        | running |  2 |         0 |
+---------+----------+------------+--------+---------+----+-----------+
```

What is State of old master ? 
How is new master ? 
What is Lag ? 

Your next task is about change PostgreSQL configuration. It's a little bit tricky, because PostgreSQL config is managed by patroni. Change max_commection from 100 to 150. Patroni will use vim to edit config, press "i" to switch to INSERT mode, use arrows to move to proper line and change value. When you finish click ESC and type ":x" to save change. 
```bash
postgres@patroni1:~$  patronictl edit-config
```

Wait few seconds and run the below command few times.
```bash
patronictl list
```

What did you notice ?


Change was accepted by cluster but not applied yet. As max_commection is critical parameter it can be changed only during the restart. 
Below command will restart PostgreSQL instance, repeat this command for all cluster members.
```bash
postgres@patroni1:~$ patronictl restart demo patroniX
```
Good practice is to restart master as the last one. 

Before we move forward, make sure that cluster is healty and all nodes applied configuration change. 

Log off from container. 

## 4. etcd

This part will help you understand what is role of etcd service in patroni cluster and what type of data it stores. 

Log into container demo-etcdX
```bash
docker exec -ti demo-extdX bash
postgres@etcd1:~$ etcdctl member list
```
How many nodes do you see ? 
Who is leader ? 

In the same container, run
```bash
postgres@etcd1:~$ curl http://etcd1:2380/members | jq .
```

Now, it's important to run below commands one by one. At the end you should find PATH where all metadata about nodes are stored. 
```bash
postgres@etcd1:~$ etcdctl ls
postgres@etcd1:~$ etcdctl ls /service
postgres@etcd1:~$ etcdctl ls /service/PATH
```
When you found path to members, please run 
```bash
postgres@etcd1:~$ etcdctl get PATH_TO_NODE/patroniX
```

What do you see ? 
What type of data are stored in etcd ? 

That was last task about etcd, now leave container and back to baseOS.

## 5. High Availability 

In this part we will prove that claster can survive without one node.

```bash
docker-compose ps
```

It' up to you which one do you kill. 
```bash
docker kill demo-patroniX
```

Confirm that node is down
```bash
docker exec -ti demo-patroniX patronictl list
```

How many nodes do you see ?
Who is leader ?

Now, let's check what happen if we kill two nodes at the time.
```bash
docker kill demo-patroniX
```

Make sure, that you run below command on running node. Repeat command few times and observe output.
```bash
docker exec -ti demo-patroniX patronictl list
```

What did you notice ? 
How long it took that cluster noticed that second node is down ? 
Who is leader ?

Start all nodes
```bash
docker-compose up -d
```

## 6. Replication

This part is dedicated to PostgreSQL replication. We will create table and confirm that is was replicated.

Find who is master
```bash
docker exec -ti demo-patroniX patronictl list
```

On master

```bash
docker exec -ti demo-patroniX
psql
```

Now you are in PostgreSQL.
Confirm replication status
```bash
postgres=# select * from pg_stat_replication ;
```

Create table test
```bash
postgres=# create table mimuw (id int);
postgres=#\dt
```

Leave docker and run command from baseOS

```bash
postgres=# \q
```

Below command run on slave node
```bash
docker exec -ti demo-patroniX psql
postgres=# select pg_is_in_recovery();
"

Output should be like below
```bash
 pg_is_in_recovery 
-------------------
 t
(1 row)
```

Confirm that tables exists on all nodes
```bash
docker exec -ti demo-patroniX psql
postgres=# \dt
"

Log off from container.

## 6. Optional tasks

Use postgres as password from user postgres. 
You need two bash consoles.

Console 1

```bash
psql -U postgres -p 5000 -h localhost
```
Do not kill this connection.

At the same time run commands in console 2. 
```bash
docker exec -ti demo-patroniX patronictl list
```

Find who is master and kill container.
```bash
docker kill demo-patroniX
```

Now look at Console 1, check if your connection is still alive ? 

## 7. Troubleshooting

As laboratory uses docker, you will not find logs files. It's much more easier if you use 
```bash
docker logs CONTAINER-NAME
``` 

In any corner case you can stop/start docker.
```bash
docker stop CONTAINER-NAME
docker start CONTAINER-NAME
```


