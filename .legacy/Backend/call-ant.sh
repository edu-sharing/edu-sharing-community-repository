#!/bin/sh
java -Djava.ext.dirs=ant:lib  -Xmx256m  -jar ant/ant-launcher.jar  $@