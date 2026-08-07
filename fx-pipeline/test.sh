#!/bin/bash
OS=$(uname)
if [ "$OS" = "Linux" ]; then
    SELECTOR_OPT="-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider"
else
    SELECTOR_OPT=""
fi
export JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC \
$SELECTOR_OPT"
echo "SELECTOR_OPT is: '$SELECTOR_OPT'"
echo "JVM_OPTS is: '$JVM_OPTS'"
java $JVM_OPTS -XshowSettings:properties -version 2>&1 | grep SelectorProvider
