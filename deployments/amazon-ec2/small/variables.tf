variable "access_key" {}
variable "secret_key" {}
variable "region" {
    default = "eu-west-1"
}
variable "zone1" {
    default = "eu-west-1a"
}
variable "key_name" {
    default = ""
}
variable "instance_type" {
    default = "m4.large"
}
variable "data_ami" {}
