# ------------------------------------------------------------------------------
# General variables.
# ------------------------------------------------------------------------------
prefix         = "cstar"
env_short      = "u"
env            = "uat"
location       = "italynorth"
location_short = "itn" 
domain         = "srtp"

tags = {
  CreatedBy   = "Terraform"
  Environment = "uat"
  Owner       = "cstar"
  Source      = "https://github.com/pagopa/rtp-sender/tree/main/src/main/terraform"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  Domain      = "rtp"
}

# ------------------------------------------------------------------------------
# External resources.
# ------------------------------------------------------------------------------
cae_name                       = "cstar-u-itn-srtp-cae"
cae_resource_group_name        = "cstar-u-itn-srtp-compute-rg"
id_name                        = "cstar-u-itn-srtp-sender-id"
id_resource_group_name         = "cstar-u-itn-srtp-identity-rg"
rtp_sender_file_share_storage_name = "cstar-u-itn-srtp-sender-fss"

# ------------------------------------------------------------------------------
# Names of key vault secrets.
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
# Configuration of the microservice.
# ------------------------------------------------------------------------------
rtp_sender_app_log_level                     = "DEBUG"
rtp_sender_image                             = "ghcr.io/pagopa/rtp-sender:latest"
rtp_sender_cpu                               = 0.25
rtp_sender_memory                            = "0.5Gi"
rtp_sender_max_replicas                      = 5
rtp_sender_min_replicas                      = 1

rtp_environment_secrets = {
  COSMOS_ACCOUNT_RTP_CONNECTION_STRING  : "cosmosdb-account-rtp-primary-connection-string"
  APPLICATIONINSIGHTS_CONNECTION_STRING : "appinsights-connection-string"
  CLIENT_CERTIFICATE                    : "client-certificate"
  CLIENT_SECRET_CBI                     : "client-secret-cbi"
  JKS_TRUST_STORE_PATH                  : "jks-trust-store-path"
  JKS_TRUST_STORE_PASSWORD              : "jks-trust-store-password"
  GDP_EVENTHUB_CONNECTION_STRING        : "gdp-eventhub-connection-string"
  MIL_AUTH_CLIENT_SECRET                : "mil-auth-client-secret"
  MIL_AUTH_CLIENT_ID                    : "mil-auth-client-id"
}


rtp_environment_configs = {
  DB_NAME                                 : "rtp"
  BASE_URL                                : "https://api-rtp.uat.cstar.pagopa.it/rtp/activation/"
  SP_BASE_URL                             : "https://api-rtp.uat.cstar.pagopa.it/rtp/rtps/"
  OTEL_TRACES_SAMPLER                     : "always_on"
  EPC_MOCK_URL                            : "https://api-rtp.uat.cstar.pagopa.it/rtp/mock"
  EPC_SEND_RETRY_MAX_ATTEMPTS             : 1
  EPC_SEND_RETRY_BACKOFF_MIN_DURATION_MS  : 1000
  EPC_SEND_RETRY_BACKOFF_JITTER           : 0.75
  EPC_SEND_TIMEOUT_MS                     : 6000
  AZURE_STORAGE_ACCOUNT_NAME              : "cstaruitnsrtpsa"
  AZURE_STORAGE_CONTAINER_NAME            : "rtp-debtor-service-provider"
  AZURE_BLOB_NAME                         : "serviceregistry.json"
  CALLBACK_BASE_URL                       : "https://api-rtp-cb.uat.cstar.pagopa.it/rtp/cb"
  GDP_EVENTHUB_NAME                       : "pagopa-u-itn-gps-rtp-integration-evh"
  GDP_EVENTHUB_TOPIC                      : "rtp-events"
  GDP_EVENTHUB_CONSUMER_GROUP             : "rtp-events-processor"
  REGISTRY_DATA_CACHE_TTL                 : "PT5M"
  MIL_AUTH_TOKEN_URL                      : "https://api-mcshared.uat.cstar.pagopa.it/auth/token"
}

