<div class="page-break"></div>

## Chapter 3.4: Bad Concurrency (Michael Barker)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Aeron & Low-Latency Multicast
Aeron is an ultra-low-latency, reliable messaging system that operates over UDP, Multicast UDP, and IPC. Co-developed by Martin Thompson and Todd Montgomery, it is the transport layer that powers many high-performance trading architectures.

#### 2. Flow Control in Multicast
In unidirectional UDP, senders can easily outpace receivers, causing packet loss. TCP solves this with sliding window flow control, but how do you do flow control for *multicast* (one sender, many receivers)?
Aeron provides several dynamic strategies:
- **Max Flow Control**: The sender limits its rate based on the *fastest* receiver. Slow receivers will drop packets.
- **Min Flow Control**: The sender limits its rate based on the *slowest* receiver. Slow nodes hold up the publisher.
- **Tagged Flow Control**: Only receivers with a specific tag are included in the min flow control calculation. Non-critical subscribers (like gateways) can drop packets without slowing the critical subscribers (like archiving databases).

#### 3. Aeron Flow Control Configuration Example
```java
// Setting up Aeron Channels with different flow control strategies

// 1. Max Flow Control (Fastest receiver dictates speed)
final String maxChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=max";

// 2. Min Flow Control (Slowest receiver dictates speed)
final String minChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=min";

// 3. Tagged Flow Control (Only nodes tagged with '1001' dictate speed)
final String taggedChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=tagged,g:1001";

// Create Publisher
final Publication publication = aeron.addPublication(taggedChannel, STREAM_ID);

// On the subscriber side (Critical Node):
final String criticalSub = "aeron:udp?endpoint=224.0.1.1:40456|gtag=1001";
final Subscription subscription = aeron.addSubscription(criticalSub, STREAM_ID);
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

<div class="scholarly-text">

#### Paper 3: Bad Concurrency (March/April 2020)
*By Michael Barker (bad-concurrency.blogspot.com)*

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Bad Concurrency Blog Posts (I Heard a Rumour... & Flow Control in Aeron)<br>
  <strong>Author(s):</strong> Michael Barker<br>
  <strong>Published:</strong> April/March 2020<br>
  <strong>Source type:</strong> Engineering Blog<br>
  <strong>Original URL:</strong> https://bad-concurrency.blogspot.com/<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

5/8/2026, 2:05 pm
Bad Concurrency
Page 1 of 5
https://bad-concurrency.blogspot.com/
Misadventures in Concurrent and Parallel programming, plus random comments on software performance and various OSS
contributions.
Bad Concurrency
Saturday, 11 April 2020
I Heard a Rumour...
Where Aeron catches up on the goss.
A few months ago a pull request appeared on Aeron's Github site that added the ability to request Aeron to resolve
or re-resolve host names to IP addresses.  In cloud environments, especially when using Kubernetes, when nodes
fail and restart it is not uncommon for a node with the same host name to restart with a different IP address. 
Unfortunately for Aeron this could make life difficult as it would resolve IP addresses up front and stick with it for the
life time of the media driver.  This is particularly bad when we consider nodes that are part of Aeron Cluster, where
we expect nodes to come and go from the cluster over time.
It became very clear that we needed a plan that would allow Aeron to use logical names instead of IP addresses as
endpoint identifiers and re-resolve those addresses appropriately.  We didn't end up using the supplied pull request
and came with an alternative solution that was a better fit with some of Aeron's longer term goals (I say we, it was
mostly Todd Montgomery - I just did the C port).
As DNS can often be a source odd network latency issues, we didn't want a name resolution solution that was
entirely reliant on default system name resolution.  So we have also included a mechanism for resolving names that
works entirely within Aeron.
The first thing we needed to tackle was re-resolving IP addresses when peer nodes went away and came back with
a different address.  Fortunately we already have a indicators within the existing protocol that allows the media driver
to detect when nodes have died.  Aeron continually sends data frames or heartbeats (sender to receiver) and status
messages (receiver to sender) during normal running.  We can use the absence of these messages as a
mechanism to detect that a node (that is identified by name rather than IP address) needs to be re-resolved.
Periodically the sender and receiver will scan their endpoints to see if any having been missing regular updates and
if those endpoints were identified by a name, trigger a re-resolution.  The simple solution here would be to in-place
re-resolve the name to an address, e.g. using getaddrinfo.  However, one of the reasons that Aeron is incredibly
fast (did I mention that already) is that it has a very principle based approach to its design.  One of the principles is
"Non-blocking IO in the message path".  This is to avoid any unusual stalls caused by the processing of blocking IO
operations.  The call to resolve a host name can block for extended periods of time (BTW, if you are ever using an
app on an other fast machine and it stalls for weird periods of time, it is worth asking the question, is it DNS causing
the problem).  Therefore we want to offload name resolution from our sender and receiver threads (the message
path) onto the conductor where we can perform the slower blocking operations.
It was apparent very early on that we could could make the resolution of names an abstract concept.  Obviously
using DNS and host names is the most obvious solutions, but it would be interesting to allow for names to come
from other sources.  E.g. we could name individual media drivers and use those names with our channel
configuration.  This allows a couple of neat behaviours.  All of the configuration for naming can be self contained
within Aeron itself independent of DNS, which may require configuration of a separate system and we could also
allow names to resolve to more that just IP addresses, e.g. host and port pairs or maybe direct to MAC addresses*
in the future.
* Bonus points if you can figure out why this might be useful.
To support this in both the Java and C media drivers have the concept of a name resolver, with 2 production
implementations, default (host name based) and driver where the media drivers are responsible to managing the list
of names.  With the driver based name resolution we need a mechanism to communicate the names between the
instances of the media driver across the network.
To allow driver names to propagate across the network, Aeron supports a gossip-style protocol, where we have an
additional frame type (resolution frame) that contains mappings of names to addresses.  Currently, only IPv4 and
IPv6 addresses are supported, but there is scope for adding others later.
The Need For Naming
Re-Resolution
Name Resolvers
Enter the Gossip Protocol
Michael Barker
View my complete profile
About Me
Java Code Geeks
▼  2020 (3)
▼  April (1)
I Heard a Rumour...
►  March (2)
►  2014 (4)
►  2013 (3)
►  2012 (9)
►  2011 (12)
►  2010 (5)
►  2009 (6)
►  2008 (4)
►  2007 (1)
Blog Archive
More
Create Blog Sign In
5/8/2026, 2:05 pm
Bad Concurrency
Page 2 of 5
https://bad-concurrency.blogspot.com/
Posted by Michael Barker at 21:01 
No comments: 
To make this work, for each media driver we specify 3 things.  The name for the media driver (this will default to the
host name when not specified), a bootstrap neighbour to send initial name resolutions to and a resolver interface. 
The most important option is the resolver interface as specifying this will enable the driver name resolution.  It also
determines which network interface to use to send and receive resolution frames and is the address reported to the
neighbors for self-resolutions.  This can also be a wildcard address (e.g. 0.0.0.0), in which case the neighbors will
use the source address of the received resolution frames to identify that node.
On start each of the nodes will have an empty set of neighbour nodes and a bootstrap neighbour.  Every 1s the
driver name resolver will send out a self resolution, i.e. tell all the nodes that it knows about, what its own name and
address are.  This will be sent (via UDP) to all of its known neighbour nodes and the bootstrap node (if not already in
the neighbour list).  Because the neighbour list is initially empty, then messages will only be sent to bootstrap
neighbours on the first pass.  The bootstrap neighbour can be specified using a host name and the driver name
resolver will ensure that it is re-resolved periodically in case it too has died and come back with a different IP
address.
As a result of this the driver name resolvers will start to receive resolution frames.  The name/address entries from
these frames will be added to a cache and the neighbor list.  If the resolution frame has come through as a
notification of a self resolution we update a last activity timestamp for that node.
Every 2s, the media driver will send its cache of name/address pairs to all of its neighbours, so eventually all of the
nodes will know about all of the other as the name/address entries are shared around the cluster.  At the higher
layer the conductor when trying to resolve a name to a supplied address on a channel URI will call the driver name
resolver first, which can resolve the name from its cache, handing off to the default resolver if not found.
Periodically the cache and the neighbor list will be checked to see if we are still receiving self resolutions for a
particular node.  If the last activity timestamp hasn't been been updated recently enough then the entries are evicted
from the cache and neighbour list under the assumption that the neighbour has died.
All of this is happening on the conductor thread so that it will not impact the performance of the sender and the
receiver.  This is primarily designed for small clusters of nodes as all nodes will be gossiping to all other nodes once
the resolutions have propagated across the network.  It is not designed for large scale system wide name
resolution.  However, it is a very new feature and we will expect to evolve over time as users experiment with it.
With a lot of the algorithms within Aeron it is often not possible to pick a single implementation, so we offer the ability
to provide your own implementation (e.g. flow control, congestion control).  Name resolution fits into that model as
well.  There is an interface for the Java Driver and a function pointer based struct on the C driver that can be
implemented by a user.  So if there is a custom name resolution strategy that you would prefer to use, it can be
plugged in quite easily.
If you look carefully, you notice that there is a 2-phase approach to resolving a name.  There is lookup method and a
resolve method.  The lookup method takes a name and returns a host name, UDP port pair, e.g.
'example.com:2020', where as the resolve function takes in the host name portion of that pair and returns an internet
address.  The additional param name is so the resolver can distinguish between an endpoint and a control address.
While perhaps not a ground-breaking feature, it is a useful one.  It manages to provide the convenience of support
name-based resolution without compromising on the latency goals of Aeron.   It is supported in both the Java
(1.26.0) and C (1.27.0) media drivers.  Feedback is always welcome and check out the wiki for more information.
Write your own
Conclusion
Thursday, 19 March 2020
Flow Control in Aeron
One of my more recent projects has led me to become more involved in the Aeron project.  If you are unaware of
Aeron, then head over to the Github site and check it out.  At its core is an reliable messaging system that works
over UDP, Multicast UDP and IPC.  It also contains an archiving feature for recording and replay and (still under
active development) an implementation of the Raft protocol for clustering.  Did I mention that it was fast too.
I've spent the last few weeks buried in the various strategies the Aeron has for flow control.  Specifically modifying
the existing flow control strategies and adding more flexible configuration on a per channel basis.  Before I jump into
that it would be useful to cover a little background first.
Within a distributed system the purpose of flow control is to limit the rate of a sender so that is does not overrun it's
associated receiver.  UDP does not come with any form of flow control, therefore it is easy to create a sender that
will out pace the receiver, leading to message loss.  There are a number of different forms of flow control, but I'm
going to focus on the sliding window flow control protocol used by TCP and Aeron. The sliding window protocol
requires that the sender maintain a buffer of data (referred to as a window).  The size of this window will typically
communicated from the receiver to the sender as part of the protocol.  With a bi-directional protocol like TCP the
size of the window is communicated in each TCP segment header.  This is the amount of data that the sender can
transmit to the receiver before having to wait until an acknowledgement is received.  If the application thread on the
receiver side is busy and does not read the data from the socket and the sender continues to transmit, the window
What is flow control?
5/8/2026, 2:05 pm
Bad Concurrency
Page 3 of 5
https://bad-concurrency.blogspot.com/
view raw
size value will decrease until it reaches 0, at which time the sender must stop and wait for an acknowledgement with
a non-zero window size before sending again.  There is a lot more networking theory around sizing the flow control
window in order to get full utilisation of the network.  But I will leave that as an exercise for the reader.
With Aeron and UDP unicast it is very similar to TCP, however Aeron is a unidirectional protocol where the receivers
send status messages to indicate to the sender that it is ready to receive data and how much.  The status message
indicates where the subscriber is up to using the consumption term id and consumption term offset for a specific
channel/stream/session triple.  The receiver window value is the amount of data that can be sent from that position
before the sender needs to stop and wait for a new status message indicating the the receiver is able to consume
more data.  The size of the receiver window is at most of ½ of the term size and at least the size of the MTU
(maximum transfer unit).
However, one of the neat features of Aeron is that it supports multicast (and multi-destination-cast, for which the
same rules will apply), where there are multiple receivers for the same publication.  In this situation how do we
determine what values should be used for the flow control window?  This is a question that has no one right answer,
so Aeron provides a number of configuration options and it is also possible to plug in your own strategy.
In fact Aeron is the only tool that supports UDP multicast messaging with dynamic flow control (that we're aware of).
The simplest and fastest form of multicast flow control is a strategy where we take the maximum position of all of the
receivers and use that value to derive limit that the sender can use for publication.  This means any receivers that
are not keeping up with the fastest one may fall behind and experience packet loss.
This is the inverse of the max flow control strategy, where instead we take minimum of all of the available receivers. 
This will prevent slower nodes (as long as they are still sending status messages) from falling behind.  However this
strategy does run the risk that the slower nodes can hold up the rest of the receivers by causing back pressure
slowing the publisher.  Because this strategy needs to track all of the individual receivers and their positions, it also
must handle the case that a node has disappeared altogether.  E.g. it has been shutdown or crashed.  This is
handled via a timeout (default 2s, but configurable).  If status messages for a receiver have not been seen that
period of time, that receiver is ejected from the flow control strategy and the publisher is allowed to move forward.
Tagged flow control is a strategy that attempts to mitigate some of the short comings of the min flow control
strategy.  It works by using a min flow control approach, but only for a subset of receivers that are tagged to be
included in the flow control group.  The min flow control strategy is a special case of this strategy where are all
receivers are considered to be in the group.
One of the new features that came with Aeron 1.26.0 was the ability to control the flow control strategy directly from
the channel URI allowing for fine grained control over each publication and subscription.  Defaults can also be
specified on the media driver context.  On the publication side the channel can be specified as:
flow control params hosted with ❤ by GitHub
The min and max flow control settings for the publication are the simplest, but the tagged one starts to get a little bit
interesting.  The ,g:1001 specifies that the group tag is 1001 and any receiver that want to be involved in flow
control for this publication will need to specify that group tag.  The subscription channel URI show how to ensure
that the receiver sends the appropriate group tag so that it will be included in the publishers flow control group.
The tagged flow control strategy is really useful for receiving from a channel where there are a number of different
types of subscribers that have different reliability requirements.  A good example is where there is a flow of events
that needs to go to a gateway service to be sent out to users, perhaps via HTTP and also needs to go to a couple of
archiving services to store the data redundantly in a database.   It may be possible for the gateway nodes to easily
deal with message loss, either by reporting an error to the user or re-requesting the data.  However it may not be
possible for the archiving service nodes to do so.  In this case the publication would specify the tagged flow control
strategy and the subscriptions on the archiving services would use gtag parameter to ensure that they are included
in the flow control group.  The gateway services could leave the gtag value unset and not impact the flow control on
the publisher.
While being able to include just the important subscribers into a flow control group so that they aren't overrun by the
publisher is useful, there would still be an issue.  If both of our archiving services happened to be down eventually
their receivers would be timed out and removed from the group.  Wouldn't it be great if we could require that a group
contain a certain number of tagged receivers before the publication can report that it is connected.  That way we
could ensure that our archiving service nodes were up before we started publishing data.
Max Flow Control
Min Flow Control
Tagged Flow Control (previously known as Preferred Flow Control)
Configuring Flow Control
1
// Publisher
2
aeron:udp?endpoint=224.20.30.39:24326|fc=max            // max strategy
3
aeron:udp?endpoint=224.20.30.39:24326|fc=min            // min strategy
4
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001  // tagged strategy, group 1001
5
6
// Subscriber
7
aeron:udp?endpoint=224.20.30.39:24326|gtag=1001         // tagged subscription
5/8/2026, 2:05 pm
Bad Concurrency
Page 4 of 5
https://bad-concurrency.blogspot.com/
Posted by Michael Barker at 22:20 
No comments: 
view raw
view raw
Turns that this is also now possible with the release of 1.26.0.  For both the tagged flow control and the min flow
control strategies we can specify a group minimum size that must be met before a publication can be considered
connected.  This is independent of to the requirement that there needs to be one connected subscriber.  Therefore
the default value for this group minimum size is 0.  Like the strategy and the flow control group, the group minimum
size can be specified on the channel URI.
flow control params 2 hosted with ❤ by GitHub
In both of these cases the group minimum size is set to 3.  For the min flow control strategy we would need at least
3 connected receivers, for the tagged flow control strategy we would need at least 3 connected receivers with tag
1001 and any receivers without the tag are disregarded.
One last new feature available on the channel URI configuration is the ability to specify the length of the timeout for
the min and tagged flow control strategies.  As mentioned the earlier this will default to 2s, but can be set to any
value.  Some care should be taken in specifying this value, if it is too short then receivers may frequently timeout
during normal running.  Status messages are emitted at least once every 200 ms (more if necessary), so any shorter
than that would not be useful.  Too long and a failed receiver could result in a significant back pressure stall on the
publisher.  Setting this for min and tagged flow control strategies:
flow control params 3 hosted with ❤ by GitHub
As mentioned earlier the idea of using flow control to provide dynamic back pressure for a multicast messaging bus
is a unique and powerful feature of Aeron.  Being able to configure these settings on a per publication provides a an
extra level of flexibility that to help our users to build the system that they need.
Flow Control Based Connectivity
1
aeron:udp?endpoint=224.20.30.39:24326|fc=min,g:/3         // group min size 3
2
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001/3  // group min size 3
Time Outs
1
aeron:udp?endpoint=224.20.30.39:24326|fc=min,g:/3,t:5000ms     // timeout 5s
2
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001/3,t:5s  // timeout 5s
Summary
Posted by Michael Barker at 04:14 
No comments: 
Resurrecting my blog
If you have been a long time follower of my blog you will have noticed that is has been a really long time since I
posted any new content.  Since my last post six years ago at lot has changed.  I no longer work at LMAX an have
started out as an independent consultant.  If you are part of an organisation looking for assistance in building
software, especially if you facing challenges around performance (throughput, latency, scalability, or efficiency) then
I might be able to help.  My services and contact details are available as http://ephemeris.tech.
I am hoping to start posting again about some of the work I've doing more recently and good potion of it will be open
source, so there should be plenty to share.
Wednesday, 3 December 2014
Read more »
Posted by Michael Barker at 01:13 
15 comments: 
The "Uncanny Valley" of L3 Cache Contention
While preparing for my talk at QCon SF 2014, I wanted to investigate a theory around how micro-benchmarks are
not a useful reflection of how software may behave when run as part of a larger application.  Specifically due
contention in the last-level cache (L3* in current Intel CPUs).
Tuesday, 23 September 2014
Speaking in October and November
I'll be giving some talks over the next few months:
8 Oct: Auckland Software Craftsmanship - 6 Years of test automation.
16 Oct: Auckland JVM Group - Stuff I learned about performance.
5 Nov: QCon San Francisco 2014 - Stuff I learned about performance.
5/8/2026, 2:05 pm
Bad Concurrency
Page 5 of 5
https://bad-concurrency.blogspot.com/
Older Posts
Home
Subscribe to: Posts (Atom)
Posted by Michael Barker at 04:38 
No comments: 
Wednesday, 23 April 2014
Posted by Michael Barker at 02:07 
No comments: 
YOW! 2013 Video
My talk on the Disruptor at YOW! 2013 is now available at the YOW! Eventer site.
Wednesday, 29 January 2014
Read more »
Posted by Michael Barker at 19:39 
8 comments: 
Linux Alternatives and Oracle Java
If, like me, you prefer to run the Oracle version of Java on your Linux machine as the default JDK, you will often find
that the Linux distro will have other ideas.  Fedora for example has a number of Java based applications as part of
the distribution which will include a dependency on the OpenJDK.  When the distro installs OpenJDK is will generally
be setup as the default for executing the various Java binaries (e.g. 'java', 'javac').  However, the team at Redhat
built a system called alternatives which maintains a set of symbolic links that allows the user to switch between
multiple implementations of a package the supports the same functionality.  I've managed to understand enough
about the alternatives package that I can now easily switch between the Oracle JDK and the OpenJDK.
Simple theme. Powered by Blogger.
</div>
