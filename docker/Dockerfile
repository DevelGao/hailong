FROM openjdk:11.0.2-jre-slim-stretch

# copy application (with libraries inside)
COPY hailong /opt/hailong/

# List Exposed Ports
EXPOSE 8008 8084 8545 30303 30303/udp

# specify default command
ENTRYPOINT ["/opt/hailong/bin/hailong"]


# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Hailong" \
      org.label-schema.description="Ethereum 2.0 Beacon Chain Client" \
      org.label-schema.url="https://devgao.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/DevelGao/hailong.git" \
      org.label-schema.vendor="Devgao" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
