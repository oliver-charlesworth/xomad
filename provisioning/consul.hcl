data_dir = "/opt/consul"
bind_addr = "0.0.0.0"
datacenter = "global"

advertise_addr = "$PRIVATE_IP"

client_addr = "0.0.0.0"
retry_join = ["$SERVER_IP"]
