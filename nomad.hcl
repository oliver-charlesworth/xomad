data_dir = "/opt/nomad/data"
bind_addr = "0.0.0.0"
datacenter = "los"

advertise {
  http = "{{ GetInterfaceIP `eth1` }}"
  rpc  = "{{ GetInterfaceIP `eth1` }}"
  serf = "{{ GetInterfaceIP `eth1` }}"
}