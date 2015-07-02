variable "access_key" {}
variable "secret_key" {}
variable "region" {
    default = "eu-west-1"
}
variable "key_name" {
    default = ""
}
variable "zookeeper_type" {
    default = "t2.micro"
}
variable "kafka_type" {
    default = "t2.micro"
}
