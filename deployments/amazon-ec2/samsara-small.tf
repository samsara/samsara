provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region = "${var.region}"
}



resource "aws_vpc" "samsara_vpc" {
	cidr_block = "10.10.0.0/16"
}


resource "aws_internet_gateway" "samsara_igw" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"
}



# Public subnets

resource "aws_subnet" "zone1" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

	cidr_block = "10.10.1.0/24"
	availability_zone = "${var.zone1}"
}


resource "aws_subnet" "zone2" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

	cidr_block = "10.10.2.0/24"
	availability_zone = "${var.zone2}"
}


resource "aws_subnet" "zone3" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

	cidr_block = "10.10.3.0/24"
	availability_zone = "${var.zone3}"
}


# Routing table for public subnets

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

resource "aws_route_table_association" "zone2_internet_route" {
	subnet_id = "${aws_subnet.zone2.id}"
	route_table_id = "${aws_route_table.internet_route.id}"
}

resource "aws_route_table_association" "zone3_internet_route" {
	subnet_id = "${aws_subnet.zone3.id}"
	route_table_id = "${aws_route_table.internet_route.id}"
}


resource "aws_security_group" "sg_ssh" {
	name = "sg_ssh"
	description = "Allow SSH traffic from the internet"

	ingress {
		from_port = 22
		to_port = 22
		protocol = "tcp"
		cidr_blocks = ["0.0.0.0/0"]
	}


        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
                cidr_blocks = ["0.0.0.0/0"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"
}


resource "aws_security_group_rule" "allow_this_group" {
        type = "ingress"
        from_port = 0
        to_port = 0
        protocol = "-1"
     
        security_group_id = "${aws_security_group.sg_ssh.id}"
        source_security_group_id = "${aws_security_group.sg_ssh.id}"
}

resource "aws_instance" "zookeeper" {
    ami		    = "ami-cc5d1dbb"
    instance_type   = "${var.zookeeper_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}"]
    subnet_id = "${aws_subnet.zone1.id}"
    associate_public_ip_address = "true"

    connection {
        user = "ubuntu"
        agent = true    
    }

    provisioner "file" {
	source = "scripts/zookeeper.sh"
	destination = "/tmp/zookeeper.sh"
    }
 
    provisioner "remote-exec" {
	inline = [
            "chmod +x /tmp/zookeeper.sh",
            "/tmp/zookeeper.sh"
        ]
    }
}
 
 
resource "aws_instance" "kafka" {
    ami		    = "ami-cc5d1dbb"
    instance_type   = "${var.kafka_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}"]
    subnet_id = "${aws_subnet.zone2.id}"
    associate_public_ip_address = "true"
 
    connection {
        user = "ubuntu"
        agent = true
    }

    provisioner "file" {
	source = "scripts/kafka.sh"
	destination = "/tmp/kafka.sh"
    }
 
    provisioner "remote-exec" {
	inline = [
            "chmod +x /tmp/kafka.sh",
            "/tmp/kafka.sh ${aws_instance.zookeeper.private_ip}"
        ]
    }
}
