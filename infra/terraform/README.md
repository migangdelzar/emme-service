# EMME Modulith — Terraform

Provisions infrastructure for the EMME Modulith platform: VM, networking, DNS, and backup/object-storage.

## Supported Providers

- **Hetzner Cloud** (cheapest, recommended)
- **DigitalOcean**

## Quick Start (Hetzner Cloud + k3s)

1. Get an API token from [Hetzner Cloud Console](https://console.hetzner.cloud)

2. Create `terraform.tfvars`:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars with your token
   ```

3. Uncomment the Hetzner provider block in `main.tf`.

4. Init and apply:
   ```bash
   terraform init
   terraform plan
   terraform apply
   ```

5. After VM is provisioned, install k3s:
   ```bash
   ssh root@<VM_IP> "curl -sfL https://get.k3s.io | sh -"
   ```

6. Pull kubeconfig:
   ```bash
   scp root@<VM_IP>:~/.kube/config ~/.kube/config
   ```

## Variables

| Variable       | Default                  | Description                     |
|---------------|--------------------------|---------------------------------|
| hcloud_token  | (required)               | Hetzner Cloud API token         |
| do_token      | (required)               | DigitalOcean API token          |
| cluster_name  | emme-prod                | k3s cluster name                |
| node_count    | 1                        | Number of VMs                   |
| node_type     | cx22                     | VM size                         |
| region        | nbg1                     | Cloud region                    |
| domain        | emme.app                 | Root DNS domain                 |
| backup_bucket | emme-postgres-backups    | S3 bucket for WAL backups       |
| environment   | prod                     | prod or dev                     |
