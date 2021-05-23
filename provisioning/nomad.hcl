data_dir = "/opt/nomad/data"
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
