#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

set -o pipefail
set -o errexit

if [ -z "$CI_JOB_ID" ]; then
    echo "CI_JOB_ID environment variable must be set"
    exit 1
fi

if [ -z "$SSH_PRIVATE_KEY" ]; then
    echo "SSH_PRIVATE_KEY environment variables must all be set";
    exit 2
fi

if [ ! -d pulsar-broker ]; then
    echo "Script must be run from top-level of repo"
    exit 3
fi

CUR_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
NEW_VERSION=$(echo $CUR_VERSION | sed 's/\([0-9]*\.[0-9]\.[0-9]\).*/\1.SPLK.'$CI_JOB_ID'/')
echo "Updating poms to version ${NEW_VERSION}"

mvn -f buildtools/pom.xml versions:set -DnewVersion=$NEW_VERSION
mvn -f pulsar-sql/presto-distribution/pom.xml versions:set -DnewVersion=$NEW_VERSION
mvn versions:set -DnewVersion=$NEW_VERSION

TAG=release-$NEW_VERSION

git commit -am "Committing version $TAG"
git tag $TAG -m "Tag for $TAG"

build/go-go tag
