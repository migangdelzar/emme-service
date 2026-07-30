variable "hcloud_token" {
  description = "Hetzner Cloud API token"
  type        = string
  sensitive   = true
  default     = ""
}

variable "cluster_name" {
  description = "Name of the k3s cluster"
  type        = string
  default     = "emme-prod"
}

variable "node_type" {
  description = "VM size (CX22 for Hetzner, s-2vcpu-4gb for DO)"
  type        = string
  default     = "cx22"
}

variable "domain" {
  description = "Root domain for EMME (e.g., emme.app)"
  type        = string
  default     = "emme.app"
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key"
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

variable "location" {
  description = "Hetzner datacenter"
  type        = string
  default     = "nbg1"
}

variable "backup_bucket" {
  description = "S3-compatible bucket for PostgreSQL WAL backups"
  type        = string
  default     = "emme-postgres-backups"
}

variable "environment" {
  description = "prod or dev"
  type        = string
  default     = "prod"
}
