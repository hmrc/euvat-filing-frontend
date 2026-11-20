#!/usr/bin/env bash

# Add the server certificate used to authenticate the other party.
if [ -n "${HMRC_TRUSTSTORE}" ] ; then
  echo $HMRC_TRUSTSTORE | base64 --decode > /app/hmrc_truststore.jks
fi

# Check if config environment variables are set
if [ -n "${SUBMISSION_KS}" ]; then
	# decode and write config
	echo $SUBMISSION_KS | base64 --decode > /app/keystore.jks
fi
