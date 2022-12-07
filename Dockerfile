FROM debian:11-slim

ENV DEBIAN_FRONTEND noninteractive

RUN ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime

RUN apt-get --quiet --yes update && apt-get install -yqq \
  supercollider \
  exiftool \
  unzip \
  make \
  coreutils \
  procps \
  findutils \
  gawk

RUN cd /usr/bin && ln -s /bin/readlink

ARG NB_USER=sfts
ARG NB_UID=1000
ENV USER ${NB_USER}
ENV NB_UID ${NB_UID}
ENV HOME /home/${NB_USER}

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid ${NB_UID} \
    ${NB_USER}

WORKDIR /home/${NB_USER}

USER ${NB_USER}

RUN mkdir -p classes extensions .sfts samples
COPY --chown=${NB_USER} classes/ classes/
COPY --chown=${NB_USER} extensions/ extensions/
COPY --chown=${NB_USER} assets/configs/ .sfts/
COPY --chown=${NB_USER} assets/samples/ samples/

COPY --chown=${NB_USER} assets/PortedPlugins-Linux.zip .
RUN unzip /home/${NB_USER}/PortedPlugins-Linux.zip -d extensions

COPY --chown=${NB_USER} Makefile .
COPY --chown=${NB_USER} gui.scd .
COPY --chown=${NB_USER} .sclang_conf.yaml .
COPY --chown=${NB_USER} .jackdrc .

RUN ln -s $(which sclang)

CMD ["make", "random-composer]
