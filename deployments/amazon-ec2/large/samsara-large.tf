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
          env     = "${var.env}"
        }
}


resource "aws_internet_gateway" "samsara_igw" {
	vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Gateway"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
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
          env     = "${var.env}"
        }
}

resource "aws_security_group" "sg_ssh" {
	name = "sg_ssh"
	description = "Allow SSH traffic from the internet"

	ingress {
		from_port = 22
		to_port = 22
		protocol = "tcp"
		cidr_blocks = ["${var.cidr_allowed_access}"]
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
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_ingestion_api" {
	name = "sg_ingestion_api"
	description = "Allow HTTP traffic to the ingestion api from the internet"

        ingress {
		from_port = 9000
		to_port = 9000
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Ingestion-API"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_ingestion_api_lb" {
	name = "sg_ingestion_api_lb"
	description = "Allow HTTP traffic to the ingestion api LB from the internet"

	ingress {
		from_port = "${var.public_ingestion_port}"
		to_port = "${var.public_ingestion_port}"
		protocol = "tcp"
		cidr_blocks = ["${var.cidr_allowed_access}"]
	}

        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Ingestion-API LB"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_web_monitor" {
	name = "sg_web_monitor"
	description = "Allow HTTP traffic to the monitoring console from the internet"

	ingress {
		from_port = 15000
		to_port = 15000
		protocol = "tcp"
		cidr_blocks = ["${var.cidr_allowed_access}"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara web monitoring"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
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
          env     = "${var.env}"
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
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_core" {
	name = "sg_core"
	description = "Samsara-CORE boxes connections"

	ingress {
		from_port = 4555
		to_port = 4555
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
          Name    = "Samsara-CORE"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_qanal" {
	name = "sg_qanal"
	description = "Qanal boxes connections"

	ingress {
		from_port = 15000
		to_port = 15000
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Qanal"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_els" {
	name = "sg_els"
	description = "ElasticSearch boxes connections"

	ingress {
		from_port = 9200
		to_port = 9200
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

	ingress {
		from_port = 9300
		to_port = 9300
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
          Name    = "Samsara ElasticSearch"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_els_lb" {
	name = "sg_els_lb"
	description = "ElasticSearch LB connections"

	ingress {
		from_port = 9200
		to_port = 9200
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara ElasticSearch LB"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_kibana" {
	name = "sg_kibana"
	description = "Samsara Kibana boxes connections"

	ingress {
		from_port = 8000
		to_port = 8000
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
          Name    = "Samsara Kibana"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


resource "aws_security_group" "sg_kibana_lb" {
	name = "sg_kibana_lb"
	description = "Samsara Kibana LB connections"

	ingress {
		from_port = "${var.public_kibana_port}"
		to_port = "${var.public_kibana_port}"
		protocol = "tcp"
		cidr_blocks = ["${var.cidr_allowed_access}"]
	}

        egress {
                from_port = 0
                to_port = 0
                protocol = "-1"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
        }

        vpc_id = "${aws_vpc.samsara_vpc.id}"

        tags {
          Name    = "Samsara Kibana LB"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}



resource "aws_security_group" "sg_monitoring" {
	name = "sg_monitoring"
	description = "Allow traffic towards the monitoring machines"

	ingress {
		from_port = 5555
		to_port = 5556
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

	ingress {
		from_port = 8083
		to_port = 8083
		protocol = "tcp"
		cidr_blocks = ["${aws_vpc.samsara_vpc.cidr_block}"]
	}

        ingress {
		from_port = 8086
		to_port = 8086
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
          Name    = "Samsara Monitoring"
          project = "${var.project}"
          build   = "${var.build}"
          env     = "${var.env}"
        }
}


#
# Elastic LoadBalancers
#

resource "aws_elb" "ingestion_api" {
    name = "ingestion-api-elb-${var.env}"

    subnets = ["${aws_subnet.zone1.id}",
               "${aws_subnet.zone2.id}",
               "${aws_subnet.zone3.id}"]

    listener {
      instance_port = 9000
      instance_protocol = "http"
      lb_port = "${var.public_ingestion_port}"
      lb_protocol = "http"
    }

    health_check {
      healthy_threshold = 3
      unhealthy_threshold = 2
      timeout = 10
      target = "HTTP:9000/v1/api-status"
      interval = 30
    }

    security_groups = ["${aws_security_group.sg_ingestion_api_lb.id}"]

    tags {
        Name    = "Samsara Ingestion-API"
        project = "${var.project}"
        build   = "${var.build}"
        env     = "${var.env}"
    }
}


resource "aws_elb" "kibana" {
    name = "kibana-elb-${var.env}"

    subnets = ["${aws_subnet.zone1.id}",
               "${aws_subnet.zone2.id}",
               "${aws_subnet.zone3.id}"]

    listener {
      instance_port = 8000
      instance_protocol = "http"
      lb_port = "${var.public_kibana_port}"
      lb_protocol = "http"
    }

    health_check {
      healthy_threshold = 3
      unhealthy_threshold = 2
      timeout = 10
      target = "HTTP:8000/"
      interval = 30
    }

    security_groups = ["${aws_security_group.sg_kibana_lb.id}"]

    tags {
        Name    = "Samsara Kibana ELB"
        project = "${var.project}"
        build   = "${var.build}"
        env     = "${var.env}"
    }
}


resource "aws_elb" "els" {
    name = "els-elb-${var.env}"

    subnets = ["${aws_subnet.zone1.id}",
               "${aws_subnet.zone2.id}",
               "${aws_subnet.zone3.id}"]

    internal = true

    listener {
      instance_port = 9200
      instance_protocol = "http"
      lb_port = 9200
      lb_protocol = "http"
    }

    health_check {
      healthy_threshold = 3
      unhealthy_threshold = 2
      timeout = 10
      target = "HTTP:9200/"
      interval = 30
    }

    security_groups = ["${aws_security_group.sg_els_lb.id}"]

    tags {
        Name    = "Samsara ELS ILB"
        project = "${var.project}"
        build   = "${var.build}"
        env     = "${var.env}"
    }
}

resource "aws_eip" "samsara_monitor_ip" {
        instance = "${aws_instance.monitor1.id}"
        vpc = true
}


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
        env     = "${var.env}"
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
        env     = "${var.env}"
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
        env     = "${var.env}"
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
        env     = "${var.env}"
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
        env     = "${var.env}"
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
        env     = "${var.env}"
    }
}


#
# Ingestion-API
#
resource "aws_autoscaling_group" "ingestion-api-asg" {
    name = "ingestion-api-asg-${var.env}"

    vpc_zone_identifier = ["${aws_subnet.zone1.id}", "${aws_subnet.zone2.id}", "${aws_subnet.zone3.id}"]

    max_size = 3
    min_size = 3
    desired_capacity = 3

    health_check_grace_period = 180
    health_check_type = "ELB"

    force_delete = true
    launch_configuration = "${aws_launch_configuration.ingestion-api-lc.name}"
    load_balancers = ["ingestion-api-elb-${var.env}"]

    tag {
        key = "Name"
        value = "ingestion"
        propagate_at_launch = true
    }
    tag {
        key = "project"
        value = "${var.project}"
        propagate_at_launch = true
    }
    tag {
        key = "build"
        value = "${var.build}"
        propagate_at_launch = true
    }
    tag {
        key = "env"
        value = "${var.env}"
        propagate_at_launch = true
    }
}


resource "aws_launch_configuration" "ingestion-api-lc" {
    name = "ingestion-api-lc-${var.env}"

    image_id = "${var.ingestion_ami}"
    instance_type = "${var.ingestion_type}"
    key_name = "${var.key_name}"

    security_groups = ["${aws_security_group.sg_ssh.id}",
                       "${aws_security_group.sg_general.id}",
                       "${aws_security_group.sg_ingestion_api.id}"]

    associate_public_ip_address = "true"

    user_data = "-e KAFKA_1_PORT_9092_TCP_ADDR=${aws_instance.kafka1.private_ip} -e KAFKA_2_PORT_9092_TCP_ADDR=${aws_instance.kafka2.private_ip} -e KAFKA_3_PORT_9092_TCP_ADDR=${aws_instance.kafka3.private_ip} -e RIEMANN_PORT_5555_TCP_ADDR=${aws_instance.monitor1.private_ip}"

}


#
# Samsara-CORE
#

resource "aws_autoscaling_group" "core-asg" {
    name = "core-asg-${var.env}"

    vpc_zone_identifier = ["${aws_subnet.zone1.id}", "${aws_subnet.zone2.id}", "${aws_subnet.zone3.id}"]

    max_size = 1
    min_size = 1
    desired_capacity = 1

    health_check_grace_period = 180
    health_check_type = "EC2"

    force_delete = true
    launch_configuration = "${aws_launch_configuration.core-lc.name}"

    tag {
        key = "Name"
        value = "core"
        propagate_at_launch = true
    }
    tag {
        key = "project"
        value = "${var.project}"
        propagate_at_launch = true
    }
    tag {
        key = "build"
        value = "${var.build}"
        propagate_at_launch = true
    }
    tag {
        key = "env"
        value = "${var.env}"
        propagate_at_launch = true
    }
}


resource "aws_launch_configuration" "core-lc" {
    name = "core-lc-${var.env}"

    image_id = "${var.core_ami}"
    instance_type = "${var.core_type}"
    key_name = "${var.key_name}"

    security_groups = ["${aws_security_group.sg_ssh.id}",
                       "${aws_security_group.sg_general.id}",
		       "${aws_security_group.sg_core.id}"]

    associate_public_ip_address = "true"

    # TODO: accept more than 1 kafka and more than 1 zk
    #user_data = "-e KAFKA1_PORT_9092_TCP_ADDR=${aws_instance.kafka1.private_ip} -e KAFKA2_PORT_9092_TCP_ADDR=${aws_instance.kafka2.private_ip} -e KAFKA3_PORT_9092_TCP_ADDR=${aws_instance.kafka3.private_ip}"
    user_data = "-e KAFKA_PORT_9092_TCP_ADDR=${aws_instance.kafka1.private_ip} -e KAFKA_PORT_9092_TCP_PORT=9092 -e ZOOKEEPER_PORT_2181_TCP_ADDR=${aws_instance.zookeeper1.private_ip} -e ZOOKEEPER_PORT_2181_TCP_PORT=2181 -e RIEMANN_PORT_5555_TCP_ADDR=${aws_instance.monitor1.private_ip}"

}



#
# Samsara-Qanal
#


resource "aws_autoscaling_group" "qanal-asg" {
    name = "qanal-asg-${var.env}"

    vpc_zone_identifier = ["${aws_subnet.zone1.id}", "${aws_subnet.zone2.id}", "${aws_subnet.zone3.id}"]

    max_size = 1
    min_size = 1
    desired_capacity = 1

    health_check_grace_period = 180
    health_check_type = "EC2"

    force_delete = true
    launch_configuration = "${aws_launch_configuration.qanal-lc.name}"

    tag {
        key = "Name"
        value = "qanal"
        propagate_at_launch = true
    }
    tag {
        key = "project"
        value = "${var.project}"
        propagate_at_launch = true
    }
    tag {
        key = "build"
        value = "${var.build}"
        propagate_at_launch = true
    }
    tag {
        key = "env"
        value = "${var.env}"
        propagate_at_launch = true
    }
}


resource "aws_launch_configuration" "qanal-lc" {
    name = "qanal-lc-${var.env}"

    image_id = "${var.qanal_ami}"
    instance_type = "${var.qanal_type}"
    key_name = "${var.key_name}"

    security_groups = ["${aws_security_group.sg_ssh.id}",
                       "${aws_security_group.sg_general.id}",
		       "${aws_security_group.sg_qanal.id}"]

    associate_public_ip_address = "true"

    # TODO: accept more than 1 kafka and more than 1 zk
    #user_data = "-e KAFKA1_PORT_9092_TCP_ADDR=${aws_instance.kafka1.private_ip} -e KAFKA2_PORT_9092_TCP_ADDR=${aws_instance.kafka2.private_ip} -e KAFKA3_PORT_9092_TCP_ADDR=${aws_instance.kafka3.private_ip}"
    user_data = "-e KAFKA_PORT_9092_TCP_ADDR=${aws_instance.kafka1.private_ip} -e KAFKA_PORT_9092_TCP_PORT=9092 -e ZOOKEEPER_PORT_2181_TCP_ADDR=${aws_instance.zookeeper1.private_ip} -e ZOOKEEPER_PORT_2181_TCP_PORT=2181 -e ZOOKEEPER_PORT_2181_TCP=tcp://${aws_instance.zookeeper1.private_ip}:2181 -e ELS_PORT_9200_TCP_ADDR=${aws_elb.els.dns_name} -e ELS_PORT_9200_TCP_PORT=9200 -e RIEMANN_PORT_5555_TCP_ADDR=${aws_instance.monitor1.private_ip}"

}



#
# ElasticSearch
#

resource "aws_autoscaling_group" "els-asg" {
    name = "els-asg-${var.env}"

    vpc_zone_identifier = ["${aws_subnet.zone1.id}", "${aws_subnet.zone2.id}", "${aws_subnet.zone3.id}"]

    max_size = 6
    min_size = 3
    desired_capacity = 3

    health_check_grace_period = 180
    health_check_type = "ELB"

    force_delete = true
    launch_configuration = "${aws_launch_configuration.els-lc.name}"
    load_balancers = ["els-elb-${var.env}"]

    tag {
        key = "Name"
        value = "els"
        propagate_at_launch = true
    }
    tag {
        key = "project"
        value = "${var.project}"
        propagate_at_launch = true
    }
    tag {
        key = "build"
        value = "${var.build}"
        propagate_at_launch = true
    }
    tag {
        key = "env"
        value = "${var.env}"
        propagate_at_launch = true
    }
}


resource "aws_launch_configuration" "els-lc" {
    name = "els-lc-${var.env}"

    image_id = "${var.els_ami}"
    instance_type = "${var.els_type}"
    key_name = "${var.key_name}"

    security_groups = ["${aws_security_group.sg_ssh.id}",
                       "${aws_security_group.sg_general.id}",
		       "${aws_security_group.sg_els.id}"]

    associate_public_ip_address = "true"

}


#
# Kibana
#

resource "aws_autoscaling_group" "kibana-asg" {
    name = "kibana-asg-${var.env}"

    vpc_zone_identifier = ["${aws_subnet.zone1.id}", "${aws_subnet.zone2.id}", "${aws_subnet.zone3.id}"]

    max_size = 3
    min_size = 2
    desired_capacity = 2

    health_check_grace_period = 180
    health_check_type = "ELB"

    force_delete = true
    launch_configuration = "${aws_launch_configuration.kibana-lc.name}"
    load_balancers = ["kibana-elb-${var.env}"]

    tag {
        key = "Name"
        value = "kibana"
        propagate_at_launch = true
    }
    tag {
        key = "project"
        value = "${var.project}"
        propagate_at_launch = true
    }
    tag {
        key = "build"
        value = "${var.build}"
        propagate_at_launch = true
    }
    tag {
        key = "env"
        value = "${var.env}"
        propagate_at_launch = true
    }
}


resource "aws_launch_configuration" "kibana-lc" {
    name = "kibana-lc-${var.env}"

    image_id = "${var.kibana_ami}"
    instance_type = "${var.kibana_type}"
    key_name = "${var.key_name}"

    security_groups = ["${aws_security_group.sg_ssh.id}",
                       "${aws_security_group.sg_general.id}",
		       "${aws_security_group.sg_kibana.id}"]

    associate_public_ip_address = "true"

    user_data = "-e ELASTICSEARCH_PORT_9200_TCP_ADDR=${aws_elb.els.dns_name} -e ELASTICSEARCH_PORT_9200_TCP_PORT=9200"

}



#
# Monitoring
#

resource "aws_instance" "monitor1" {
    ami		    = "${var.data_ami}"
    instance_type   = "${var.monitoring_type}"
    key_name	    = "${var.key_name}"
    vpc_security_group_ids = ["${aws_security_group.sg_ssh.id}",
                              "${aws_security_group.sg_general.id}",
                              "${aws_security_group.sg_monitoring.id}"]
    subnet_id = "${aws_subnet.zone2.id}"
    associate_public_ip_address = "true"

    connection {
	user = "ubuntu"
	agent = true
    }

    provisioner "file" {
	source = "scripts/monitoring.conf"
	destination = "/tmp/monitoring.conf"
    }

    provisioner "remote-exec" {
	inline = [
	    "sudo mv /tmp/monitoring.conf /etc/init/",
            "sudo docker pull samsara/elasticsearch",
	    "sudo service monitoring start"
	]
    }

    tags {
        Name    = "monitor1"
        project = "${var.project}"
        build   = "${var.build}"
        env     = "${var.env}"
    }
}


##########################################################################
#
#                            Output variables
#
##########################################################################


# Monitoring public IP
output "monitoring_ip" {
    value = "${aws_eip.samsara_monitor_ip.public_ip}"
}

output "ingestion_api_lb" {
    value = "${aws_elb.ingestion_api.dns_name}"
}
output "ingestion_api_lb_port" {
    value = "${var.public_ingestion_port}"
}

output "dashboard_lb" {
    value = "${aws_elb.kibana.dns_name}"
}
output "dashboard_lb_port" {
    value = "${var.public_kibana_port}"
}

output "cidr_allowed_access" {
    value = "${var.cidr_allowed_access}"
}
