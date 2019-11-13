#!/usr/bin/env python2
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

import sys, csv, os
import xml.etree.ElementTree as ET

def writeCsv(xmlFile):
    tree = ET.parse(xmlFile)
    root = tree.getroot()
    writer = csv.writer(sys.stdout)
    project = os.environ["CI_PROJECT_NAME"] \
              if "CI_PROJECT_NAME" in os.environ else "unknown"
    branch = os.environ["CI_COMMIT_REF_NAME"] \
             if "CI_COMMIT_REF_NAME" in os.environ else "unknown"
    url = os.environ["CI_JOB_URL"] \
          if "CI_JOB_URL" in os.environ else "unknown"
    for test in root.iter("testcase"):
        failure = test.find("failure")
        time = (float(test.attrib["time"]) if "time" in test.attrib else 0.0)
        writer.writerow(["TESTRESULT",
                         project,
                         branch,
                         root.attrib["name"],
                         test.attrib["name"],
                         time,
                         "FAIL" if failure != None else "SUCCESS",
                         url])

alltests = []
for i in sys.argv[1:]:
    writeCsv(i)
