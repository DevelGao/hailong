FROM ubuntu:18.04

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y build-essential maven libsodium-dev \
    tmux wget iperf3 curl apt-utils iputils-ping expect npm git git-extras \
    software-properties-common openssh-server

# install java
RUN add-apt-repository ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-11-jdk && \
    rm -rf /var/lib/apt/lists/* 

# get hailong
RUN git clone --recursive https://github.com/devgaoEng/hailong.git
WORKDIR hailong/
RUN ./gradlew build -x test
WORKDIR /hailong/build/distributions
RUN tar -xzf hailong-1.0.0-SNAPSHOT.tar.gz
RUN ln -s /hailong/build/distributions/hailong-*-SNAPSHOT/bin/hailong /usr/bin/hailong
WORKDIR /usr/local/bin
RUN wget https://github.com/Whiteblock/hailong_log_EATER/releases/download/v1.5.10/hailong-log-parser && chmod +x /usr/local/bin/hailong-log-parser

WORKDIR /
#ENV PATH="/hailong/build/distributions/hailong-1.0.0-SNAPSHOT/bin:${PATH}"

ENTRYPOINT ["/bin/bash"]
