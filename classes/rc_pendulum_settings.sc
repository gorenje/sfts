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
    var <>rcGui;
    var <>rcMidiBus;

    *new { |klz,gui,midibus|
        ^super.new.init(klz,gui,midibus);
    }

    init { |klz,gui,midibus|
        dials           = RcPendulumSettingsDial.new;
        klzPendulum     = klz;
        currentPendulum = nil;
        rcGui           = gui;
        rcMidiBus       = midibus;
    }

    incrementDial { |which, val|
        { dials.incrementDial(which, val); }.defer;
    }

    newPendulum {
        if ( currentPendulum.isNil, {
            currentPendulum = klzPendulum.new(
                rcMidiBus,
                rcGui.lastDialedDial,
                dials
            );
            currentPendulum.setSynth(
                rcGui.currentSynth,
                rcGui.currentPad.knobIdx(rcGui.lastDialedDial) + 1
            );
            currentPendulum.setGui( rcGui );
        }, {
            Error("WARNING: new pendulum created even though there is a one").throw;
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