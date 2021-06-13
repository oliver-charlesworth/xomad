locals {
  version = "0.0.13"
}

# TODO - log files
# TODO - readiness check
# TODO - check_restart + restart policy
# TODO - migrate / reschedule policies
# TODO - outbound TLS sidecar (stunnel replacement)
# TODO - multiple datacenters
# TODO - #core resources
# TODO - host_volume for e.g. data dirs
# TODO - service discovery for upstreams
# TODO - expose services (+ load-balancing?)
# TODO - low-friction dev deploys
job "quoter" {
  datacenters = ["los"]

  type = "service"

  update {
    stagger      = "10s"
    max_parallel = 3
  }

  group "app" {
    count = 6

    service {
      name = "quoter"
      port = "http"

      check {
        type = "http"
        port = "http"
        path = "/_healthz"
        interval = "10s"
        timeout = "5s"
      }
    }

    network {
      port "http" {}
    }

    task "app" {
      driver = "java"

      artifact {
        # Need to run 'python3 -m http.server 8000' on host machine
        source = "http://10.0.2.2:8000/build/distributions/xomad-${local.version}.zip"
      }

      config {
        class = "choliver.xomad.quoter.Quoter"
        class_path = "${NOMAD_TASK_DIR}/xomad-${local.version}/xomad-${local.version}.jar"
      }

      resources {
        cpu = 300
        memory = 64
      }

      constraint {
        attribute = "${attr.driver.java.version}"
        operator = ">="
        value = "15"
      }
    }
  }
}