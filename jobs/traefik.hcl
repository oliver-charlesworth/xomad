job "traefik" {
  datacenters = ["los"]

  type = "service"

  group "traefik" {
    count = 1

    service {
      name = "traefik"
      port = "http"

      check {
        type = "http"
        port = "internal"
        path = "/ping"
        interval = "10s"
        timeout = "5s"
      }
    }

    network {
      port "http" { static = 8080 }
      port "internal" { static = 8081 }
    }

    task "app" {
      driver = "exec"

      artifact {
        source = "https://github.com/traefik/traefik/releases/download/v2.4.8/traefik_v2.4.8_linux_amd64.tar.gz"
        options {
          checksum = "md5:42efb8cd194fa8b01f4e78473092631f"
        }
      }

      config {
        command = "${NOMAD_TASK_DIR}/traefik"
      }

      resources {
        cpu = 300
        memory = 64
      }

      # Pin to host that has port forwarding defined
      constraint {
        attribute = "${attr.unique.hostname}"
        value = "los-1"
      }

      template {
        data = <<EOF
[entryPoints]
    [entryPoints.http]
    address = "0.0.0.0:8080"
    [entryPoints.traefik]
    address = "0.0.0.0:8081"

[api]
    dashboard = true
    insecure  = true

[pilot]
    dashboard = false

[ping]
[log]
[accessLog]
[providers.consulCatalog]
    exposedByDefault = false
    # See https://community.traefik.io/t/name-returns-no-value-with-consul-catalog/5465/13
    defaultRule = "Path(`/{{"{{ .Name }}"}}`)"
EOF

        destination = "/etc/traefik/traefik.toml"
      }
    }
  }
}
