#!/bin/bash

mkdir -p $ARTIFACTS_DIR
cat << EOF > artifacts/junit-dummy.xml
<testsuite tests="1">
    <testcase classname="dummy" name="dummytest"/>
</testsuite>
EOF
