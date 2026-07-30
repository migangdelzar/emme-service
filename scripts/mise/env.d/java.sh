#!/usr/bin/env bash
_sdkman_init="${HOME}/.sdkman/bin/sdkman-init.sh"
if [[ -f "$_sdkman_init" ]]; then
  source "$_sdkman_init" 2>/dev/null
  sdk env &>/dev/null || true
fi
if [[ -z "$JAVA_HOME" ]] || [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  _gradle_jdk="${HOME}/.gradle/jdks/jdk-25"
  if [[ -d "$_gradle_jdk" ]]; then
    export JAVA_HOME="$_gradle_jdk"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi
if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi
