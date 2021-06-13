data_dir = "/opt/consul"
# TODO - fix the the address we bind to (initially comes up as 127.0.0.1 on all clients)
bind_addr = "0.0.0.0"
datacenter = "los"

advertise_addr = "{{ GetInterfaceIP `eth1` }}"

client_addr = "0.0.0.0"
retry_join = ["172.16.1.101"]
