provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region = "${var.region}"
}


##########################################################################
#
#                            Network setup
#
##########################################################################

#
# VPC
#
resource "aws_vpc" "samsara_vpc" {
	cidr_block = "10.10.0.0/16"
}


resource "aws_internet_gateway" "samsara_igw" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"
}


#
# Public subnets
#
resource "aws_subnet" "zone1" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

	cidr_block = "10.10.1.0/24"
	availability_zone = "${var.zone1}"
}


#
# Routing table for public subnets
#
resource "aws_route_table" "internet_route" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

	route {
		cidr_block = "0.0.0.0/0"
		gateway_id = "${aws_internet_gateway.samsara_igw.id}"
	}
}


resource "aws_route_table_association" "zone1_internet_route" {
	subnet_id = "${aws_subnet.zone1.id}"
	route_table_id = "${aws_route_table.internet_route.id}"
}


#
# Security group
#
resource "aws_security_group" "sg_ssh" {
	name = "sg_ssh"
	description = "Allow SSH traffic from the internet"

	ingress {
		from_port = 22
		to_port = 22
		protocol = "tcp"
		cidr_blocks = ["0.0.0.0/0"]
	}

	ingress {
		from_port = 0
		to_port = 0
		protocol = "-1"
		self = true
	}

        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
                cidr_blocks = ["0.0.0.0/0"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"
}

resource "aws_security_group" "sg_kibana" {
	name = "sg_kibana"
	description = "Allow HTTP traffic to the kibana dashboard from the internet"

	ingress {
		from_port = 8000
		to_port = 8000
		protocol = "tcp"
		cidr_blocks = ["0.0.0.0/0"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"
}

resource "aws_security_group" "sg_ingestion_api" {
	name = "sg_ingestion_api"
	description = "Allow HTTP traffic to the ingestion api from the internet"

	ingress {
		from_port = 9000
		to_port = 9000
		protocol = "tcp"
		cidr_blocks = ["0.0.0.0/0"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"
}


resource "aws_security_group" "sg_web_monitor" {
	name = "sg_web_monitor"
	description = "Allow HTTP traffic to the monitoring console from the internet"

	ingress {
		from_port = 15000
		to_port = 15000
		protocol = "tcp"
		cidr_blocks = ["0.0.0.0/0"]
	}
        
        vpc_id = "${aws_vpc.samsara_vpc.id}"
}

##########################################################################
#
#                            Instance setup
#
##########################################################################

#
# Samsara's instance
#

resource "aws_instance" "samsara" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.instance_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}", "${aws_security_group.sg_ingestion_api.id}", "${aws_security_group.sg_web_monitor.id}", "${aws_security_group.sg_kibana.id}"]
    subnet_id = "${aws_subnet.zone1.id}"
    associate_public_ip_address = "true"

    connection {
        user = "ubuntu"
        agent = true    
    }

    provisioner "file" {
	source = "scripts/samsara.yml"
	destination = "/tmp/samsara.yml"
    }
 
    provisioner "file" {
	source = "scripts/samsara.conf"
	destination = "/tmp/samsara.conf"
    }

    provisioner "remote-exec" {
	inline = [
            "sudo mkdir -p /opt/samsara",
            "sudo mv /tmp/samsara.yml  /opt/samsara/",
            "sudo mv /tmp/samsara.conf /etc/init/",
            "sudo service samsara start"
        ]
    }
}
 
 
