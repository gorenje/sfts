# SCLANG=/Applications/SuperCollider/SuperCollider.app/Contents/MacOS/sclang
# SCLANG=/Applications/SuperCollider.app/Contents/MacOS/sclang
SCLANG=$(shell /usr/bin/readlink sclang)

docker-build:
	docker build -t sfts-supercollider -f Dockerfile  .

docker-run-on-debian-linux: docker-build
	docker run --rm --net=host --env DISPLAY=${DISPLAY} --env XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR} --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume ${XDG_RUNTIME_DIR}:${XDG_RUNTIME_DIR} --volume $$XAUTHORITY:${HOME}/.Xauthority sfts-supercollider make random-composer

# Following this and the docker image might start on macos:
#      https://gist.github.com/cschiewek/246a244ba23da8b9f0e7b11a68bf3285
# But sound device does not
docker-run-macos-quartz: docker-build
	docker run --rm --net=host --env DISPLAY=${HOSTNAME}:0 --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume /tmp/.X11-unix:/tmp/.X11-unix --volume $$XAUTHORITY:${HOME}/.Xauthority sfts-supercollider make random-composer

docker-run-interactive: docker-build
	docker run --rm --net=host --env DISPLAY=${DISPLAY} --env XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR} --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume ${XDG_RUNTIME_DIR}:${XDG_RUNTIME_DIR} --volume $$XAUTHORITY:${HOME}/.Xauthority -it sfts-supercollider /bin/bash

osx-release:
	#echo "DANGER: Distructive action, edit Makefile and remove this notice"
	#and_remove_this
	## Destruction and disaster follow .... beware
	rm -fr sc_osx_standalone
	git clone --depth=1 --branch 3.12.2a git@github.com:dathinaios/sc_osx_standalone.git
	cd sc_osx_standalone && unzip Frameworks.zip && rm Frameworks.zip
	cd sc_osx_standalone && timeout 5 sh run.sh || :
	rm -fr sc_osx_standalone/Resources/sounds/*
	cp assets/mac_app/exiftool sc_osx_standalone/Resources
	cd sc_osx_standalone && mkdir Resources/assets SCClassLibrary/install
	cp -R assets/configs sc_osx_standalone/Resources/assets/
	cp -R assets/samples sc_osx_standalone/Resources/assets/
	cd sc_osx_standalone && ln -s Resources/assets/samples
	cd sc_osx_standalone && rm -fr .git .gitattributes README.md
	yes | cp assets/mac_app/init.scd sc_osx_standalone/init.scd
	cp classes/* sc_osx_standalone/SCClassLibrary
	cp extensions/* sc_osx_standalone/SCClassLibrary
	cd sc_osx_standalone/SCClassLibrary/install && unzip ../../../assets/PortedPlugins-macos.zip
	echo "Now start Platypus to build app"

shell:
	${SCLANG} -l .sclang_conf.yaml

random-composer: kill-existing-server
	${SCLANG} -l .sclang_conf.yaml gui.scd

start-jackd:
	jackd -d alsa -d hw:1 -r 48000 -p 4096 -n2 -s

.PHONY: sample-comments
sample-comments:
	find ./samples -type f \( -name \*.wav -o -name \*.aiff \) -print | xargs -I {} exiftool -T -FileName -Comment {}

.PHONY: kill-existing-server
kill-existing-server:
	 ps auxwww | grep scsynth | grep -v grep | awk '// { print $$2 }' | xargs -I {} kill {}

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
