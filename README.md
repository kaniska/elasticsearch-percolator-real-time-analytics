elasticsearch-percolator
========================

Real-time Analytics and Notification using Percolator
#### How does it work ?
##### All Queries are loaded in memory
##### Each document us indexed in memory
##### All queries get executed against it
##### Execution time is linear to # of queries
##### The memory index is quickly cleaned up

#### Business usecases
Lets register following queries 
(a) salesCounterAlert() : boolFilter().must(rangeFilter("salescount").from("100").to("200"));
(b) priceVariationAlert() : boolFilter().must(rangeFilter("price").from("500").to("1000"));

Then we percolate the incoming documents ..
{"time":"2014-11-02T20:51:06.667Z","salescount":150, "price":800}

The percolation process finds that the document matches the query salesCounterAlert and priceVariationAlert and accordingly notifictations are sent through websocket / email / slack 

#### Development Tricks
let percolator query implementors be annotated with an annotation PercolatorQuery
let the PercolationService discover the implementations 
use the Watcher plugin to create watches and implement the notifications

#### Metrics Collector
This is a reporter for the metrics library, similar to the graphite or ganglia reporters, except that it reports to an elasticsearch server.
Then percolation listeners invoke custom Notifiers ( Http / Pager / Web Sockets ) to send the events 
This is useful for aggregating various attribute values of HTTP metrics / JVM metrics / any other info published by JMX

curl http://es.tools.prod.walmart.com/jenkins_metrics/.percolator/server-monitor -X PUT -d '{ "query" : { "bool" : { "must": [ { "term": { "name" : "app.responsetime" } }, { "range": { "t1": { "to" : "5" } } } ] } } }'

#### References :
1) nice presentation : https://speakerdeck.com/javanna/whats-new-in-percolator

2) metrics reporter code repo: https://github.com/elasticsearch/elasticsearch-metrics-reporter-java

3) percolator video : http://berlinbuzzwords.de/session/elasticsearch-percolator

4) percolator pdf : http://berlinbuzzwords.de/sites/berlinbuzzwords.de/files/media/documents/martijn_van_groningen_es_percolator.pdf

5) percolator test code : https://github.com/elasticsearch/elasticsearch/blob/master/src/test/java/org/elasticsearch/percolator/PercolatorTests.java

6) nice percolator usecase : http://blog.qbox.io/birdwatch-twitter-analysis-with-elasticsearch

7) Google Guice Dynamic plgin loader :  https://github.com/google/guice/tree/master/lib/build

8) Query DSL :  http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/query-dsl-queries.html , http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-filtered-query.html

9)   sorting :  http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_sorting.html             

10) aggregation : http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html

11)   discover modules dynamically : https://github.com/ronmamo/reflections  , http://stackoverflow.com/questions/2799316/how-to-collect-and-inject-all-beans-of-a-given-type-in-spring-xml-configuration

12) Other pointers :
http://altfatterz.blogspot.com/2013/09/playing-with-distributed-percolator.html
https://github.com/altfatterz/elasticsearch-fun/blob/master/pom.xml
http://www.programcreek.com/java-api-examples/index.php?api=org.elasticsearch.action.index.IndexResponse

