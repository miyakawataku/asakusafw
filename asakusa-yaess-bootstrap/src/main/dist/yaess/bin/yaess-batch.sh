#!/bin/sh
#
# Copyright 2011-2012 Asakusa Framework Team.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


usage() {
    cat 1>&2 <<EOF
YAESS - A portable Asakusa workflow processor

Usage:
    yaess-batch.sh batch-id \\
        [-A <key>=<value> [-A <key>=<value>  [...]]] \\
        [-D <key>=<value> [-D <key>=<value>  [...]]]

Parameters:
    batch-id
        batch ID of current execution
    -A <key>=<value>
        argument for this execution
    -D <key>=<value>
        definition for this execution

Definitions:
    -D skipFlows=<flowId>[,<flowId>[,...]]
        ignores target jobflow execution
    -D serializeFlows=true
        serializes each jobflow execution (for debug)

Examples:
    # run a batch "example.batch"
    yaess-batch.sh example.batch
    
    # run a batch "example.params" with {date="2011-03-31", code="123"}
    yaess-batch.sh example.params -A date=2011-03-31 -A code=123
    
    # run a batch "example.skip" except joblows "first" and "second"
    yaess-batch.sh example.skip -D skipFlows=first,second

Environment Variables:
    \$ASAKUSA_HOME
        The location where the Asakusa Framework is installed
    \$YAESS_OPTS
        Java VM options for YAESS execution
    \$YS_PATH_SEPARATOR
        Path separator character (default is ':')
EOF
}

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

_OPT_BATCH_ID="$1"
shift

_YS_ROOT="$(dirname $0)/.."
if [ -e "$_YS_ROOT/conf/env.sh" ]
then
    . "$_YS_ROOT/conf/env.sh"
fi

if [ "$ASAKUSA_HOME" = "" ]
then
    echo '$ASAKUSA_HOME'" is not defined" 1>&2
    exit 1
fi

if [ "$YS_PATH_SEPARATOR" = "" ]
then
    _YS_PATH_SEPARATOR=':'
else 
    _YS_PATH_SEPARATOR="$YS_PATH_SEPARATOR"
fi

_YS_PROFILE="$_YS_ROOT/conf/yaess.properties"
_YS_SCRIPT="$ASAKUSA_HOME/batchapps/$_OPT_BATCH_ID/etc/yaess-script.properties"

_YS_CLASSPATH=""
if [ -d "$_YS_ROOT/conf" ]
then
    _YS_CLASSPATH="$_YS_ROOT/conf"
fi
if [ -d "$_YS_ROOT/lib" ]
then
    for f in $(ls "$_YS_ROOT/lib/")
    do
        if [ "$_YS_CLASSPATH" = "" ]
        then
            _YS_CLASSPATH="${_YS_ROOT}/lib/$f"
        else
            _YS_CLASSPATH="${_YS_CLASSPATH}${_YS_PATH_SEPARATOR}${_YS_ROOT}/lib/$f"
        fi
    done
fi

_YS_PLUGIN=""
if [ -d "$_YS_ROOT/plugin" ]
then
    for f in $(ls "$_YS_ROOT/plugin/")
    do
        if [ "$_YS_PLUGIN" = "" ]
        then
            _YS_PLUGIN="$_YS_ROOT/plugin/$f"
        else
            _YS_PLUGIN="${_YS_PLUGIN}${_YS_PATH_SEPARATOR}${_YS_ROOT}/plugin/$f"
        fi
    done
fi

_YS_CLASS="com.asakusafw.yaess.bootstrap.Yaess"

echo "Starting YAESS"
echo "   Profile: $_YS_PROFILE"
echo "    Script: $_YS_SCRIPT"
echo "  Batch ID: $_OPT_BATCH_ID"
echo "  Plug-ins: $_YS_PLUGIN"
echo " Classpath: $_YS_CLASSPATH"
echo "Main Class: $_YS_CLASS"
echo " Arguments: $@"
echo "Properties: $YAESS_OPTS"

java \
    $YAESS_OPTS \
    "-Dcom.asakusafw.yaess.log.batchId=$_OPT_BATCH_ID" \
    -classpath "$_YS_CLASSPATH" \
    "$_YS_CLASS" \
    -profile "$_YS_PROFILE" \
    -script "$_YS_SCRIPT" \
    -batch "$_OPT_BATCH_ID" \
    -plugin "$_YS_PLUGIN" \
    "$@"

_YS_RET=$?
if [ $_YS_RET -ne 0 ]
then
    echo "YAESS Failed with exit code: $_YS_RET" 1>&2
    echo "Classpath: $_YS_CLASSPATH" 1>&2
    echo "  Profile: $_YS_PROFILE" 1>&2
    echo "   Script: $_YS_SCRIPT" 1>&2
    echo " Batch ID: $_OPT_BATCH_ID" 1>&2
    echo " Plug-ins: $_YS_PLUGIN" 1>&2
    echo "Arguments: $@" 1>&2
    echo "Properties: $YAESS_OPTS" 1>&2
    echo "Finished: FAILURE"
    exit $_YS_RET
fi

echo "Finished: SUCCESS"
