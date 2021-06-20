locals {
  version = "0.0.17"
}

job "grid-dummy" {
  datacenters = ["los"]

  type = "service"

  group "app" {
    count = 1

    service {
      name = "grid-dummy"
      port = "http"

      check {
        type = "http"
        port = "http"
        path = "/_healthz"
        interval = "10s"
        timeout = "5s"
      }

      tags = [
        "traefik.enable=true",
      ]
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
        class = "choliver.xomad.grid.dummy.DummyGrid"
        class_path = "${NOMAD_TASK_DIR}/xomad-${local.version}/xomad-${local.version}.jar"
      }

      env {
        BASE_ROUTE = "grid-dummy"
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
