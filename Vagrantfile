Vagrant.configure("2") do |config|
    config.vm.box = "bento/fedora-31"
    config.vm.provider :virtualbox do |vb|
        vb.memory = "1024"
    end

    server_ip = "172.16.1.101"

    hosts = [
      { :name => "los-1", :ip => "172.16.1.101", :datacenter => "los", :server => true },
      { :name => "los-2", :ip => "172.16.1.102", :datacenter => "los", :server => false },
      { :name => "los-3", :ip => "172.16.1.103", :datacenter => "los", :server => false },
      { :name => "nys-1", :ip => "172.16.1.104", :datacenter => "nys", :server => false },
    ]

    hosts.each do |h|
        config.vm.define h[:name] do |host|
            host.vm.hostname = h[:name]
            host.vm.network :private_network, ip: h[:ip]

            host.vm.provision :shell do |s|
                s.path = "provisioning/provision.sh"
                s.env = {
                  "SERVER_IP" => server_ip,
                  "PRIVATE_IP" => h[:ip],
                  "DATACENTER" => h[:datacenter],
                }
                if h[:server]
                    s.args = ["--server"]
                end
            end

            if h[:server]
                # Nomad dashboard
                host.vm.network :forwarded_port, guest: 4646, host: 4646, auto_correct: true, host_ip: "127.0.0.1"
                # Consul dashboard
                host.vm.network :forwarded_port, guest: 8500, host: 8500, auto_correct: true, host_ip: "127.0.0.1"
                # Traefik dashboard
                host.vm.network :forwarded_port, guest: 8081, host: 8081, auto_correct: true, host_ip: "127.0.0.1"
                # Traefik main
                host.vm.network :forwarded_port, guest: 8080, host: 8080, auto_correct: true, host_ip: "127.0.0.1"
            end
        end
    end
end
