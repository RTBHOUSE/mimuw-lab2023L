# Database replication, load balancing and failover

In this laboratory we will implement replication, load balancing and failover for
PostgreSQL 14 database using *[Pgpool-II](http://pgpool.net)* project.

We will configure 2 PostgreSQL servers - a primary node and a single standby replica. We
will also configure a cluster of 2 Pgpool-II instances which will be running on the
same machines as the database servers. The Pgpool-II instances are responsible for
management of replication, load balancing, caching, connection pooling and failover of
the PostgreSQL instances. The Pgpool-II instances also present high-availability features like redundancy and failover and are able to automatically choose a leader instance in case of failure, what is transparent to external world due to the virtual ip
address which is dynamically shared between instances.

Then we will do some basic operations on the cluster to verify the correctness of
replication and high availability features.

![Architecture](/lab02/pgpool/cluster.gif "source: pgpool.net")

## 1. Prerequisites

You need to select the machines to serve as primary and standby for PostgreSQL
instances. Apart from that you will also have to select a Virtual IP for your Pgpool-II
instances. The exemplary choice could look like this:

| Address                | Purpose       |
| ---------------------- | ------------- |
| stXXXvm105.rtb-lab.pl  | *Primary*     |
| stXXXvm106.rtb-lab.pl  | *Standby*   |
| 10.112.XXX.111         | *Virtual IP* for universal access to Pgpool-II instances  |

Where 'XXX' is your student account number. Be aware to choose the last octet of the
Virtual IP address in the range of 111-255, smaller numbers are taken by your virtual
machines.

To have easy access for prepared files, clone the labs repository into both of your
machines:

```bash
git clone https://github.com/RTBHOUSE/mimuw-lab2023L.git
```


## 2. Install PostgreSQL and PgPool-II

Execute the below steps on both selected machines.

Add PostgreSQL repository, key and update the packages list:
```bash
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt -y update
```

Install the required packages for PostgreSQL:
```bash
sudo apt -y install gnupg2 wget vim postgresql-14
```

For the purpose of the laboratory set the password for the postgres user to 'postgres':
```bash
sudo passwd postgres
```

Copy the sudoers file that allows no password use of privileged commands for 'postgres'
user:

```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/sudoers.d/postgres /etc/sudoers.d
```

Add Pgpool-II repository, key and update the packages list:
```bash
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update
```

Install the required packages for Pgpool-II:
```bash
sudo apt-get -y install pgpool2 libpgpool2 postgresql-14-pgpool2 arping
```

## 3. Configure PostgreSQL

On both servers, create the directory to store WAL files:

```bash
sudo mkdir /var/lib/postgresql/archivedir
sudo chown postgres:postgres /var/lib/postgresql/archivedir
```

On both servers, copy the postgres configuration file:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/postgresql/14/main/postgresql.conf /etc/postgresql/14/main
sudo chown postgres:postgres /etc/postgresql/14/main/postgresql.conf
```

On primary server, create Pgpool-II and replication users, and set their passwords to
'postgres' (this password is only valid for laboratory purposes).
```bash
sudo su - postgres
psql -U postgres -p 5432
postgres=# SET password_encryption = 'scram-sha-256';
postgres=# CREATE ROLE pgpool WITH LOGIN;
postgres=# CREATE ROLE repl WITH REPLICATION LOGIN;
postgres=# \password pgpool
postgres=# \password repl
postgres=# \password postgres
postgres=# GRANT pg_monitor TO pgpool;
postgres=# \q
logout
```

On both servers, copy the postgres client authentication configuration file:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/postgresql/14/main/pg_hba.conf /etc/postgresql/14/main
sudo chown postgres:postgres /etc/postgresql/14/main/pg_hba.conf
```

On both servers, enable passwordless SSH to 'postgres' user for online recovery and
failover, remember to create private keys without password and to replace the placeholders
with correct values:
<ul>
  <li>&lt;PRIMARY DNS NAME&gt; - replace with the name of the server for primary node</li>
  <li>&lt;STANDBY DNS NAME&gt; - replace with the name of the server for standby replica node</li>
</ul>

```bash
sudo su - postgres
mkdir -p .ssh/
cd ~/.ssh
ssh-keygen -t rsa -f id_rsa_pgpool
ssh-copy-id -i id_rsa_pgpool.pub postgres@<PRIMARY DNS NAME>
ssh-copy-id -i id_rsa_pgpool.pub postgres@<STANDBY DNS NAME>
cp id_rsa_pgpool id_rsa
logout
```

On both servers, to allow streaming replication and recovery without specifying passwords
copy `.pgpass` file to 'postgres' user home directory:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/.pgpass /var/lib/postgresql
sudo chown postgres:postgres /var/lib/postgresql/.pgpass
sudo chmod 600 /var/lib/postgresql/.pgpass
```

On both servers, edit the `/var/lib/postgresql/.pgpass` file, and replace the placeholders with
correct values:
<ul>
  <li>&lt;PRIMARY DNS NAME&gt; - replace with the name of the server for primary node</li>
  <li>&lt;STANDBY DNS NAME&gt; - replace with the name of the server for standby replica node</li>
</ul>

## 4. Configure Pgpool-II

On each server create `pgpool_node_id` file.

On primary:
```bash
sudo bash -c "echo 0 > /etc/pgpool2/pgpool_node_id"
sudo chown postgres:postgres /etc/pgpool2/pgpool_node_id
```

On standby:
```bash
sudo bash -c "echo 1 > /etc/pgpool2/pgpool_node_id"
sudo chown postgres:postgres /etc/pgpool2/pgpool_node_id
```

On both servers create the Pgpool-II logging directory:
```bash
sudo mkdir /var/log/pgpool_log
sudo chown postgres:postgres /var/log/pgpool_log/
```


On both servers, copy the Pgpool-II configuration file. You may inspect the file
to find out what has been configured (each option has a documentation inside the file):
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/pgpool.conf /etc/pgpool2
sudo chmod 644 /etc/pgpool2/pgpool.conf
```

On both servers, edit the `/etc/pgpool2/pgpool.conf`, and replace the placeholders with
correct values:
<ul>
  <li>&lt;PRIMARY DNS NAME&gt; - replace with the name of the server for primary node</li>
  <li>&lt;STANDBY DNS NAME&gt; - replace with the name of the server for standby replica node</li>
  <li>&lt;VIRTUAL IP&gt; - replace with your selected virtual IP address.
</ul>

On both servers, copy the failover script (which promotes the new primary in case of master failure)
and follow script (which orders the replicas to follow a new primary):
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/failover.sh /etc/pgpool2
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/follow_primary.sh /etc/pgpool2
sudo chown postgres:postgres /etc/pgpool2/{failover.sh,follow_primary.sh}
sudo chmod 755 /etc/pgpool2/{failover.sh,follow_primary.sh}
```

On both servers, copy the Pgpool-II client authentication file (which
contains password for communication between Pgpool-II instances):
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/pcp.conf /etc/pgpool2
sudo chmod 644 /etc/pgpool2/pcp.conf
```

On both servers, to allow follow script to execute Pgpool-II commands on remote machines without
password, copy `.pcppass` file to 'postgres' user home directory:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/.pcppass /var/lib/postgresql
sudo chown postgres:postgres /var/lib/postgresql/.pcppass
sudo chmod 600 /var/lib/postgresql/.pcppass
```

On both servers, to allow Pgpool-II to communicate with backend PostgreSQL servers,
copy the `pg_hba.conf` and `pool_passwd` file to Pgpool-II configuration directory:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/pool_hba.conf /etc/pgpool2
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/pool_passwd /etc/pgpool2
sudo chmod 644 /etc/pgpool2/pool_hba.conf
sudo chmod 640 /etc/pgpool2/pool_passwd
```

Also on both servers, copy the password decryption key to the `postgres` user
home directory:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/.pgpoolkey /var/lib/postgresql
sudo chown postgres:postgres /var/lib/postgresql/.pgpoolkey
sudo chmod 600 /var/lib/postgresql/.pgpoolkey
```

On both servers, copy the escalation script which disables the virtual ip on
failed Pgpool-II instances:
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool2/escalation.sh /etc/pgpool2
sudo chown postgres:postgres /etc/pgpool2/escalation.sh
sudo chmod 755 /etc/pgpool2/escalation.sh
```

Edit the `escalation.sh` script and replace the placeholders with correct values as in `pgpool.conf` file:
<ul>
  <li>&lt;PRIMARY DNS NAME&gt; - replace with the name of the server for primary node</li>
  <li>&lt;STANDBY DNS NAME&gt; - replace with the name of the server for standby replica node</li>
  <li>&lt;VIRTUAL IP&gt; - replace with your selected virtual IP address.
</ul>


And lastly, let's configure the failed and new nodes recovery.

On primary server, copy the recovery and remote start scripts to the PostgreSQL data directory
(these scripts will be copied over to replicas as well):
```bash
sudo cp mimuw-lab2023L/lab02/pgpool/files/recovery_1st_stage /var/lib/postgresql/14/main
sudo cp mimuw-lab2023L/lab02/pgpool/files/pgpool_remote_start /var/lib/postgresql/14/main
sudo chown postgres:postgres /var/lib/postgresql/14/main/{recovery_1st_stage,pgpool_remote_start}
sudo chmod 744 /var/lib/postgresql/14/main/{recovery_1st_stage,pgpool_remote_start}
```

In order to use the online recovery functionality we need to install `pgpool_recovery`
extension on PostgreSQL primary server:
```bash
sudo su - postgres
psql template1 -c "CREATE EXTENSION pgpool_recovery"
logout
```

## 5. Run and test

Note: according to the prepared configuration, every time you will be asked to enter password
to run some commands, you should provide "`postgres`"


### 5.1. Start the Pgpool-II instances

On both servers restart the postgresql service:

```bash
sudo systemctl restart postgresql
```

On both servers, execute the startup command for Pgpool-II:

```bash
sudo systemctl start pgpool2
```

Examine the logs in `/var/log/pgpool_log` to check if there are no critical errors.

### 5.2 Bring up the standby replica node

Log on to the standby server and disable the database instance:
```bash
sudo pg_ctlcluster 14 main stop
```

Log on to the primary server and start the recovery (initialization) of the standby server,
of course replace the placeholder value with your virtual ip address:
```bash
sudo pcp_recovery_node -h <VIRTUAL IP> -p 9898 -U pgpool -n 1
Password:
pcp_recovery_node -- Command Successful
```

Verify that the standby has been brought up:
```bash
psql -h <VIRTUAL IP> -p 9999 -U pgpool postgres -c "show pool_nodes"
node_id |       hostname        | port | status | pg_status | lb_weight |  role   | pg_role | select_cnt | load_balance_node | replication_delay | replication_state | replication_sync_state | last_status_change
---------+-----------------------+------+--------+-----------+-----------+---------+---------+------------+-------------------+-------------------+-------------------+------------------------+---------------------
 0       | stXXXvmXXX.rtb-lab.pl | 5432 | up     | up        | 0.500000  | primary | primary | 0          | false             | 0                 |                   |                        | 2022-03-30 15:06:58
 1       | stXXXvmXXY.rtb-lab.pl | 5432 | up     | up        | 0.500000  | standby | standby | 0          | true              | 0                 |                   |                        | 2022-03-30 13:53:47

```

### 5.3 Create test table and check replication

Create test database, schema and table through Pgpool-II proxy and insert a row.

```bash
psql -h <VIRTUAL IP> -p 9999 -U postgres

postgres=# CREATE DATABASE test;
CREATE DATABASE
postgres=# \c test
You are now connected to database "test" as user "postgres".
test=# CREATE schema test;
CREATE SCHEMA
test=# CREATE TABLE test.test(id integer PRIMARY KEY, value integer NOT NULL);
CREATE TABLE
test=# INSERT INTO test.test values(1, 1);
INSERT 0 1
test=#\q
```

Now log on to the standby server and verify that the data has been replicated by
connecting and querying directly on the replica node:

```bash
sudo su - postgres
psql -p 5432 -U postgres test -c "select * from test.test"
id | value
----+-------
 1 |     1
(1 row)
logout
```

### 5.4 Load balancing queries

Remember the current `select_cnt` from pool nodes status on Pgpool-II cluster:
```bash
psql -h <VIRTUAL IP> -p 9999 -U pgpool postgres -c "show pool_nodes"
node_id |       hostname        | port | status | pg_status | lb_weight |  role   | pg_role | select_cnt | load_balance_node | replication_delay | replication_state | replication_sync_state | last_status_change
---------+-----------------------+------+--------+-----------+-----------+---------+---------+------------+-------------------+-------------------+-------------------+------------------------+---------------------
 0       | stXXXvmXXX.rtb-lab.pl | 5432 | up     | up        | 0.500000  | primary | primary | 0          | false             | 0                 |                   |                        | 2022-03-30 15:06:58
 1       | stXXXvmXXY.rtb-lab.pl | 5432 | up     | up        | 0.500000  | standby | standby | 0          | true              | 0                 |                   |                        | 2022-03-30 13:53:47
```

Run the below script which connects to the Pgpool-II proxy via virtual ip and generates read queries:
```bash
for i in `seq 1 10`; do PGPASSWORD=postgres psql -h <VIRTUAL IP> -p 9999 -U postgres test -c "select * from test.test"; done
```

Verify that the values of `select_cnt` have changed for both primary and standby.

Now run the below script which generates update queries:
```bash
for i in `seq 1 10`; do PGPASSWORD=postgres psql -h <VIRTUAL IP> -p 9999 -U postgres test -c "update test.test set value=$i where id=1"; done
```

Verify that the `select_cnt` values haven't changed at all (since the DML queries are
only executed on the primary server).


### 5.5 Pgpool-II high availability and failover

On any server check the Pgpool-II cluster status:
```bash
sudo pcp_watchdog_info -h <VIRTUAL IP> -p 9898 -U pgpool
Password:
2 2 YES stXXXvmXXX.rtb-lab.pl:9999 Linux stXXXvmXXX stXXXvmXXX.rtb-lab.pl

stXXXvmXXX.rtb-lab.pl:9999 Linux stXXXvmXXX stXXXvmXXX.rtb-lab.pl 9999 9000 4 LEADER 0 MEMBER
stXXXvmXXY.rtb-lab.pl:9999 Linux stXXXvmXXY stXXXvmXXY.rtb-lab.pl 9999 9000 7 STANDBY 0 MEMBER
```

Now log in to the server where cluster leader is running and shut it down:
```bash
sudo systemctl stop pgpool2
```

Verify that the new leader has been elected:
```bash
sudo pcp_watchdog_info -h <VIRTUAL IP> -p 9898 -U pgpool
Password:
2 2 YES stXXXvmXXY.rtb-lab.pl:9999 Linux stXXXvmXXY stXXXvmXXY.rtb-lab.pl

stXXXvmXXY.rtb-lab.pl:9999 Linux stXXXvmXXY stXXXvmXXY.rtb-lab.pl 9999 9000 4 LEADER 0 MEMBER
stXXXvmXXX.rtb-lab.pl:9999 Linux stXXXvmXXX stXXXvmXXX.rtb-lab.pl 9999 9000 10 SHUTDOWN 0 MEMBER
```

Verify that the proxy still accepts and runs queries:
```bash
psql -h 10.112.102.111 -p 9999 -U postgres test -c "select * from test.test"
 id | value
----+-------
  1 |     10
(1 row)
```

Re-enable the disabled Pgpool-II instance:
```bash
sudo systemctl start pgpool2
```

You can check if the leader of the Pgpool-II cluster has changed.

### 5.6 PostgreSQL failover and recovery

Now we will kill the primary node and Pgpool-II should automatically promote the standby
server to master.

Log in to the primary server and kill the database:
```bash
su - postgres
/usr/lib/postgresql/14/bin/pg_ctl -D /var/lib/postgresql/14/main/ -m immediate stop
logout
```

Verify that the replica has been promoted to primary status:
```bash
psql -h <VIRTUAL IP> -p 9999 -U pgpool postgres -c "show pool_nodes"
node_id |       hostname        | port | status | pg_status | lb_weight |  role   | pg_role | select_cnt | load_balance_node | replication_delay | replication_state | replication_sync_state | last_status_change
---------+-----------------------+------+--------+-----------+-----------+---------+---------+------------+-------------------+-------------------+-------------------+------------------------+---------------------
 0       | stXXXvmXXX.rtb-lab.pl | 5432 | down   | down      | 0.500000  | standby | unknown | 0          | false             | 0                 |                   |                        | 2022-04-01 16:06:07
 1       | stXXXvmXXY.rtb-lab.pl | 5432 | up     | up        | 0.500000  | primary | primary | 0          | true              | 0                 |                   |                        | 2022-04-01 16:06:07
```

Finally it is time to recover the previous master to standby replica state:
```bash
pcp_recovery_node -h <VIRTUAL IP> -p 9898 -U pgpool -n 0
```

```bash
psql -h <VIRTUAL IP> -p 9999 -U pgpool postgres -c "show pool_nodes"
 node_id |       hostname        | port | status | pg_status | lb_weight |  role   | pg_role | select_cnt | load_balance_node | replication_delay | replication_state | replication_sync_state | last_status_change
---------+-----------------------+------+--------+-----------+-----------+---------+---------+------------+-------------------+-------------------+-------------------+------------------------+---------------------
 0       | stXXXvmXXX.rtb-lab.pl | 5432 | up     | up        | 0.500000  | standby | standby | 0          | false             | 0                 |                   |                        | 2022-04-01 16:15:14
 1       | stXXXvmXXY.rtb-lab.pl | 5432 | up     | up        | 0.500000  | primary | primary | 0          | true              | 0                 |                   |                        | 2022-04-01 16:06:07
(2 rows)
```

### 5.7 Server detachment

If we want to perform some maintenance on the server, we can detach it gracefully
using `pcp_detach_node` command and then optionally attach it using the
`pcp_attach_node` command.

The cluster behavior depends on the role of the server:
  - If we detach the primary node then the Pgpool-II cluster performs failover and the previous primary is not considered a working standby nor the replication is working. If we want to attach it as a standby and enable replication, then we must use the `pcp_recovery_node` command.
  - If we detach the standby node, then it is simply not governed by the Pgpool-II cluster. There are no client connections to it, but the streaming replication is working. We may do some maintenance on the server and depending on the state of the
  data either perform `pcp_attach_node` (no data lost) or `pcp_recover_node` (data is lost).

Try to do both of these scenarios using the commands that have been covered so far.

## 6. Optional tasks

Here are some ideas for additional tasks that can be done:

<ol>
  <li>Add second standby replica.</li>
  <li>Set different priorities/load-balancing weights to PostgreSQL servers and observe
  the load balancing ratio for read queries.</li>
  <li>Enable query caching on Pgpool-II cluster.</li>
  <li>Create Ansible playbook to automate the whole setup.</li>
  <li>Dockerize PostgreSQL and Pgpool-II configuration.</li>
</ol>

## 7. Troubleshooting

Always consult the log files in  `/var/log/postgresql` for PostgreSQL and `/var/log/pgpool_log`
for Pgpool-II, to look for configuration errors.

Check other PCP commands at <https://www.pgpool.net/docs/42/en/html/pcp-commands.html>.
Especially if the primary is reported with `down` status but actually is working, use
the `pcp_attach_node` command.

Pgpool-II prevents the shutdown of the PostgreSQL server using `systemctl stop postgresql`. The command to disable the server without turning off the Pgpool-II is
`pg_ctlcluster 14 main stop`.
