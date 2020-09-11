FROM hseeberger/scala-sbt:8u242_1.3.8_2.12.10

####### YARN INSTALLATION ########

## Found in https://github.com/yarnpkg/yarn/issues/6914#issuecomment-454165516

# Setup the base OS
RUN apt-get update -qq \
 && apt-get install -y --no-install-recommends build-essential  \
    apt-transport-https curl ca-certificates gnupg2 apt-utils nodejs

# Install node from nodesource
 RUN curl -sL https://deb.nodesource.com/setup_10.x | bash - \
  && apt-get install -y nodejs

# Install yarn
RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
 && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
 && apt-get update -qq \
 && apt-get install -y yarn

# Install ammonite

RUN curl -Lo /usr/local/bin/amm https://github.com/lihaoyi/Ammonite/releases/download/2.2.0/2.12-2.2.0 && chmod +x /usr/local/bin/amm && amm --help

# Test
RUN yarn --version

####### END OF YARN INSTALLATION ########

### GPG TWEAKS ###

COPY gpg.conf .gnupg/gpg.conf
COPY gpg-agent.conf .gnupg/gpg-agent.conf
RUN echo RELOADAGENT | gpg-connect-agent

### END OF GPG TWEAKS ###
