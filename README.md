# Sounds from the Scape

Soundscape generated written in [SuperCollider](https://supercollider.github.io/).

There is a longer [project description](https://gregorius.rippenstein.art/works/sfts) and a collection of samples soundscapes on [SoundCloud](https://soundcloud.com/user-47380021).

This exists because I felt that I needed something that provides a sound scape or sound _teppich_ (german for carpet) to distract my conscious and allow myself to concentrate on my thoughts. I cannot write listening to music that has vocals or that stops after three minutes. I needed something that is calming yet changing, something to which my conscious attempts to find the undefined pattern in the music. I also meditate with headphones while listening to these soundscapes.

You mileage my vary however what I have found is that since I recorded the samples, I have images of the places where I made these records. That means that these images are triggered when I listen to a soundscape. I feel myself transported to the forest where I recorded various birds sounds or to the beach where I recorded the waves crashing onto the shore or mundane sounds such as a wineglass in my kitchen.

I am also fascinated by how simple sounds can be morphed into deeper, stranger sounds. A chicken sounds as it were a dog when slowed down, a wine glass as a church bell or birds sound the same played forward or backward. The combination of the morphed and alternated becomes the soundscape.

It has become a specific loop machine, but something that I developed to match my purpose. The freedom to reinvent the loop machine is the reason we have so many different loop machines.

## How does it work?

Loopable samples are played endless and mutated while they play. Controlled by a Midi controller, currently that is an [Arturia MiniLab MK II](https://www.arturia.com/products/hybrid-synths/minilab-mkii/overview) with a specific [layout](/assets/sfts-arturia-layout.minilabmk2).

Many of the operations done using the midi controller can also be done directly with the mouse, only the pendulums require the use of the midi controller.

## User Interface

When started, the interface looks like this:

<img src="/assets/screen-1-clean.png"/>

What does this all do:

<img src="/assets/screen-1.png"/>

- `Sample Effect Values`

    Effects that can be applied to a sample. Things like pan, reverb, volume and frequency. Each of the eight dials all duplicated for all 16 samples, i.e., each sample has its own values for each effect and those values are shown in the `Playing Samples Area`.

- `Pendulum Selector`

    Pendulums are triggers that move the dials automatically. Things like timer that moves a dial between two values in a give period of time. Record which records the changes to a dial of a period of time and endlessly replays those.

- `Synth and Sample Selector`

    Here a synth can be selected and then a sample is selected. The sample is then shown in the `Notes Playing` area. Once the note is clicked on, it is moved the the `Playing Samples Area`. This is done because the application was built with a midi controller with keys. So pressing a key would initially have the note playing and if a pad was hit the note was moved in the samples area. If the key was lifted, the note was removed again.

- `Playing Samples Area`

    All samples that are currently playing are shown here. The can be removed with a right click. All effects of a sample can be adjusted by using the mouse on the dials. The synth of a sample can be changed by using the synth dropdown for that sample.

- `Notes Playing`

    All notes are shown here. They can be moved to the samples area with a left click on the number-label or removed with a right click on the number label.

Top left hand corner is the `Load Scape` button for loading a scape, this is a good place to begin to have something playing.

### Loading a Scape

After loading a scape, in this case [scape.221121_144649.yaml](/assets/configs/scape.221121_144649.yaml), the player shows the samples:

<img src="/assets/screen-load-scape.png"/>

Some highlights of what is going on:

<img src="/assets/screen-load-scape-highlights.png"/>

- Marked with a green star is the load scape button. Some predefined scapes come with the application and can be modified. Changes can be saved with the `Save Scape` button.
- The red rectangles are indicating the that the eight dials for each sample are the same effects as shown at the top. Each effect is applied separately to each sample that is playing. The effects are: volume, pan, speed, reverb, frequency, damp, mix, front-back location.
- The dials marked with the blue hexagon are pendulum dials. Pendulum is the term I came up with to describe patterns that move dials automatically. There are a number of a pendulums: record, timer, randomise, and a couple more. They can only be sensibly configured using a midi controller. Pendulums are simply predefined values for a dial over a time period. For example, a timer will move a dial between two values over a specific duration, simply back and forth.
- The black arrows point to the synth drop down for each playing sample. Changing the synth is probably the simplest change that can be made to a sample.

A new sample can be selected via the `Sample Selector`, a sample first appears as a note in `Notes Playing` area. By clicking on it, it will be moved to the samples area. The volume is set to zero by default, so to hear a sample, the volume dial for the sample needs uplifting - volume dial is top, left dial for each sample.

## Setup

### Locally

Install [SuperCollider](https://supercollider.github.io/), for MacOS the [DMG](https://supercollider.github.io/downloads#mac) or for Linux `sudo apt-get install supercollider` or [download from SC](https://supercollider.github.io/downloads#linux).

Clone the repository and change directory:
```
git clone git@github.com:gorenje/sfts
cd sfts
```

The code assumes there is a link to the `sclang` executable in the base directory. To do this,

On MacOS:

```
ln -s /Applications/SuperCollider.app/Contents/MacOS/sclang
```

Or on Linux:

```
ln -s /bin/sclang
```

Install the [PortedPlugins](https://github.com/madskjeldgaard/portedplugins) from the assets directory:

On MacOS:

```
unzip assets/PortedPlugins-macos.zip -d "$(make extensions-folder)"
```

On Linux:

```
unzip assets/PortedPlugins-Linux.zip -d $(make extensions-folder)
```

Create a soft link to the samples directory in the assets folder:

```
ln -s assets/samples
```

Create a config directory in your home directory and copy the sample scapes and presets files into that directory:

```
mkdir ${HOME}/.sfts && cp assets/configs/*.yaml ${HOME}/.sfts/
```

Start the application with

```
$(readlink ./sclang) -l .sclang_conf.yaml gui.scd
```

You might also have to install the [exiftool](https://exiftool.org/) for the samples to have names and be listed. I make an assumption that is already installed, for MacOS there is a copy of [exiftool](/assets/mac_app/exiftool) or install it via [brew](https://formulae.brew.sh/formula/exiftool).


### Dockerfile

There is also a docker file that can be used to create an container:

```
docker build -t sfts-supercollider -f Dockerfile . && docker run --rm --net=host --env DISPLAY=${DISPLAY} --env XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR} --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume ${XDG_RUNTIME_DIR}:${XDG_RUNTIME_DIR} --volume $$XAUTHORITY:${HOME}/.Xauthority sfts-supercollider make random-composer
```

Releases are also available from [DockerHub](https://hub.docker.com), for examples release 0.0.1:

```
docker run --rm --net=host --env DISPLAY=${DISPLAY} --env XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR} --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume ${XDG_RUNTIME_DIR}:${XDG_RUNTIME_DIR} --volume $$XAUTHORITY:${HOME}/.Xauthority gorenje/sfts:0.0.1 make random-composer
```


### MacOS Application

There is also a MacOS Application built with [Platypus](http://sveinbjorn.org/platypus) and [sc_osx_standalone](https://github.com/dathinaios/sc_osx_standalone).

The app is available under [releases](https://github.com/gorenje/sfts/releases).


## What does it sound like?

A collection of samples soundscapes on [SoundCloud](https://soundcloud.com/user-47380021).

## What is the hardest thing to do?

> The hardest thing to describe is oneself, which is closely followed by the second hardest thing to describe: ones own projects. - Me.
