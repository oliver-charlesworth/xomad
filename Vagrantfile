Vagrant.configure("2") do |config|
    config.vm.box = "bento/fedora-31"
    config.vm.provider :virtualbox do |vb|
        vb.memory = "1024"
    end

    (1..3).each do |i|
        name = "los-#{i}"

        config.vm.define name do |host|
            host.vm.hostname = name
            host.vm.network :private_network, ip: "172.16.1.#{i + 100}"

            host.vm.provision :shell do |s|
                s.path = "provisioning/provision.sh"
                if i == 1
                    s.args = ["--server"]
                end
            end

            if i == 1
                host.vm.network :forwarded_port, guest: 4646, host: 4646, auto_correct: true, host_ip: "127.0.0.1"
                host.vm.network :forwarded_port, guest: 8500, host: 8500, auto_correct: true, host_ip: "127.0.0.1"
            end
        end
    end
end
