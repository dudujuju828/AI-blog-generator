#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.3.2
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# Determine project base directory
MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"

# Set JAVA_CMD
if [ -n "${JAVA_HOME-}" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVA_CMD="$JAVA_HOME/jre/sh/java"
  elif [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
  else
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    exit 1
  fi
else
  JAVA_CMD="$(command -v java)" || :
  if [ ! -x "${JAVA_CMD-}" ]; then
    echo "ERROR: JAVA_HOME is not set and 'java' command not found" >&2
    exit 1
  fi
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -r "$WRAPPER_JAR" ]; then
  echo "ERROR: Maven wrapper jar not found at $WRAPPER_JAR" >&2
  exit 1
fi

exec "$JAVA_CMD" \
  ${MAVEN_OPTS-} \
  ${MAVEN_DEBUG_OPTS-} \
  -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
  -classpath "$(native_path "$WRAPPER_JAR")" \
  org.apache.maven.wrapper.MavenWrapperMain \
  ${MAVEN_CONFIG-} \
  "$@"
