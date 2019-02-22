#!/bin/sh
TIMEOUT=60
COMMAND=$@
wait_for_e2e() {
  for i in `seq $TIMEOUT` ; do
    result=$(wget -qO- http://$E2E_HOST/ruok)
    echo $result
    if [ "$result" = "imok" ] ; then
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
wait_for_e2e
