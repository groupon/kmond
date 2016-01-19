KMonD
=====

<a href="https://raw.githubusercontent.com/groupon/kmond/master/LICENSE">
    <img src="https://img.shields.io/hexpm/l/plug.svg"
         alt="License: Apache 2">
</a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.groupon.aint%22%20a%3A%22kmond%22">
    <img src="https://img.shields.io/maven-central/v/com.groupon.aint/kmond.svg"
         alt="Maven Artifact">
</a>

A service to forward metrics to both Nagios and Ganglia.

Behavior
--------

KMonD is a 'Y' pipe: it receives input via http (each of which is a 'metric') and pipes the input to both Nagios and
Ganglia after doing format conversion. KMonD checks the *cluster name* and *host* of each metric received, and uses these
to select a specific Nagios host and Ganglia host-and-port-pair to which to forward the metric. The selection logic is
as follows:

### Nagios

The gzip file `nagios_info.gz` is retrieved from the config server (which is a simple file server) and contains the file
`nagios_info.json` (see the example in `src/test/resources/nagios_info.json`).

KMonD uses a rudimentary bucketing method to map the *host name* from each metric to an bucket (integer) in the range
(inclusive) 0-99. The `nagios_info.json` file maps from the integer to the associated nagios host, to which KMonD
forwards the metric after format conversion.  If the bucket number is not found in the mapping, the metric is
discarded.

### Ganglia

The files `ganglia_cluster.yml` and `ganglia_port_clusters.yml` are retrieved from the config server. KMonD extracts
the *cluster name* from each metric and maps it to both a port (integer) via `ganglia_port_clusters.yml`, and a ganglia
host via `ganglia_cluster.yml`. KMonD then connects to this host at the given port to forward the metric.

Usage
-----

KMonD requires an instance of Nagios and Ganglia to which to forward metrics. It supports partitioning of both the Nagios
and Ganglia destination clusters. Nagios partitioned is achieved by mapping the host the metrics originated from to a
particular Nagios host. Ganglia partitioning is achieved by mapping the originating metric's cluster to a port and Ganglia
host.

### Configuration

The configuration format can be found in `src/main/conf/example_config`. Copy this
template to a directory named with the environment, e.g. `src/main/conf/production`,
and update the values based on your environment.

For examples of the mapping configuration files that are retrieved from the config server,
look in `src/test/resources/`.

### Execution

Deploy the fat or uber jar to your target host(s).  Simply run the jar to start the service:

    > java -jar kmond-x.y.z-fat.jar

where `x.y.z` is the version and `kmond-x.y.z-fat.jar` is the artifact build by Maven (see 'Building').

Building
--------

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)


    kmond> ./mvnw install

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2015
