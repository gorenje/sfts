RcPendulumSettingsDial {
    var <>duration;
    var <>start;
    var <>end;

    *new {
        ^super.new.init
    }

    init {
    }

    incrementDial { |which, incVal|
        var mapVal = [0,127,\lin].asSpec;
        var but = switch( which,
            \duration, { duration  },
            \end,      { end       },
            \start,    { start     }
        );

        var val = mapVal.map( but.controlSpec.unmap( but.value ) );
        but.value = but.controlSpec.map(mapVal.unmap(val + incVal));
    }
}

RcPendulumSettings {
    var <>dials;
    var <>klzPendulum;
    var <>currentPendulum;

    *new { |klz|
        ^super.new.init(klz);
    }

    init { |klz|
        dials = RcPendulumSettingsDial.new;
        klzPendulum = klz;
        currentPendulum = nil;
    }

    incrementDial { |which, val|
        AppClock.sched(0, {
            dials.incrementDial(which, val);
            nil
        });
    }

    newPendulum { |midibus, dal, synth, argnum|
        if ( currentPendulum.isNil, {
            currentPendulum = klzPendulum.new(midibus, dal, this.dials);
            currentPendulum.setSynth( synth, argnum );
        }, {
            "WARNING: new pendulum created even though there is a current one".postln;
        })
        ^currentPendulum;
    }

    go {
        currentPendulum.go;
        currentPendulum = nil;
    }

    isLoaded {
        ^currentPendulum.notNil;
    }
}