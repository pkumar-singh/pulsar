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

cp ./distribution/server/target/apache-pulsar-*-bin.tar.gz dev/
cp -R docker/pulsar/scripts dev/scripts
cp -R tests/docker-images/latest-version-image/scripts/* dev/scripts/
cp -R tests/docker-images/latest-version-image/ssl dev/ssl
cp -R tests/docker-images/latest-version-image/conf dev/conf
cp tests/docker-images/java-test-functions/target/java-test-functions.jar dev/
cd dev && docker build . --tag apachepulsar/pulsar-test-latest-version:latest
