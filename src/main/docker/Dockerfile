FROM @aai.docker.namespace@/aai-common-ubuntu:@aai.base.image.version@

RUN  mkdir -p /opt/aaihome/aaiadmin /opt/aai/logroot/AAI-CACHER

VOLUME /opt/aai/logroot/AAI-CACHER
VOLUME /opt/aaihome
VOLUME /opt/aaihome/aaiadmin

HEALTHCHECK --interval=40s --timeout=10s --retries=3 CMD nc -z -v localhost 8444 || exit 1

# Add the proper files into the docker image from your build
WORKDIR /opt/app/aai-cacher
COPY /maven/aai-cacher/ .

ENV AAI_BUILD_VERSION @aai.docker.version@
# Expose the ports for outside linux to use
# 8444 is the important one to be used
EXPOSE 8444
ENTRYPOINT ["/bin/bash", "/opt/app/aai-cacher/docker-entrypoint.sh"]