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
        enable_dns_support = true
        enable_dns_hostnames = true
        
        tags {
          Name    = "Samsara Virtual network"
          project = "${var.project}"
          build   = "${var.build}"
        }
}


resource "aws_internet_gateway" "samsara_igw" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"
        
        tags {
          Name    = "Samsara Gateway"
          project = "${var.project}"
          build   = "${var.build}"
        }
}


#
# Public subnets
#
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

resource "aws_route_table_association" "zone2_internet_route" {
	subnet_id = "${aws_subnet.zone2.id}"
	route_table_id = "${aws_route_table.internet_route.id}"
}

resource "aws_route_table_association" "zone3_internet_route" {
	subnet_id = "${aws_subnet.zone3.id}"
	route_table_id = "${aws_route_table.internet_route.id}"
}


#
# Security group
#

resource "aws_security_group" "sg_general" {
	name = "sg_general"
	description = "General rules"

        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
                cidr_blocks = ["0.0.0.0/0"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara general"
          project = "${var.project}"
          build   = "${var.build}"
        }
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

        tags {
          Name    = "Samsara SSH"
          project = "${var.project}"
          build   = "${var.build}"
        }
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

        tags {
          Name    = "Samsara Kibana"
          project = "${var.project}"
          build   = "${var.build}"
        }
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

        tags {
          Name    = "Samsara Ingestion-API"
          project = "${var.project}"
          build   = "${var.build}"
        }
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

        tags {
          Name    = "Samsara web monitoring"
          project = "${var.project}"
          build   = "${var.build}"
        }
}


resource "aws_security_group" "sg_zookeeper" {
	name = "sg_zookeeper"
	description = "zookeeper boxes connections"

	ingress {
		from_port = 2181
		to_port = 2181
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

	ingress {
		from_port = 2888
		to_port = 2888
		protocol = "tcp"
		self = true
	}

	ingress {
		from_port = 3888
		to_port = 3888
		protocol = "tcp"
		self = true
	}

	ingress {
		from_port = 15000
		to_port = 15000
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"
        
        tags {
          Name    = "Samsara Zookeeper"
          project = "${var.project}"
          build   = "${var.build}"
        }
}


resource "aws_security_group" "sg_kafka" {
	name = "sg_kafka"
	description = "kafka boxes connections"

	ingress {
		from_port = 9092
		to_port = 9092
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

	ingress {
		from_port = 15000
		to_port = 15000
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Kafka"
          project = "${var.project}"
          build   = "${var.build}"
        }
}

#resource "aws_eip" "samsara_ip" {
#        instance = "${aws_instance.samsara.id}"
#        vpc = true
#}


##########################################################################
#
#                            Instance setup
#
##########################################################################

#
# Zookeepers
#

resource "aws_instance" "zookeeper1" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.zookeeper_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_zookeeper.id}"]
    subnet_id = "${aws_subnet.zone1.id}"
    associate_public_ip_address = "true"

    private_ip = "10.10.1.5"
 
    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/zookeeper1.conf"
	destination = "/tmp/zookeeper.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/zookeeper.conf /etc/init/",
            "sudo docker pull samsara/zookeeper",
	    "sudo service zookeeper start"
	]
    }

    tags {
        Name    = "zookeeper1"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


resource "aws_instance" "zookeeper2" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.zookeeper_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_zookeeper.id}"]
    subnet_id = "${aws_subnet.zone2.id}"
    associate_public_ip_address = "true"
 
    private_ip = "10.10.2.5"

    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/zookeeper2.conf"
	destination = "/tmp/zookeeper.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/zookeeper.conf /etc/init/",
            "sudo docker pull samsara/zookeeper",
	    "sudo service zookeeper start"
	]
    }

    tags {
        Name    = "zookeeper2"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


resource "aws_instance" "zookeeper3" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.zookeeper_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_zookeeper.id}"]
    subnet_id = "${aws_subnet.zone3.id}"
    associate_public_ip_address = "true"

    private_ip = "10.10.3.5"

    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/zookeeper3.conf"
	destination = "/tmp/zookeeper.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/zookeeper.conf /etc/init/",
            "sudo docker pull samsara/zookeeper",
	    "sudo service zookeeper start"
	]
    }

    tags {
        Name    = "zookeeper3"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


#
# Kafka
#

resource "aws_instance" "kafka1" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.kafka_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_kafka.id}"]
    subnet_id = "${aws_subnet.zone1.id}"
    associate_public_ip_address = "true"

    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/kafka1.conf"
	destination = "/tmp/kafka.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/kafka.conf /etc/init/",
            "sudo docker pull samsara/kafka",
	    "sudo service kafka start"
	]
    }

    tags {
        Name    = "kafka1"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


resource "aws_instance" "kafka2" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.kafka_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_kafka.id}"]
    subnet_id = "${aws_subnet.zone2.id}"
    associate_public_ip_address = "true"

    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/kafka2.conf"
	destination = "/tmp/kafka.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/kafka.conf /etc/init/",
            "sudo docker pull samsara/kafka",
	    "sudo service kafka start"
	]
    }

    tags {
        Name    = "kafka2"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


resource "aws_instance" "kafka3" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.kafka_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_kafka.id}"]
    subnet_id = "${aws_subnet.zone3.id}"
    associate_public_ip_address = "true"

    connection {
	user = "ubuntu"
	agent = true	
    }
 
    provisioner "file" {
	source = "scripts/kafka3.conf"
	destination = "/tmp/kafka.conf"
    }
 
    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/kafka.conf /etc/init/",
            "sudo docker pull samsara/kafka",
	    "sudo service kafka start"
	]
    }

    tags {
        Name    = "kafka3"
        project = "${var.project}"
        build   = "${var.build}"
    }
}


##########################################################################
#
#                            Output variables
#
##########################################################################

# instance public IP
#output "ip" {
#    value = "${aws_eip.samsara_ip.public_ip}"
#}
