#!/bin/sh

cd "$(dirname "$0")"
cd ..

if ! type "sbt" > /dev/null; then
  /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
  brew install sbt
fi

sbt "gatling:testOnly com.medialets.MainSimulation" "gatling:testOnly com.medialets.LogErrors"