job "xuota" {
  datacenters = ["los"]

  # TODO - sensible names for this hierarchy
  group "app" {
    task "app" {
      driver = "java"

      artifact {
        # Need to run 'python3 -m http.server 8000' on host machine
        source = "http://10.0.2.2:8000/build/distributions/xomad-0.0.2.zip"
      }

      config {
        class = "choliver.xomad.Xuota"
        # TODO - this is horrible
        class_path = "local/xomad-0.0.2/xomad-0.0.2.jar"
      }

//      constraint {
//        attribute = "${driver.java.version}"
//        operator  = ">="
//        value     = "15"
//      }
    }
  }
}
