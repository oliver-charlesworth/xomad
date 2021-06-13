data_dir = "/opt/nomad/data"
# TODO - fix the the address we bind to (initially comes up as 127.0.0.1 on all clients)
bind_addr = "0.0.0.0"
datacenter = "los"

advertise {
  http = "{{ GetInterfaceIP `eth1` }}"
  rpc  = "{{ GetInterfaceIP `eth1` }}"
  serf = "{{ GetInterfaceIP `eth1` }}"
}

client {
  enabled = true
  network_interface = "eth1"
  servers = ["172.16.1.101"]
}
