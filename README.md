# Sounds from the Scape

Soundscape generated written in [SuperCollider](https://supercollider.github.io/).

There is a longer [project description](https://gregorius.rippenstein.art/works/sfts) and a collection of samples soundscapes on [SoundCloud](https://soundcloud.com/user-47380021).

This exists because I felt that I needed something that provides a sound scape or sound _teppich_ (german for carpet) to distract my conscious and allow myself to concentrate on my thoughts. I cannot write listening to music that has vocals or that stops after three minutes. I needed something that is calming yet changing, something to which my conscious attempts to find the undefined pattern in the music. I also meditate with headphones while listening to these soundscapes.

You mileage my vary however what I have found is that since I recorded the samples, I have images of the places where I made these records. That means that these images are triggered when I listen to a soundscape. I feel myself transported to the forest where I recorded various birds sounds or to the beach where I recorded the waves crashing onto the shore or mundane sounds such as a wineglass in my kitchen.

I am also fascinated by how simple sounds can be morphed into deeper, stranger sounds. A chicken sounds as it were a dog when slowed down, a wine glass as a church bell or birds sound the same played forward or backward. The combination of the morphed and alternated becomes the soundscape.

## How does it work?

Loopable samples are played endless and mutated while they play. Controlled by a Midi controller, currently that is an [Arturia MiniLab MK II](https://www.arturia.com/products/hybrid-synths/minilab-mkii/overview) with a specific [layout](/assets/sfts-arturia-layout.minilabmk2).

Many of the operations done using the midi controller can also be done directly with the mouse, only the pendulums require the use of the midi controller.

## User Interface

When started, the interface looks like this:

<img src="/assets/screen-1-clean.png"/>

What does this all do? Here are some highlights:

<img src="/assets/screen-1.png"/>

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
./sclang -l .sclang_conf.yaml gui.scd
```


### Dockerfile

There is also a docker file that can be used to create an container, this also requires make and probably is best done using Linux:

```
make docker-run-on-debian-linux
```

That make task represents two commands:

```
docker build -t sfts-supercollider -f Dockerfile  .
docker run --rm --net=host --env DISPLAY=${DISPLAY} --env XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR} --privileged --device /dev/snd --device /dev/midi* --group-add audio --volume ${XDG_RUNTIME_DIR}:${XDG_RUNTIME_DIR} --volume $$XAUTHORITY:${HOME}/.Xauthority sfts-supercollider make random-composer
```


### MacOS Application

There is also a MacOS Application built with [Platypus](http://sveinbjorn.org/platypus) and [sc_osx_standalone](https://github.com/dathinaios/sc_osx_standalone).

The app is available under releases.


## What does it sound like?

A collection of samples soundscapes on [SoundCloud](https://soundcloud.com/user-47380021).

## What is the hardest thing to do?

> The hardest thing to describe is oneself, which is closely followed by the second hardest thing to describe: ones own projects. - Me.

