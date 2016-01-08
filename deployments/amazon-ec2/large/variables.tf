variable "access_key" {}
variable "secret_key" {}
variable "region" {
    default = "eu-west-1"
}
variable "zone1" {
    default = "eu-west-1a"
}
variable "zone2" {
    default = "eu-west-1b"
}
variable "zone3" {
    default = "eu-west-1c"
}
variable "key_name" {
    default = ""
}

variable "base_ami" {}
variable "data_ami" {}
variable "ingestion_ami" {}
variable "kibana_ami" {}
variable "els_ami" {}
variable "qanal_ami" {}
variable "core_ami" {}
variable "zookeeper_ami" {}
variable "kafka_ami" {}
variable "monitoring_ami" {}
variable "spark_master_ami" {}
variable "spark_worker_ami" {}

variable "project" {
    default = "samsara"
}
variable "env" {
    default = "dev"
}
variable "build" {}
variable "zookeeper_type" {
    default = "t2.small"
}
variable "kafka_type" {
    default = "m4.large"
}
variable "ingestion_type" {
    default = "t2.small"
}
variable "core_type" {
    default = "m4.xlarge"
}
variable "qanal_type" {
    default = "t2.medium"
}
variable "els_type" {
    default = "m4.xlarge"
}
variable "kibana_type" {
    default = "t2.small"
}
variable "monitoring_type" {
    default = "m4.large"
}
variable "spark_master_type" {
    default = "m3.large"
}
variable "spark_worker_type" {
    default = "m3.xlarge"
}
variable "public_ingestion_port" {
    default = 9000
}
variable "public_kibana_port" {
    default = 8000
}
variable "cidr_ssh_access" {
    default = "0.0.0.0/0"
}
variable "cidr_ingestion_access" {
    default = "0.0.0.0/0"
}
variable "cidr_kibana_access" {
    default = "0.0.0.0/0"
}
variable "cidr_monitoring_access" {
    default = "0.0.0.0/0"
}
