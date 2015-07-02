provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region = "${var.region}"
}

resource "aws_vpc" "default" {
	cidr_block = "10.10.0.0/16"
}


resource "aws_instance" "zookeeper" {
    ami           = "ami-d6d293a1"
    instance_type = "${var.zookeeper_type}"
    key_name      = "${var.key_name}"

    provisioner "file" {
        source = "scripts/zookeeper.sh"
        destination = "/opt/zookeeper.sh"
    }

    provisioner "local-exec" {
        command = "/opt/zookeeper.sh"
    }
}


resource "aws_instance" "kafka" {
    ami           = "ami-d6d293a1"
    instance_type = "${var.kafka_type}"
    key_name      = "${var.key_name}"

    provisioner "file" {
        source = "scripts/kafka.sh"
        destination = "/opt/kafka.sh"
    }

    provisioner "local-exec" {
        command = "/opt/kafka.sh ${aws_instance.zookeeper.private_ip}"
    }
}
