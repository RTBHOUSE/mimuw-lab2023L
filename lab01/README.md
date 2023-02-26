# Introduction

The aim of these exercises is to familiarize students with infrastructure that has been shared by RTB House. Every student is given access to 10 identical virtual machines within RTB House premises. These VMs will be useful during the rest of the laboratories and the final project will be required to run on these resources.


## 1. VPN Connection to RTB House

Firstly, the connection to the RTB House virtual machines has to be established. Follow the instructions here:
[https://mimuw.rtbhouse.com/OpenVPN.pdf](https://mimuw.rtbhouse.com/OpenVPN.pdf)


You can also log in with your credentials to the VM management service at [https://mimjenkins.rtb-lab.pl/](https://mimjenkins.rtb-lab.pl/).
This service allows you to restore the selected VM’s to their initial state - beware, all data will be lost!

## 2. Infrastructure Management with Ansible

The aim of this exercise is to create an Ansible playbook that will orchestrate the installation of docker engine on students virtual machines. Later you will have to create an Ansible playbook to deploy a docker registry instance. After that you will have to create an Ansible playbook that will connect the machines in a docker swarm cluster. Finally you will have to prepare a simple http application, containerize it, and then run as docker swarm service.

### 2.1 Docker runtime installation

1. Log into one of your machines, preferably the first available one:

```bash
ssh <username>@<username>vm101.rtb-lab.pl
```

2. Install Ansible and sshpass:

```bash
sudo apt -y install ansible sshpass
```

3. Add all of your machines to ssh known hosts:

```bash
for i in `seq -w 01 10`; do sshpass -p <password> ssh <username>@<username>vm1$i.rtb-lab.pl -o StrictHostKeyChecking=no -C "/bin/true"; done
```

4. Edit the `hosts` file for Ansible do define docker nodes group:

```bash
sudo nano /etc/ansible/hosts
```

<div style="background-color: rgb(0, 0, 0);">
[docker]<br/>
&lt;username&gt;vm1[01:10].rtb-lab.pl
</div>
<br>

5. Check that ansible works:

```bash
ansible docker -m ping  --extra-vars "ansible_user=<username> ansible_password=<password>"
```

6. Create docker.service override file to enable docker external port access:

```bash
nano docker.service
```

<div style="background-color: rgb(0, 0, 0);">
[Service]<br>
ExecStart=<br>
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375<br>
</div>
<br>

7. Create docker installation playbook file:

```bash
nano docker-playbook.yaml
```

```yaml
---
- name: Docker install
  hosts: docker
  tasks:
     - name: Install the latest version of Docker
       become: true
       become_user: root
       yum:
             name: docker.io
             state: latest
     - name: Create directory
       become: true
       ansible.builtin.file:
              path: /etc/systemd/system/docker.service.d
              state: directory
     - name: Copy docker service file with owner and permissions
       become: true
       register: service_conf
       ansible.builtin.copy:
             src: docker.service
             dest: /etc/systemd/system/docker.service.d/override.conf
             owner: root
             group: root
             mode: '0644'
     - name: Ensure the docker daemon is enabled
       become: true
       become_user: root
       systemd:
             name: docker
             state: started
             enabled: yes
             daemon_reload: yes
     - name: Restart daemon on config change
       become: true
       become_user: root
       systemd:
             name: docker
             state: restarted
       when: service_conf.changed
```


8. Run docker installation playbook file:

```bash
ansible-playbook --extra-vars "ansible_user=<username> ansible_password=<password>" docker-playbook.yaml
```

9. Verify that the local and remote docker runtimes are working:

```bash
DOCKER_HOST=”<username>vm102.rtb-lab.pl” sudo docker info
```

### 2.2 Docker Registry

1. Add docker registry group to `host` file with one selected host (preferably <username>vm101):

```bash
sudo nano /etc/ansible/hosts
```

<div style="background-color: rgb(0, 0, 0);">
[registry]<br>
&lt;username&gt;vm101.rtb-lab.pl<br><br>
</div>
<br>

2. Create the registry playbook file:

```bash
nano registry-playbook.yaml
```


```yaml
---
- name: Docker registry install
  hosts: registry

  tasks:
     - name: Create certs directory
       become: true
       ansible.builtin.file:
              path: /etc/docker/certs
              state: directory

     - name: Create registry certs directory
       become: true
       ansible.builtin.file:
               path: /etc/docker/cert.d/{{inventory_hostname}}:5000
               state: directory

     - name: Create selfsigned cert
       become: true
       command: openssl req -newkey rsa:4096 -nodes -sha256 -subj "/C=PL/ST=Warsaw/L=Warsaw/O=Global Security/OU=RTB Department/CN=rtb-lab.pl" -keyout /etc/docker/certs/ca.key -addext "subjectAltName = DNS:{{inventory_hostname}}" -x509 -days 365 -out /etc/docker/certs/ca.crt

     - name: Copy selfsigned cert to system CA
       become: true
       command: cp /etc/docker/certs/ca.crt /usr/local/share/ca-certificates/{{inventory_hostname}}.crt

     - name: Copy selfsigned cert to docker registry CA
       become: true
       command: cp /etc/docker/certs/ca.crt /etc/docker/cert.d/{{inventory_hostname}}:5000/ca.crt

     - name: Update CA certs
       become: true
       command: update-ca-certificates

     - name: Restart docker daemon
       become: true
       become_user: root
       systemd:
             name: docker
             state: restarted

     - name: Start registry
       become: true
       become_user: root
       command: docker run -d --restart=always --name registry  -v /etc/docker/certs:/certs -e REGISTRY_HTTP_ADDR=0.0.0.0:443 -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/ca.crt -e REGISTRY_HTTP_TLS_KEY=/certs/ca.key -p 443:443 registry:2

- name: Sync Push task - Executed on source host "{{groups['registry'][0]}}"
  hosts: docker:!registry


  tasks:

     - name: Create registry certs directory
       become: true
       ansible.builtin.file:
          path: /etc/docker/cert.d/{{groups['registry'][0]}}:5000
          state: directory


     - name: Copy the CA file from registry to docker hosts using push method
       become: true
       tags: sync-push
       synchronize:
             src: "{{ item }}"
             dest: "{{ item }}"
             mode: push
       delegate_to: "{{groups['registry'][0]}}"
       register: syncfile
       with_items:
        - "/etc/docker/cert.d/{{groups['registry'][0]}}:5000/ca.crt"
        - "/usr/local/share/ca-certificates/{{groups['registry'][0]}}.crt"

     - name: Update CA certs
       become: true
       command: update-ca-certificates

     - name: Restart docker daemon
       become: true
       become_user: root
       systemd:
             name: docker
             state: restarted

```

3. Run docker regisrty installation playbook file:

```bash
ansible-playbook --extra-vars "ansible_user=<username> ansible_password=<password>" registry-playbook.yaml
```

4. Verify that the registry is working by pulling the `debian:bullseye-slim` image, tagging it with regard to the schema registry, and pushing it back:

```bash
sudo docker pull debian:bullseye-slim
sudo docker tag debian:bullseye-slim <username>vm101.rtb-lab.pl/debian:bullseye-slim
sudo docker push <username>vm101.rtb-lab.pl/debian:bullseye-slim
```

Now log in to a different machine and try to pull the image from the schema registry:

```bash
sudo docker pull <username>vm101.rtb-lab.pl/debian:bullseye-slim
```

### 2.3 Docker Swarm cluster

1. Add swarm manager and swarm worker groups to `host` file:

```bash
sudo nano /etc/ansible/hosts
```

<div style="background-color: rgb(0, 0, 0);">
[swarm_manager]<br>
&lt;username&gt;vm101.rtb-lab.pl<br><br>
[swarm_nodes]<br>
&lt;username&gt;vm1[02:10].rtb-lab.pl<br>
</div>
<br>

2. Create swarm playbook file:

```bash
nano swarm-playbook.yaml
```


```yaml
---
- name: Docker Swarm init
  any_errors_fatal: true
  hosts: swarm_manager
  remote_user: root
  tasks:
     - name: Ensure the docker daemon is running
       become: true
       become_user: root
       command: systemctl is-active docker
       register: docker_status
       failed_when: docker_status.stdout_lines[0] != "active"
     - name: Init Docker Swarm
       become: true
       become_user: root
       command: docker swarm init --advertise-addr {{ ansible_facts.eth0.ipv4.address }}
       register: swarm_init
       failed_when: swarm_init.rc != 0
     - name: Get join token
       become: true
       become_user: root
       command: docker swarm join-token -q worker
       register: join_token
       failed_when: join_token.rc != 0
- name: Swarm nodes register
  any_errors_fatal: true
  hosts: swarm_nodes
  remote_user: root
  tasks:
     - name: Ensure the docker daemon is running
       become: true
       become_user: root
       command: systemctl is-active docker
       register: docker_status
       failed_when: docker_status.stdout_lines[0] != "active"
     - name: Join swarm
       become: true
       become_user: root
       command: docker swarm join --token {{ hostvars[groups['swarm_manager'][0]]['join_token']['stdout_lines'][0] }} {{ hostvars[groups['swarm_manager'][0]]['ansible_eth0']['ipv4']['address'] }}:2377
       register: join_status
       failed_when: join_status.rc != 0
```

3. Run swarm installation playbook file:

```bash
ansible-playbook --extra-vars "ansible_user=<username> ansible_password=<password>" swarm-playbook.yaml
```

4. Check if swarm is active:

```bash
sudo docker info | grep Swarm -A 25
```

## 3. Create simple HTTP service

Build a simple HTTP service. The purpose of the service is to return via simple GET request the IP address of the underlying machine. Remember to dockerize the application and to push it to the docker registry.

#### Advices

* You can use Python and the [FastAPI](https://fastapi.tiangolo.com/tutorial/) library to quickly develop the service.
* When editing a Dockerfile prefix the names of the base images with `mirror.gcr.io/library/` int the `FROM` clause to alleviate the image download rate limiting on Docker Hub.
* The IP address can be retrieved by a `hostname -I | awk '{print $1}'` command.


1. Build the service container with tagging according to our private docker registry and then push it:

```bash
sudo docker build -t <username>vm101.rtb-lab.pl/appserver:latest .
sudo docker push <username>vm101.rtb-lab.pl/appserver:latest
```

2. Start the service:

```bash
sudo docker service create --name appserver -p 8000:8000 -d <username>vm101.rtb-lab.pl/appserver:latest
```

3. Check if the service works:

```bash
curl <username>vm101.rtb-lab.pl:8000
```

It should reply with the IP address of the container.

4. Scale the service to 10 replicas:

```bash
sudo docker service scale appserver=10
```

5. Verify that the swarm manager uses ingress load balancing on the service (the address of server is changing on every request):

```bash
for i in `seq 1 10`; do curl <username>vm101.rtb-lab.pl:8000; done
```

6. Remove the service:

```bash
sudo docker service rm appserver
```

#### Hints

In case you wanted to change something in your app:

* rebuild and push the Docker image as described above
* perform a rolling update on a swarm service: `sudo docker service update --image <username>vm101.rtb-lab.pl/appserver:latest appserver`



## 3. Additional tasks

Deploy the above service using the [Docker Compose](https://docs.docker.com/compose/).
