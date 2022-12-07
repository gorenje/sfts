RcPendulumBase {
    classvar <>pendulums;

    var <>dial;
    var <>synth;
    var <>argNum;
    var <>step;
    var <>stopNow;
    var <>settingsDials;
    var <>startDelay;
    var <>endDelay;

    *new { |midiBus,dal,settingDials|
        var obj;
        if ( pendulums.isNil, { pendulums = Dictionary.new });

        obj = super.new.init(midiBus,dal,settingDials);
        obj.stopNow       = false;
        obj.settingsDials = settingDials;
        obj.dial          = dal;
        obj.startDelay    = 0.0;
        obj.endDelay      = 0.0;

        if ( pendulums[dal].isNil, { pendulums[dal] = List.new; });
        pendulums[dal].add(obj);

        ^obj
    }

    *newEmpty {
        ^super.new.initEmpty;
    }

    *newFromYaml { |yamlData|
        var obj = nil;

        if ( (yamlData["argnum"] == "nil") || (yamlData["type"] == "Base"), {
            ^nil;
        });

        obj = switch( yamlData["type"].asSymbol,
            \Timer,         { RcPendulumTimer.newEmpty;         },
            \Recorder,      { RcPendulumRecorder.newEmpty;      },
            \Randomise,     { RcPendulumRandomise.newEmpty;     },
            \FadeOut,       { RcPendulumFadeOut.newEmpty;       },
            \FadeIn,        { RcPendulumFadeIn.newEmpty;        },
            \FadeInOneTime, { RcPendulumFadeInOneTime.newEmpty; }
        ).initFromYamlData(yamlData);

        obj.startDelay = yamlData.atFail("startDelay", { 0 }).asFloat;
        obj.endDelay   = yamlData.atFail("endDelay", { 0 }).asFloat;
        obj.stopNow    = false;
        obj.argNum     = yamlData.atFail("argnum", { 0 }).asInteger;

        ^obj;
    }

    *newWithPad { |yamlData, pad|
        var obj = RcPendulumBase.newFromYaml(yamlData);

        if ( obj.isNil, { ^nil; });

        obj.setSynth( pad.synth, yamlData["argnum"].asInteger );

        obj.dial = pad.knobs[obj.argNum-1];

        if ( pendulums.isNil, { pendulums = Dictionary.new });
        if ( pendulums[obj.dial].isNil, { pendulums[obj.dial] = List.new; });
        pendulums[obj.dial].add(obj);

        ^obj;
    }

    *dropDownList {
        ^[ ["Timer",     RcPendulumTimer],
           ["Recorder",  RcPendulumRecorder],
           ["Fade In",   RcPendulumFadeIn],
           ["Fade Out",  RcPendulumFadeOut],
           ["Randomise", RcPendulumRandomise],
           ["Delete",    RcPendulumRemove],
        ].repeatItems(3);
    }

    initEmpty {
        stopNow = false;
    }

    initFromYamlData { |yamlData|
        ^this;
    }

    // return an initial value for the dial on which this pendulum acts.
    // if there isn't one, then return the current dial value. This is used
    // when storing the setup as a yaml file so that when the yaml is
    // loaded, the dial is already set to where the pendulum, that affects it,
    // will begin.
    initValue { |currValue|
        ^currValue;
    }

    setSynth { |s, n|
        this.synth = s;
        this.argNum = n;
    }

    stop { |keepValue = false|
        this.step = nil;
        this.stopNow = true;

        AppClock.sched(0.5, {
            if ( keepValue, {}, { this.dial.value = 0 });
            this.dial.setColors( knobColors: [
                Color(0.91764705882353, 0.91764705882353, 0.91764705882353),
                Color(),
                Color(0.85882352941176, 0.85882352941176, 0.85882352941176),
                Color() ] )
        });
    }

    isStopped {
        ^stopNow;
    }

    go {
        // ignore the go called from main program, we trigger ourselves when
        // we're good and ready! Each subclass needs to overwrite this.
    }

    setPrepareColour_ {
        AppClock.sched(0, {
            this.dial.setColors( knobColors: [
                Color(0.8,0.4,0.2),
                Color(),
                Color(0.85882352941176, 0.85882352941176, 0.85882352941176),
                Color()
            ])
        });
    }

    setLoopColour_ { |knobColors = nil|
        if ( knobColors.isNil, {
            knobColors = [
                Color(0.4,0.8,0.2),
                Color(),
                Color(0.85882352941176, 0.85882352941176, 0.85882352941176),
                Color()
            ];
        });

        AppClock.sched(0, {
            this.dial.setColors( knobColors: knobColors)
        });
    }

    asYaml { |pendId|
        ^format("pend%:\n"++
            "  type: Base\n" ++
            "  defined: false\n", pendId);
    }

    startFromYamlLoad {
        this.stop;
        ^this;
    }
}

//
// TODO need to support the stop() call in each of the loop_ methods
// TODO but at the various points where the delays now play a role,
// TODO i.e. if a pendulum is delaying and it's stop, once it comes out
// TODO of the delay, it should stop immediately and not go through
// TODO with it's effect.
//

RcPendulumRandomise : RcPendulumBase {
    var <>range;

    init { |midiBus,dal,settingDials|
        range      = [dal.value.asInteger, 0];

        this.setPrepareColour_;
    }

    initFromYamlData { |yamlData|
        step       = yamlData["stepDelay"].asFloat;
        range      = [ yamlData["rangeStart"].asInteger,
                       yamlData["rangeEnd"].asInteger];
        ^this;
    }

    loop_ {
        this.setLoopColour_([
            Color(0.65,0.8,0.2),
            Color(),
            Color(0.85882352941176, 0.85882352941176, 0.85882352941176),
            Color() ] );

        SystemClock.sched(startDelay, {
            SystemClock.sched(0.0, {
                var newval = Array.rand( 1, *range.sort).first;

                if ( this.stopNow, { nil }, {
                    this.synth.set(["arg",this.argNum-1].join, newval);

                    AppClock.sched(0, {
                        this.dial.value = newval;
                        nil
                    });

                    this.step;
                });
            });

            nil;
        });
    }

    go {
        startDelay = settingsDials.start.value;
        step       = settingsDials.duration.value;
        range[1]   = dial.value.asInteger;
        if ( range[0] == range[1], { this.stop; }, { this.loop_; });
    }

    asYaml { |pendId|
        ^format("pend%:\n"        ++
            "  type: Randomise\n" ++
            "  stepDelay: %\n"    ++
            "  rangeStart: %\n"   ++
            "  rangeEnd: %\n"     ++
            "  startDelay: %\n"   ++
            "  argnum: %\n",
            pendId,
            step.asFloat,
            range[0].asInteger,
            range[1].asInteger,
            startDelay.asFloat,
            argNum);
    }

    startFromYamlLoad {
        this.loop_;
        ^this;
    }

}

RcPendulumFadeOut : RcPendulumBase {
    var <>timeRangeValue;
    var <>startValue;

    init { |midiBus,dal,settingDials|
        this.setPrepareColour_;
    }

    initFromYamlData { |yamlData|
        timeRangeValue = yamlData["timeRangeValue"].asFloat;
        startValue = yamlData["startValue"].asInteger;

        ^this;
    }

    initValue { |v|
        ^startValue;
    }

    loop_ {
        var timeRangeValue = this.timeRangeValue;
        var numberOfSteps = (timeRangeValue / 0.01).asInteger;
        var count = 0;

        this.setLoopColour_;

        SystemClock.sched(startDelay, {
            this.synth.set(["arg",this.argNum-1].join,
                startValue - ( startValue * (count / numberOfSteps)));

            AppClock.sched(0, {
                this.dial.value = (startValue - ( startValue * (count /
                                                    numberOfSteps))).asInteger;
                nil
            });

            count = count + 1;
            if ( count > numberOfSteps, {
                this.stop;
                nil
            }, {
                0.01
            });
        });
    }

    go {
        startValue     = this.dial.value;
        timeRangeValue = settingsDials.duration.value;
        startDelay     = settingsDials.start.value;

        if ( startValue != 0, { this.loop_; }, { this.stop; });
    }

    asYaml { |pendId|
        ^format("pend%:\n"          ++
            "  type: FadeOut\n"     ++
            "  timeRangeValue: %\n" ++
            "  startValue: %\n"     ++
            "  startDelay: %\n"     ++
            "  argnum: %\n",
            pendId,
            timeRangeValue.asFloat,
            startValue.asInt,
            startDelay.asFloat,
            argNum);
    }

    startFromYamlLoad {
        this.loop_;
        ^this;
    }
}

RcPendulumFadeIn : RcPendulumBase {
    var <>timeRangeValue;
    var <>endValue;

    init { |midiBus,dal,settingDials|
        this.setPrepareColour_;
    }

    initFromYamlData { |yamlData|
        timeRangeValue = yamlData["timeRangeValue"].asFloat;
        endValue = yamlData["endValue"].asInteger;

        ^this;
    }

    initValue { |v|
        ^endValue;
    }

    loop_ {
        var timeRangeValue = this.timeRangeValue;
        var numberOfSteps = (timeRangeValue / 0.01).asInteger;
        var count = 0;

        // first rewind to zero in 1 second
        var steps = 1 / 0.01;
        var stepFactor = endValue / steps;

        this.setLoopColour_;

        SystemClock.sched(startDelay, {
            this.synth.set(["arg",this.argNum-1].join,
                endValue - (stepFactor * count));

            AppClock.sched(0, {
                this.dial.value = (endValue - (stepFactor * count)).asInteger;
                nil
            });

            count = count + 1;
            if ( count > steps, {
                count = 0;
                AppClock.sched(0, { this.dial.value = 0; nil; });

                this.synth.set(["arg",this.argNum-1].join, 0);

                SystemClock.sched(0.0, {
                    this.synth.set(["arg",this.argNum-1].join,
                        endValue * ( count / numberOfSteps));

                    AppClock.sched(0, {
                        this.dial.value = (endValue * ( count /
                                                     numberOfSteps)).asInteger;
                        nil
                    });

                    count = count + 1;
                    if ( count > numberOfSteps, {
                        this.stop(true);
                        nil
                    }, {
                        0.01
                    });
                });
            }, {
                0.01
            });
        });
    }

    go {
        endValue = this.dial.value;
        timeRangeValue = settingsDials.duration.value;
        startDelay = settingsDials.start.value;

        if ( endValue != 0, { this.loop_; }, { this.stop; });
    }

    asYaml { |pendId|
        ^format("pend%:\n"          ++
            "  type: FadeIn\n"      ++
            "  timeRangeValue: %\n" ++
            "  endValue: %\n"       ++
            "  startDelay: %\n"     ++
            "  argnum: %\n",
            pendId,
            timeRangeValue.asFloat,
            endValue.asInt,
            startDelay.asFloat,
            argNum);
    }

    startFromYamlLoad {
        this.loop_;
        ^this;
    }
}

RcPendulumFadeInOneTime : RcPendulumFadeIn {

    initValue { |v|
        ^0;
    }

    loop_ {
        var timeRangeValue = this.timeRangeValue;
        var numberOfSteps = (timeRangeValue / 0.01).asInteger;
        var count = 0;

        this.setLoopColour_;

        SystemClock.sched(startDelay, {
            SystemClock.sched(0.0, {
                this.synth.set(["arg",this.argNum-1].join,
                    endValue * ( count / numberOfSteps));

                AppClock.sched(0, {
                    this.dial.value = (endValue * ( count /
                        numberOfSteps)).asInteger;
                    nil
                });

                count = count + 1;
                if ( count > numberOfSteps, {
                    this.stop(true);
                    nil
                }, { 0.01 });
            });
            nil;
        });
    }

    asYaml { |pendId|
        ^format("pend%:\n"            ++
            "  type: FadeInOneTime\n" ++
            "  timeRangeValue: %\n  " ++
            "  endValue: %\n"         ++
            "  startDelay: %\n"       ++
            "  argnum: %\n",
            pendId,
            timeRangeValue.asFloat,
            endValue.asInt,
            startDelay.asFloat,
            argNum);
    }
}

RcPendulumTimer : RcPendulumBase {
    var <>startValue;
    var <>endValue;
    var <>values;
    var <>count;
    var <>stepFactor;
    var <>timeRangeValue;

    init { |midiBus,dal,settingDials|
        startValue    = dal.value;
        endValue      = nil;
        this.setPrepareColour_;
    }

    initFromYamlData { |yamlData|
        startValue     = yamlData["startValue"].asInteger;
        endValue       = yamlData["endValue"].asInteger;
        timeRangeValue = yamlData["timeRangeValue"].asInteger;

        ^this;
    }

    initValue { |v|
        ^endValue;
    }

    loop_ {
        this.setLoopColour_;

        SystemClock.sched(startDelay, {
            if ( this.stopNow, { nil }, {
                AppClock.sched(0, {
                    this.dial.value = this.startValue;
                    nil;
                });

                stepFactor = if ( startValue > endValue, {-1}, {1} );

                this.values = Interval.new( endValue, startValue,
                    0-stepFactor ).asArray ++ Interval.new( startValue,
                        endValue, stepFactor ).asArray;
                this.count = 0;
                this.step = timeRangeValue / this.values.size;

                SystemClock.sched(0.0, {
                    var val = this.values.wrapAt( this.count );
                    this.synth.set(["arg",this.argNum-1].join, val);

                    AppClock.sched(0, {
                        this.dial.value = val;
                        nil
                    });

                    this.count = this.count + 1;

                    // endDelay does not apply if the dial begins with the
                    // endValue - the timer starts and ends with the same value
                    if ( (val == this.endValue) && (this.count > 1), {
                        this.endDelay
                    }, {
                        this.step
                    });
                });
            });
            nil;
        });
    }

    go {
        endValue       = dial.value;
        timeRangeValue = settingsDials.duration.value;
        startDelay     = settingsDials.start.value;
        endDelay       = settingsDials.end.value;

        if ( endValue != startValue, { this.loop_; }, { this.stop; });
    }

    asYaml { |pendId|
        ^format("pend%:\n"          ++
            "  type: Timer\n"       ++
            "  startValue: %\n"     ++
            "  endValue: %\n"       ++
            "  timeRangeValue: %\n" ++
            "  startDelay: %\n"     ++
            "  endDelay: %\n"       ++
            "  argnum: %\n",
            pendId,
            startValue.asInteger,
            endValue.asInteger,
            timeRangeValue,
            startDelay.asFloat,
            endDelay.asFloat,
            argNum);
    }

    startFromYamlLoad {
        this.loop_;
        ^this;
    }
}

RcPendulumRecorder : RcPendulumBase {
    var <>values;
    var <>pos;

    init { |mBus, dal, settingDials|
        var thisObj, midiBusListener, midiBus;

        values     = List.new;
        pos        = 0;
        midiBus    = mBus;
        thisObj    = this;

        values.add([Date.localtime.rawSeconds, dal.value]);

        midiBusListener = { |tstamp, src, chan, num, val|
            if ( num == 18, {
                var last_value = thisObj.values[thisObj.values.size-1];
                last_value[0] = tstamp - last_value[0];

                midiBus.remove_listener(midiBusListener);

                thisObj.endDelay   = thisObj.settingsDials.end.value;
                thisObj.startDelay = thisObj.settingsDials.start.value;
                thisObj.go_for_it;
            });

            if ( (num > 0) && (num < 9), {
                var last_value = thisObj.values[thisObj.values.size-1];
                if ( last_value[1] != thisObj.dial.value ) {
                    last_value[0] = tstamp - last_value[0];
                    thisObj.values.add( [tstamp, thisObj.dial.value] );
                }
            });
        };
        midiBus.add_listener(midiBusListener);

        this.setPrepareColour_;
    }

    initFromYamlData { |yamlData|
        values = yamlData["values"].collect { |a|
            [ a[0].asFloat, a[1].asFloat ]
        };

        ^this;
    }

    initValue { |v|
        ^this.values[0][1];
    }

    loop_ {
        var valLen = this.values.size;
        this.pos = 0;

        this.setLoopColour_([
            Color(0.7,0.8,0.2),
            Color(),
            Color(0.85882352941176, 0.85882352941176, 0.85882352941176),
            Color() ]
        );

        SystemClock.sched( startDelay, {
            SystemClock.sched(0.0, {
                var val = this.values.wrapAt( this.pos )[1];

                if ( this.stopNow, { nil }, {
                    this.synth.set(["arg",this.argNum-1].join, val);

                    AppClock.sched(0, {
                        this.dial.value = val;
                        nil
                    });

                    this.pos = this.pos + 1;
                    if ( this.stopNow, { nil }, {
                        if ( ((this.pos % valLen) == 0) && (this.endDelay > 0),{
                            this.endDelay;
                        }, {
                            this.values.wrapAt( this.pos-1 )[0];
                        });
                    });
                });
            });
            nil
        });
    }

    go_for_it {
        // this ignores the 'go' call that is made when button pressed for
        // the second time, instead the midiBusListener calls this method
        // when the button is pressed for the second time.
        if ( this.values.size > 1, { this.loop_; }, { this.stop; } );
    }

    asYaml { |pendId|
        ^format("pend%:\n"        ++
            "  type: Recorder\n"  ++
            "  values: [%]\n"     ++
            "  startDelay: %\n"   ++
            "  endDelay: %\n"     ++
            "  argnum: %\n",
            pendId,
            values.collect { |a| a }.join(","),
            startDelay.asFloat,
            endDelay.asFloat,
            argNum);
    }

    startFromYamlLoad {
        this.go_for_it;
        ^this;
    }
}

RcPendulumRemove : RcPendulumBase {
    init { |mBus, dal, settingDials|
        this.setPrepareColour_;
    }

    isStopped {
        ^true;
    }

    go {
        pendulums[dial].do { |a| a.stop(true) };
        pendulums[dial] = [];
    }
}
