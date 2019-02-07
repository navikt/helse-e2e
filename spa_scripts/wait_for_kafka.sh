#!/bin/sh
TIMEOUT=30
HOST=$(printf "%s\n" "$1"| cut -d : -f 1)
PORT=$(printf "%s\n" "$1"| cut -d : -f 2)
shift
COMMAND=$@
wait_for_kafka() {
  for i in `seq $TIMEOUT` ; do
    result=$(echo 'dump' | nc "$HOST" "$PORT" | grep -o 'broker')
    if [ "$result" = "broker" ] ; then
      if [ -n "$COMMAND" ] ; then
        exec $COMMAND
      fi
      exit 0
    fi
    sleep 1
  done
  echo "Timed out" >&2
  exit 1
}
wait_for_kafka
