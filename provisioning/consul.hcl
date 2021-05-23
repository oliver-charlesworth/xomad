data_dir = "/opt/consul"
bind_addr = "0.0.0.0"
datacenter = "los"

advertise_addr = "{{ GetInterfaceIP `eth1` }}"

client_addr = "0.0.0.0"
retry_join = ["172.16.1.101"]
