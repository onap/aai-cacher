FROM aaionap/aai-common:1.3.0

# Add the proper files into the docker image from your build
WORKDIR /opt/app/aai-cacher

# Expose the ports for outside linux to use
# 8444 is the important one to be used
EXPOSE 8444

HEALTHCHECK --interval=40s --timeout=10s --retries=3 CMD nc -z -v localhost 8444 || exit 1

ENTRYPOINT ["/bin/bash", "/opt/app/aai-cacher/docker-entrypoint.sh"]

RUN  mkdir -p /opt/aaihome/aaiadmin /opt/aai/logroot/AAI-CACHER

VOLUME /opt/aai/logroot/AAI-CACHER
VOLUME /opt/aaihome
VOLUME /opt/aaihome/aaiadmin

COPY /maven/aai-cacher/ .

ENV AAI_BUILD_VERSION @aai.docker.version@