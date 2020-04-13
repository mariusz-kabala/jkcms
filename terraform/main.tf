resource "docker_container" "jkcms" {
  name  = "jkcms-${var.tag}"
  image = "docker-registry.kabala.tech/jkcms:${var.tag}"
  restart = "always"
  networks_advanced {
      name = "${var.app_network}"
  }

  labels {
    label = "traefik.enable"
    value = "true"
  }

  labels {
    label = "traefik.backend"
    value = "jklawyers-${var.tag}"
  }

  labels {
    label = "traefik.frontend.rule"
    value = "Host:${var.app_domain}"
  }

  labels {
    label = "traefik.protocol"
    value = "http"
  }

  labels {
    label = "traefik.port"
    value = "1337"
  }

  env = [
    "NODE_ENV=production"
  ]
}

resource "docker_container" "jkcms-db" {
  name  = "jkcms-db"
  image = "mongo:4"
  restart = "always"

  networks_advanced {
      name = "${var.app_network}"
  }

  volumes {
    host_path      = "${var.mount_point}/jkcms-db"
    container_path = "/data/db"
  }
}
