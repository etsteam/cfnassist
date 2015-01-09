#!/bin/bash

base=`dirname $0`

java -cp "$base/cfnassist-${project.version}.jar:$base/conf:$base/lib/*" tw.com.commandline.Main $*
