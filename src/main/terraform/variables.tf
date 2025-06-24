# ------------------------------------------------------------------------------
# Generic variables definition.
# ------------------------------------------------------------------------------
variable "prefix" {
  type = string
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env" {
  type = string
  validation {
    condition = (
      length(var.env) <= 4
    )
    error_message = "Max length is 4 chars."
  }
}

variable "env_short" {
  type = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "location_short" {
  type        = string
  description = "Location short like eg: neu, weu."
}

variable "tags" {
  type = map(any)
}

variable "domain" {
  type    = string
  default = ""
}

# ------------------------------------------------------------------------------
# Container Apps Environment.
# ------------------------------------------------------------------------------
variable "cae_name" {
  type = string
}

variable "cae_resource_group_name" {
  type = string
}

variable "rtp_sender_file_share_storage_name" {
  description = "Name of the shared Azure File Storage for rtp-sender"
  type        = string
}

# ------------------------------------------------------------------------------
# Identity for this Container App.
# ------------------------------------------------------------------------------
variable "id_name" {
  type = string
}

variable "id_resource_group_name" {
  type = string
}

# ------------------------------------------------------------------------------
# Specific to rtp-sender microservice.
# ------------------------------------------------------------------------------
variable "rtp_sender_app_log_level" {
  type    = string
  default = "DEBUG"
}

variable "rtp_sender_max_replicas" {
  type    = number
  default = 10
}

variable "rtp_sender_min_replicas" {
  type    = number
  default = 1
}

variable "rtp_sender_base_url" {
  type = string
}

variable "rtp_sender_cpu" {
  type    = number
  default = 1
}

variable "rtp_sender_memory" {
  type    = string
  default = "1Gi"
}

variable "rtp_sender_image" {
  type = string
}

variable "rtp_environment_configs" {
  type    = map(any)
  default = {}
}

variable "rtp_environment_secrets" {
  type    = map(any)
  default = {}
}
