terraform {
  required_version = ">= 1.5"
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.48"
    }
  }
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_ssh_key" "default" {
  name       = "${var.cluster_name}-ssh"
  public_key = file(pathexpand(var.ssh_public_key_path))
}

resource "hcloud_firewall" "k3s" {
  name = "${var.cluster_name}-firewall"
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "22"
    source_ips = ["0.0.0.0/0"]
  }
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "80"
    source_ips = ["0.0.0.0/0"]
  }
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "443"
    source_ips = ["0.0.0.0/0"]
  }
}

resource "hcloud_server" "k3s" {
  name         = var.cluster_name
  server_type  = var.node_type
  image        = "ubuntu-24.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.default.id]
  firewall_ids = [hcloud_firewall.k3s.id]

  user_data = templatefile("${path.module}/cloud-init.yaml", {
    cluster_name = var.cluster_name
    domain       = var.domain
  })

  lifecycle {
    ignore_changes = [user_data]
  }
}

resource "hcloud_floating_ip" "k3s" {
  type      = "ipv4"
  server_id = hcloud_server.k3s.id
}

output "server_ip" {
  value = hcloud_server.k3s.ipv4_address
}

output "floating_ip" {
  value = hcloud_floating_ip.k3s.ip_address
}

output "kubeconfig_command" {
  value = "ssh -fN -L 6443:127.0.0.1:6443 root@${hcloud_server.k3s.ipv4_address} && scp root@${hcloud_server.k3s.ipv4_address}:/etc/rancher/k3s/k3s.yaml ~/.kube/config"
}
