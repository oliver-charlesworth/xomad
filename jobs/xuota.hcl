job "xuota" {
  datacenters = ["los"]

  type = "service"

  # TODO - sensible names for this hierarchy
  group "app" {
    count = 3

    task "app" {
      driver = "java"

      artifact {
        # Need to run 'python3 -m http.server 8000' on host machine
        source = "http://10.0.2.2:8000/build/distributions/xomad-0.0.2.zip"
      }

      config {
        class = "choliver.xomad.Xuota"
        # TODO - this is horrible
        class_path = "${NOMAD_TASK_DIR}/xomad-0.0.2/xomad-0.0.2.jar"
      }

      resources {
        cpu = 300
        memory = 64
      }

      # TODO - healthcheck

    }
  }
}
