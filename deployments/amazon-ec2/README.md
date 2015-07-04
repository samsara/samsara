# Amazon EC2 deployment

To deploy Samsara on Amazon EC2 there are multiple options. Which one
is the best suited for your case depends on your specific workload.

Here we are going to see the following options:

  * Small deployment - single machine, no redundancy
  * Small redundant deployment - 3 hosts, 3 AZs, triple replication
  * Compact deployment - 7 hosts, 3 AZs, triple replicaiton, scalable
    workload
  * Large deployment - 17 hosts, 3 AZs, triple replication,
    indepentely scalable workload.



## Small deployment

The small deployment is fine only for demo, development and testing
purposes.  It doesn't provide redundancy to machine loss and all
processes fight for the same resources (CPU / Memory / IO).  In
particular if the IO of the storage isn't particularly fast then the
query experience isn't great.
**I don't recommend this deployment for a production use**.

### Storage

In this case we will use a single machine with attached storage and
run all containers in the same machine.  The host configuration will
contain LVM2 so if the storage space isn't enough then a new disk
volume can be easily added and the storage space extended.

Two logical volumes will be mounted:

  * `/data` for the database storage
  * `/logs` for the application logs

### Linking

For this simple set up we will make use of `docker-compose`
to coordinate and link all each container.


### Deployment steps

To deploy the app to AWS you basically need to do only two main steps.

  - build the instance images (automated with `packer`)
  - build the stack on aws (automated with `terraform`)

#### To build the images

Set your AWS access keys:

```bash
export AWS_ACCESS_KEY_ID=xxxxx
export AWS_SECRET_ACCESS_KEY=xxxxx
```


By default the tool will build two images.  One with all OS updates
and basic tools installed but without data storage, the second one is
based on the first plus a EBS storage of 200Gb.  If you want to change
the EBS disk size to make it smaller (or bigger) then set the following
packer variable `data_disk_size` to the `build-all-images.sh`

It will use LVM2 to create two logical volumes with the following
repartition:

  - 90% reserved to the `/data` volume
  - 10% reserved to the `/logs` volume


Then run the packer tool providing a base (unique) name for the build,
and any other variable you might want to change.

```bash
cd images
./build-all-images.sh sam-v01 \
    -var 'region=eu-west-1'      \
    -var 'data_disk_size=200'
```

at the end of the build process you will have two images built whose
name depend on the base name provided.

  - `${BASE_NAME}-base`: is the base ami installation with all
    required tools and config
  - `${BASE_NAME}-data`: is the base ami plus a data disk.

these will be need to be provided on the next step.


#### Deploying the stack

Once the base images are built then we can deploy the stack.

```bash
cd small
terraform apply
```

This will build all necessary to have a fully running system.
