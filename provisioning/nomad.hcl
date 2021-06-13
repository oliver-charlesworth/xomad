data_dir = "/opt/nomad/data"
bind_addr = "0.0.0.0"
datacenter = "los"

advertise {
  http = "PRIVATE_IP"
  rpc  = "PRIVATE_IP"
  serf = "PRIVATE_IP"
}

client {
  enabled = true
  network_interface = "eth1"
  servers = ["172.16.1.101"]
}
