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
variable "project" {
    default = "samsara"
}
variable "build" {}
variable "zookeeper_type" {
    default = "t2.small"
}
variable "kafka_type" {
    default = "t2.small"
}
variable "ingestion_type" {
    default = "t2.small"
}
variable "core_type" {
    default = "t2.small"
}
variable "qanal_type" {
    default = "t2.small"
}
variable "els_type" {
    default = "t2.small"
}
variable "kibana_type" {
    default = "t2.small"
}
variable "monitoring_type" {
    default = "t2.small"
}
variable "public_ingestion_port" {
    default = 9000
}
variable "public_kibana_port" {
    default = 8000
}
variable "cidr_allowed_access" {
    default = "0.0.0.0/0"
}
