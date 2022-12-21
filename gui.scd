// Variables defined with builtin classes

var sampleHome       = "./samples";
var randomBlackBoard = Dictionary.new;
var synthLookup      = List.new;
var defaultSoundDev  = 1;
var rcRecorder       = nil;
var noteRemapPresets = Dictionary.new;
var optsDir          = Platform.userHomeDir ++ "/.sfts";
var presetsFile      = optsDir ++ "/presets.yaml";

var mainWindow       = Window(
    "Sound <---- Scape",
    bounds: Rect(0,0,1810,1150),
    resizable: false
).front;

var gui;
var midiBus;
var pendSettings;
var pendulumClaess;
var cfgMgr;

var allSamples = SortedList.new(8, { |a,b|
    a.comment.toLower < b.comment.toLower
});

// fix the plotter!
GUI.skin.plot.fontColor = Color.white;
GUI.skin.plot.gridColorX = Color.white;
GUI.skin.plot.gridColorY = Color.white;
GUI.skin.plot.labelX = "";
GUI.skin.plot.labelY = "";

gui            = RcGui.new;
midiBus        = RcMidiMessageBus.new;
pendSettings   = RcPendulumSettings.new(RcPendulumTimer, gui, midiBus);
pendulumClaess = RcPendulumBase.dropDownList;
cfgMgr         = RcConfig.new(optsDir, gui, s);

randomBlackBoard.add("currChannel" -> 0);

// Written for an Arturia MiniLab Mark 2 with a very specific midi controller
// mapping.
try {
    MIDIIn.connectAll;
    midiBus.connectTo(MIDIIn);
} {};

if ( File.exists( optsDir ).not, {
    File.mkdir( optsDir );
});

Platform.case(
    \osx, {
        Server.default.options.sampleRate = 44100;

        Server.default.options.inDevice_("Built-in Microph");
        Server.default.options.outDevice_("Built-in Output");

        ServerOptions.outDevices.do { |devname|
            if ((devname == "LE-Bose QC35 II") || (devname == "Bose QC35 II"),{
                Server.default.options.outDevice_(devname);
                Server.default.options.sampleRate = 48100;
            });
        };

        Server.default.options.maxLogins = 2;
        // this might not be needed for normal operations but when
        // using Bose headphones it's required. The bose headphones
        // seem to run on 48kHZ - don't know why.
    },

    \linux, {
        Server.default.options.sampleRate = 48000;
    }
);

Server.default.options.memSize = 48000;

s.reboot();

SystemClock.sched(1.0, {
    if ( s.serverBooting, {
        0.4
    }, {
        if ( s.hasBooted, {
            nil
        }, {
            s.boot;
            s.waitForBoot;
            if ( s.hasBooted, { nil }, { 0.5 });
        });
    });
});

~runOnAppClock = { |func| func.defer };

~doShellCmd = { |commandline, func|
    var piper = Pipe.new(commandline, "r");
    l = piper.getLine;
    while ( {l.notNil}, { func.value(l); l = piper.getLine; }, {});
    piper.close;
};

{
    var allFilenames = Dictionary.new;

    ~doShellCmd.value(["find '", sampleHome,
        "/' -type f \\( -name \\*.wav -o -name \\*.aiff \\) -print"].join(""),
        { |l|
            allFilenames[ l.basename ] = l;
        });

    // -MDItemUserTags is information on the tag given to the file in Finder
    // --> ignore samples that are tagged with a "Red" label.

    ~doShellCmd.value("exiftool -T -FileName -Comment -Duration -MDItemUserTags " +
        allFilenames.values.collect { |a| ["'",a,"'"].join("") }.join(" "),
        { |line|
            var cmt, bname, dur, labels, idx;
            var data = line.split($\t);

            format("%\r",line).post;

            bname  = data[0];
            cmt    = data[1];
            dur    = data[2];
            labels = data[3];

            if ( "Red".matchRegexp(labels).not, {
                var smpl = RcSample.new(allFilenames[bname], bname);
                smpl.comment = cmt;
                smpl.duration = dur;
                allSamples.add( smpl );
            });
        });
}.value();

~setDials = { |sample|
    if( sample.notNil, {
        sample.getn(0, RcSynthDef.argCount, { |ary|
            ~runOnAppClock.value({
                8.do { |idx| gui.knobs[idx].value = ary[idx]; };
            });
        });
    }, { 8.do { |idx| gui.knobs[idx].value = 0; }; } );
};

~playSample = { |num,vel,initVals|
    var note = gui.obtainNote;
    if (note.notNil, {
        var sampleIdx = num.abs % allSamples.size;
        var sample    = allSamples[sampleIdx];
        var filename  = sample.filename;
        var comment   = sample.comment;
        var synthType = gui.synthType;

        var action = { |buffer|
            var initArgs  = [
                "num",       num,
                "vel",       vel,
                "bufnum",    buffer.bufnum,
                "sampleIdx", sampleIdx,
                "arg0",      vel,
            ] ++ RcSynthDef.addInitVals(initVals);

            note.synth     = Synth.new(synthLookup[synthType].first, initArgs);
            note.filename  = filename;
            note.synthType = synthLookup[synthType][1];
            note.buffer    = buffer;

            ~runOnAppClock.value({
                ~setDials.value(note.synth);
                gui.noteOn(note, num, vel, sample.basename +
                    ["(",sample.comment,", ",sample.duration,")"].join +
                    note.synthType);
            });
        };

        Buffer.read(s, filename, action: action);
    });
    note;
};

~playSinOsc = { |num,vel,initVals|
    var note = gui.obtainNote;
    if (note.notNil, {
        var initArgs = [
            "num",       num,
            "vel",       vel,
            "bufnum",    nil,
            "sampleIdx", nil,
            "arg0",      vel,
        ] ++ RcSynthDef.addInitVals(initVals);

        note.synth     = Synth.new(synthLookup[gui.synthType].first, initArgs);
        note.synthType = synthLookup[gui.synthType][1];

        ~runOnAppClock.value({
            ~setDials.value(note.synth);
            gui.noteOn(note, num, vel, ["Freq: ", num.freq].join +
                note.synthType);
        });
    });
    note;
};

// this is the lookup for the different types of synth's
synthLookup     = RcSynthDef.new(~playSinOsc, ~playSample).synthLookup;
gui.synthLookup = synthLookup;
gui.allSamples  = allSamples;

~controlPads = { |src, chan, num, val|
    if ( (num > 19) && (num < 36), {
        var idx,ref;
        if ( val == 0, {
            { gui.releasePadByMidiControlNum(num); }.defer;
        }, {
            // freeze and keep.
            // first ensure that the pad isn't already taken, then find the
            // index of an sample that is playing via a key from the keyboard.
            if ( gui.padTaken(num).not, {
                var note = gui.playingNote;

                if ( note.notNil, {
                    var pad;
                    note.synth.set(\padNr, num);
                    note.synth.getn(0, RcSynthDef.argCount, { |ary|
                        ~runOnAppClock.value({
                            pad = gui.setupPad(num,ary,note);
                            gui.setActivePad(pad);
                            note.hide();
                        });
                    });
                });
            });
        });
    });
};

//
// The following is all related to the midi controller, it specific to
// Arturia Minilab MK-II and assumes a very specific layout, so mostly
// ignorable. It also extremely badly done since key numbers are just
// hardcoded into the code - special constants without meaning.
// Assumption was that I would code this for personal use only :(
//
~noteOn = { |src, chan, num, vel|
    if ( randomBlackBoard["shiftpressed"], {
        if ( gui.currentPad.notNil, {
            noteRemapPresets.add( [randomBlackBoard["currChannel"],num] -> RcPresets.new(
                num,
                randomBlackBoard["currChannel"],
                gui.currentPad
            ));
        }, {
            noteRemapPresets.removeAt( [randomBlackBoard["currChannel"],num] )
        });

        ~runOnAppClock.value({
            gui.updateMappingTable( noteRemapPresets,
                randomBlackBoard["currChannel"] );
        });
    }, {
        var presetsKey = [randomBlackBoard["currChannel"],num];

        if ( noteRemapPresets[presetsKey].isNil, {
            synthLookup[gui.synthType][2].value(num,vel,nil);
        }, {
            var preset = noteRemapPresets[presetsKey];

            if ( (preset.filename == "") || preset.filename.isNil, {
                var note = synthLookup[gui.synthType][2].value(
                    preset.destNoteNum,
                    vel,
                    preset.dialValues);
                note.presetData = noteRemapPresets[presetsKey];
            }, {
                var note = synthLookup[gui.synthType][2].value(
                    allSamples.indexOfEqual(RcSample.new(preset.filename,"")),
                    vel,
                    preset.dialValues
                );
                note.presetData = noteRemapPresets[presetsKey];
            });
        });
    });
};

~noteOff = { |src,chan,num,vel|
    ~runOnAppClock.value({
        var presetsKey = [randomBlackBoard["currChannel"],num];
        var preset = noteRemapPresets[presetsKey];

        if ( preset.isNil, {
            gui.noteOff( num, vel );
        }, {
            if ( (preset.filename == "") || preset.filename.isNil, {
                gui.noteOff( preset.destNoteNum, vel);
            }, {
                gui.noteOff(
                    allSamples.indexOfEqual(RcSample.new(preset.filename,"")),
                    vel);
            });
        });
    });
};

~controlSetSynthValue = { |src, chan, num, val|
    if ( (num < 9) && (val != 64), {
        var synth = gui.currentSynth;

        var newval = (gui.currentDial(num-1).value + (val - 64)).asInteger.clampTo(0,127);

        if ( synth.notNil, { synth.set(["arg",num-1].join, newval); });

        ~runOnAppClock.value({
            gui.knobs[num-1].value = newval;
            gui.dialChanged(num-1, newval);
        });

    })
};

~midiStartRecord = { |src, chan, num, val|
    if ( num == 19, { rcRecorder.toggle });
};

~midiSelectActivePad = { |src, chan, num, val|
    if ( num == 10, {
        var cP = [0,15,\lin].asSpec.map(
            [0,127,\lin].asSpec.unmap(val)
        ).asInteger;

        ~runOnAppClock.value({
            if ( if (gui.pads[cP].notNil, {gui.pads[cP].isDefined}, {false}), {
                gui.setActivePad( gui.pads[cP] );
            });
        });
    });
};

~midiSampleSelectorPressed = { |src, chan, num, val|
    if ( num == 17, {
        ~runOnAppClock.value({
            gui.sampleSelector.valueAction_(gui.sampleSelector.value);
        });
    });
};

~midiSampleSelector = { |src, chan, num, val|
    if ( (num == 9) && (val != 64), {
        gui.bb["sampleSelectorCounter"].willSomethingChange(val, { |val|
            RcHelpers.drpdwnSetValue(val,gui.sampleSelector,false);
        });
    });
};

~midiPendulumSelectorPressed = { |src, chan, num, val|
    if ( num == 18, {
        if ( pendSettings.isLoaded, {
            pendSettings.go;
        }, {
            if ( gui.lastDialedDial.notNil && gui.currentSynth.notNil, {
                gui.pushPendulum(pendSettings.newPendulum());
            });
        });
    });
};

~midiPendulumSelector = { |src, chan, num, val|
    if ( (num == 11) && (val != 64), {
        gui.bb["pendulumSelectorCounter"].willSomethingChange(val, { |val|
            RcHelpers.drpdwnSetValue(val, gui.pendulumSelection);
        });
    });
};

~midiPendulumSettings = { |src, chan, num, val|
    if ( val != 64, {
        switch( num,
            12, { pendSettings.incrementDial( \end,      val - 64 ) },
            13, { pendSettings.incrementDial( \duration, val - 64 ) },
            16, { pendSettings.incrementDial( \start,    val - 64 ) }
        );
    });
};


~midiGlobalVolume = { |src, chan, num, val|
    if ( (num == 15) && (val != 64), {
        ~runOnAppClock.value({
            var betweenZeroAndOne = [s.volume.min,
                s.volume.max, \lin].asSpec.unmap(s.volume.volume);

            betweenZeroAndOne = betweenZeroAndOne + ( (val-64) / 127);

            s.volume.volume = [s.volume.min, s.volume.max, \lin].asSpec.map(betweenZeroAndOne);
        });
    });
};

~midiSynthSelector = { |src, chan, num, val|
    if ( (num == 14) && (val != 64), {
        gui.bb["synthSelectorCounter"].willSomethingChange(val, { |val|
            RcHelpers.drpdwnSetValue(val, gui.synthSelection);
        });
    });
};

MIDIIn.addFuncTo(\noteOff, ~noteOff);
MIDIIn.addFuncTo(\noteOn,  ~noteOn);

MIDIIn.addFuncTo(\control, ~controlSetSynthValue);
MIDIIn.addFuncTo(\control, ~midiStartRecord);
MIDIIn.addFuncTo(\control, ~midiSelectActivePad);
MIDIIn.addFuncTo(\control, ~controlPads);

MIDIIn.addFuncTo(\control, ~midiSampleSelectorPressed);
MIDIIn.addFuncTo(\control, ~midiSampleSelector);
MIDIIn.addFuncTo(\control, ~midiPendulumSelectorPressed);
MIDIIn.addFuncTo(\control, ~midiPendulumSettings);
MIDIIn.addFuncTo(\control, ~midiPendulumSelector);

MIDIIn.addFuncTo(\control, ~midiSynthSelector);
MIDIIn.addFuncTo(\control, ~midiGlobalVolume);

MIDIIn.addFuncTo(\control, { |src, chan, num, val|
    if ( num == 64, {
        // this sets the preset channel - in this case 32 presets channels,
        // each with 9 notes.
        ~runOnAppClock.value({
            randomBlackBoard.add("currChannel" -> [0,31,\lin].asSpec.map(
                [0,127,\lin].asSpec.unmap(val)).asInteger);
            gui.updateMappingTable( noteRemapPresets,
                randomBlackBoard["currChannel"]);
        });
    });
});

randomBlackBoard.add("shiftpressed" -> false); // midi shift button
MIDIIn.addFuncTo(\sysex, { |src, chan, num, vel|
    // is the shift button being pressed?
    if ( (chan[9] == 46), {
        randomBlackBoard["shiftpressed"] = (chan[10] != 0);
    });

    // this is the pad button and shift are both pressed
    if ( (chan[10] == 127) && (chan[9] == 98), {
        RcPresets.overwriteDefault(
            noteRemapPresets,
            randomBlackBoard["currChannel"]
        );
    });

    // this is the shift button and either Oct+ or Oct- button being
    // pressed together
    if ( (chan[10] == 127) && ( (chan[9] == 96) || (chan[9] == 97) ), {
        cfgMgr.saveWithoutDialog;
    });
});

//
// Debug functionality ...
//
//RcDebug.new(MIDIIn);

~queryNotesOn = {
    gui.notes.select { |n| n.noteNum.notNil }.collect { |l| l.noteNum };
};


//
// GUI Definition
//

~addSample = { |path|
    if ( allSamples.collect { |a| a.filename }.indexOfEqual(path).isNil, {
        ~doShellCmd.value("exiftool -T -FileName -Comment -Duration -MDItemUserTags " + ["'",path,"'"].join(""), { |line|
            var cmt, bname, dur, labels, idx;
            var data = line.split($\t);

            bname  = data[0];
            cmt    = data[1];
            dur    = data[2];
            labels = data[3];

            if ( dur != "-", {
                var smpl = RcSample.new( path, bname );
                smpl.comment = cmt;
                smpl.duration = dur;
                allSamples.add( smpl );

                // allSamples = allSamples.asSortedList( { |a,b|
                //     a.comment < b.comment
                // }).asArray.asList;

                gui.sampleSelector.items = [["---"]].add(allSamples.collect { |a|
                    [a.basename, a.comment, a.duration].join(", ")
                }).flatten;
            });
        });
    });
};

cfgMgr.addSample = ~addSample;

// Meters and volume control.
{
    var viewSteth = HLayoutView(mainWindow, Rect(1390, 10, 300, 300));
    var steh = Stethoscope.new(s, view: viewSteth, bufnum: 0);
    var volView = HLayoutView(mainWindow, Rect(1650, 70, 400, 240));

    var meterView = ServerMeterView.new(s, mainWindow, 1250@10, 2, 2);


    meterView.view.fixedHeight = 500;

    s.volume.gui(volView);
}.value();

// store and save setup
{
    cfgMgr.setupGui( mainWindow );
}.value();

// buttons and samples being played
{
    var view, exitButton, outDevSelector, inDevSelector, sampleRateButton;

    rcRecorder = RcRecorder.new(s,
        Button(mainWindow, Rect(1650, 10, 120, 25)),
        ~addSample,
        cfgMgr
    );

    exitButton = RcHelpers.buttonBW("Quit",mainWindow,Rect(1650,40,120,25),{
        RcHelpers.confirmExit( {
            rcRecorder.quitting;
            try { gui.padCtrl.allOff; } {};
            SystemClock.sched(0.2, {
                s.quit( { 0.exit; } );
            });
        });
    });

    Platform.case( \osx, {
        outDevSelector = PopUpMenu(mainWindow, Rect(780, 10, 150, 25));
        outDevSelector.items = ServerOptions.outDevices;
        outDevSelector.value = Dictionary.newFrom(ServerOptions.outDevices.
            collect { |a,i| [a,i] }.flatten)[s.options.outDevice];
        outDevSelector.action = { |button|
            s.options.outDevice = ServerOptions.outDevices[button.value];
            s.reboot();
            s.waitForBoot();
        };

        inDevSelector = PopUpMenu(mainWindow, Rect(940, 10, 150, 25));
        inDevSelector.items = ServerOptions.inDevices;
        inDevSelector.value = Dictionary.newFrom(ServerOptions.inDevices.
            collect { |a,i| [a,i] }.flatten)["Built-in Microph"];
        inDevSelector.action = { |button|
            s.options.inDevice = ServerOptions.inDevices[button.value];
            s.reboot();
            s.waitForBoot();
        };

        sampleRateButton = Button(mainWindow, Rect(1100, 10, 120, 25));
        sampleRateButton.states = [["Sample Rate", Color.black, Color.white]];
        sampleRateButton.action = { "open -a 'Audio MIDI Setup'".unixCmd; };
    });

    view = FlowView(mainWindow, Rect(360, 10, 420, 30));

    RcHelpers.buttonBW("Load Sample", view, 120@25, {
        FileDialog({ |paths|
            paths.do { |path| ~addSample.value(path); }
        }, {}, fileMode: 3, stripResult: false);
    });

    RcHelpers.buttonBW("Server Window", view, 120@25, {
        s.makeWindow;
    });

    Platform.case( \osx, {
        RcHelpers.buttonBW("Samples --> Audacity", view, 150@25, {
            gui.pads.collect { |a| a.filename }.reject { |a| a.isNil }.asSet.do { |a|
                format( "open -a Audacity \"%\"", a ).unixCmd
            };
        });
    });

    RcClock.new( mainWindow );

    view = FlowView(mainWindow, Rect(777, 40, 450, 67), margin: 3@5);
    view.background = Color.grey;

    // title for the config
    gui.titleField = EZText( view, 440@50, "Scape Title",
        initVal: nil, labelWidth: 40, layout: \vert
    )
    .setColors(Color.white, Color.black,
        textBackground: Color.white,
        stringBackground: Color.grey,
        stringColor: Color.white
    ).enabled_(true).visible_(true);

    // sample selector and synth type selector
    view = FlowView(mainWindow, Rect(777, 112, 450, 67), margin: 3@5);
    view.background = Color.grey;

    RcHelpers.addStaticText(view, "Sample and Synth Selectors");

    gui.synthSelection             = PopUpMenu(view, 150@25);
    gui.synthSelection.items       = synthLookup.collect { |a| a[1] };
    gui.synthSelection.value       = 0;
    gui.synthType                  = 0;
    gui.synthSelection.action      = { |button|
        gui.synthType = button.value.asInteger;
    };
    gui.bb["synthSelectorCounter"] = RcGuiCounter.new(0,3);

    view.startRow;
    gui.sampleSelector = PopUpMenu(view, 440@25);
    gui.sampleSelector.items = [["---"]].add(allSamples.collect { |a|
        [a.basename, a.comment, a.duration].join(", ")
    }).flatten;
    gui.sampleSelector.action = { |item|
        if ( item.value > 0, {
            ~noteOn.value(nil,nil,item.value-1, 0);
        }, {
            ~noteOn.value(nil,nil, gui.sampleSelector.items.size.rand, 0);
        });
    };
    gui.bb["sampleSelectorCounter"] = RcGuiCounter.new(0,2);

    // pendulum settings
    view = FlowView(mainWindow, Rect(10, 112, 750, 67), margin: 3@3);
    view.background = Color.grey;

    RcHelpers.addStaticText(view,"Pendulum selector and settings");

    view.startRow;
    gui.pendulumSelection = PopUpMenu(view, 180@24);
    gui.pendulumSelection.items = pendulumClaess.collect { |a| a[0] };
    gui.pendulumSelection.action = { |button|
        pendSettings.klzPendulum = pendulumClaess[button.value][1];
    };
    gui.bb["pendulumSelectorCounter"] = RcGuiCounter.new(0,5);

    {
        var psBut = { |name, initVal=0, minValue=0|
            EZKnob(view, 180@24, name,
                ControlSpec.new(minval: minValue, maxval: 59,
                    warp: 'lin', step: 0.1, default: initVal, units: "secs"),
                unitWidth: 0, initVal: initVal, layout:\horz, labelWidth: 80)
            .setColors(Color.grey,Color.white)
            .font_(Font("Helvetica",11));
        };

        pendSettings.dials.start    = psBut.value( "Start", 0 );
        pendSettings.dials.duration = psBut.value( "Duration", 1, 0.1 );
        pendSettings.dials.end      = psBut.value( "End", 0 );
    }.value();

    // dial settings
    view = FlowView(mainWindow, Rect(10, 50, 750, 60));

    8.do { |idx|
        var func = { |knob|
            var synth = gui.currentSynth;
            if ( synth.notNil, { synth.set(["arg",idx].join, knob.value) });
            gui.dialChanged(idx, knob.value);
        };

        var knob = EZKnob(view, 180@24, gui.knobNameLookup[idx],
            ControlSpec.new(minval: 0, maxval: 127,
                warp: 'lin', step: 1.0, default: 0, units: nil),
            unitWidth:0, initVal: 0, layout:\horz, labelWidth: 80, action: func)
        .setColors(Color.grey,Color.white)
        .font_(Font("Helvetica",11));

        gui.knobs.add(knob);
    };
}.value();

// list of frozen samples
{
    var view2 = FlowView(mainWindow, Rect(10, 230, 1240, 680));
    // view2.background = Color.grey;

    // this defines the 16 pads, 4 rows of 4 pads.
    4.do { |idx|
        view2.startRow;

        // add a gap between the top 2 rows and the bottom 2 rows.
        if ( idx == 2, {

            // midi linking controls
            var view = nil;

            FlowView( view2, Rect(10,10, 580,30));
            view = FlowView(view2, Rect(10, 200, 300, 30), margin: 3@3);

            CheckBox.new( view, 15@25, "").action = { |cbx|
                gui.pads.do { |paddef|
                    RcHelpers.setValueIfEnabled(paddef.linkMidi,cbx);
                };
            };
            CheckBox.new( view, 15@25, "").action = { |cbx|
                gui.pads.do { |paddef|
                    RcHelpers.toggleValueIfEnabled(paddef.linkMidi);
                };
            };

            view2.startRow;
        });

        4.do { |ridx|
            var view = FlowView(view2, Rect(100, 100, 300, 150));

            var txt = EZText(view, 292@28, nil, initVal: nil,
                labelWidth: 0, layout: \horz, margin: 2@3).
            setColors(Color.grey, Color.white, textBackground: Color.white);

            var paddef = RcPadDef.new(view, txt, idx, ridx);

            // view.background = Color.green;
            view.startRow;

            view.mouseDownAction = { |view,x,y,modifier,button,clickcnt|
                if ( button == 0, { // left mouse
                    if ( paddef.isDefined, {
                        gui.setActivePad(paddef);
                    })
                });

                if ( button == 1, { // right mouse
                    if ( paddef.isDefined, {
                        ~runOnAppClock.value({
                            gui.padCtrl.padOff(paddef.padIdx);
                            gui.releasePad(paddef);
                        });
                    })
                });
            };

            txt.textField.enabled = false;
            gui.pads.add(paddef);

            // this defines the eight dials, 2 rows of 4.
            2.do { |kridx|
                view.startRow;

                4.do { |kcidx|
                    var knob_action = { |knob|
                        var argidx = (kridx * 4) + kcidx;
                        if ( paddef.synth.notNil, {
                            paddef.synth.set(["arg",argidx].join, knob.value)
                        });
                    };

                    var knob = EZKnob(view, 28@24,nil,
                        ControlSpec.new(minval: 0, maxval: 127,
                            warp: 'lin', step: 1.0, default: 0, units: nil),
                        unitWidth: 0, initVal: 0, layout:\horz, labelWidth: 80,
                        action: knob_action);
                    knob.numberView.visible = false;
                    knob.enabled = true;
                    paddef.knobs.add(knob);
                };

                // first row or second row? second row has checkbox and first
                // row just synth drop down
                if ( kridx == 0,
                    {
                        paddef.typeText = PopUpMenu(view, 150@20);
                        paddef.typeText.items = synthLookup.collect {|a| a[1] };
                        paddef.typeText.value = 0;
                        paddef.typeText.action = { |new_value|
                            var synthDef = synthLookup.reject { |a|
                                a[1] != new_value.items[new_value.value]
                            }.first;

                            paddef.synth.getn(0, RcSynthDef.argCount, { |args|
                                paddef.synthUpdate( Synth.replace(
                                    paddef.synth,
                                    synthDef.first,
                                    RcSynthDef.argsToArgs(args),
                                    sameID: true)
                                );
                            });
                        }
                    },
                    {
                        paddef.linkMidi = CheckBox.new( view, 20@20, "");
                        paddef.linkMidi.value = false;
                        paddef.linkMidi.enabled = false;
                        paddef.linkMidi.action = { |checkbox|
                            if ( checkbox.value, {
                                checkbox.setBgColor(Color.red);
                                midiBus.add_listener(paddef.midiBusListener);
                            }, {
                                checkbox.setBgColor(Color.white);
                                midiBus.remove_listener(paddef.midiBusListener);
                            });
                        };

                        paddef.fileText = EZText(view, 130@20, nil,
                            initVal: nil,
                            labelWidth: 0,
                            layout: \horz).
                        setColors(Color.grey, Color.white,
                            textBackground: Color.white);
                        paddef.fileText.textField.enabled = false;
                    }
                );
            };

            // add ranger
            paddef.ranger = Slider.new(view, 294@10);
            paddef.ranger.thumbSize = 10;
            paddef.ranger.orientation = \horizontal;
            paddef.ranger.knobColor = Color.new(red: 0, green: 191/255,
                blue: 0.1, alpha: 0.8);

            paddef.midiPadNum = gui.padIdxToMidi( paddef.padIdx );

            // add plotter for buffer data
            {
                var compView = CompositeView(view,
                    Rect( 0,0,294,40)
                ).background_(Color.rand(0.7)).resize_(50);

                paddef.plotter = Plotter(format("p.%.%",idx,ridx),
                    parent: compView);
                paddef.plotter.value_([0,0,0,0,0]);
                paddef.plotter.superpose = true;
            }.value();

            OSCFunc.new( { |msg|
                if ( msg[3].asInteger == paddef.midiPadNum, {
                    paddef.samplePos = msg[4];
                    { paddef.ranger.value = msg[4]; }.defer;
                });
            }, '/pos');
        };
        {
            var view3 = FlowView(view2, 20@150);

            13.do { view3.startRow };

            CheckBox.new( view3, 15@15, "").action = { |cbx|
                Range( (idx*4), 4 ).do { |padidx|
                    RcHelpers.setValueIfEnabled(gui.pads[padidx].linkMidi,cbx);
                };
            };

            view3.startRow;

            CheckBox.new( view3, 15@15, "").action = { |cbx|
                Range( (idx*4), 4 ).do { |padidx|
                    RcHelpers.toggleValueIfEnabled(gui.pads[padidx].linkMidi);
                };
            };
        }.value();
    };
    view2.startRow;

    // link midi dials checkboxs by columns
    4.do { |cidx|
        var v = FlowView.new( view2, 300@20);

        FlowView.new( v, 120@20 );

        CheckBox.new( v, 15@15, "").action = { |cbx|
            Interval(cidx, 15, 4).do { |padidx|
                RcHelpers.setValueIfEnabled(gui.pads[padidx].linkMidi,cbx);
            };
        };
        CheckBox.new( v, 15@15, "").action = { |cbx|
            Interval(cidx, 15, 4).do { |padidx|
                RcHelpers.toggleValueIfEnabled(gui.pads[padidx].linkMidi);
            };
        };
    };
}.value();

// setup presets info
{
    var view = FlowView(mainWindow, Rect(1250, 340, 530, 300));

    view.background = Color.grey;

    gui.chanValue = StaticText.new(mainWindow, Rect(1250, 310, 250, 30));
    gui.chanValue.string = "Current Presets Channel: -";

    ~whiteKey = { |num, view|
        var txt = EZText(
            view, 510@20, num, initVal: nil, labelWidth: 40, layout: \horz
        )
        .setColors(Color.white, Color.black, textBackground: Color.white)
        .enabled_(true)
        .visible_(true);

        txt.labelView.mouseDownAction = { |view,x,y,modifier,button,clickcnt|
            if ( button == 0, { // left button
                ~noteOn.value(nil,nil,num, 0);
            });
        };

        [num, txt].asAssociations[0];
    };

    ~blackKey = { |num, view|
        var txt = EZText(
            view, 510@20, num, initVal: nil, labelWidth: 40, layout: \horz
        )
        .setColors(Color.black, Color.white, textBackground: Color.white)
        .enabled_(true)
        .visible_(true);

        txt.labelView.mouseDownAction = { |view,x,y,modifier,button,clickcnt|
            if ( button == 0, { // left button
                ~noteOn.value(nil,nil,num, 0);
            });
        };

        [num, txt].asAssociations[0];
    };

    StaticText.new(view, 100@10 );
    gui.mappingTable = Dictionary.new;

    gui.mappingTable.add(~whiteKey.value(72,view));
    gui.mappingTable.add(~whiteKey.value(71,view));
    gui.mappingTable.add(~blackKey.value(70,view));
    gui.mappingTable.add(~whiteKey.value(69,view));

    StaticText.new(view, 100@60 );

    gui.mappingTable.add(~whiteKey.value(52,view));
    gui.mappingTable.add(~blackKey.value(51,view));
    gui.mappingTable.add(~whiteKey.value(50,view));
    gui.mappingTable.add(~blackKey.value(49,view));
    gui.mappingTable.add(~whiteKey.value(48,view));

    RcPresets.setupGui( mainWindow, noteRemapPresets, allSamples,
        ~addSample, presetsFile, randomBlackBoard);
}.value();

// list of notes being played
{
    var view = FlowView(mainWindow, Rect(1250, 690, 530, 410));
    view.background = Color.grey;
    StaticText.new(view, 100@10 );

    16.do {
        var notedef;
        var txt = EZText(view, 510@20, "Key", initVal: nil,
            labelWidth: 120, layout: \horz)
        .setColors(Color.black, Color.white, textBackground: Color.white)
        .enabled_(true)
        .visible_(false);

        notedef = RcNoteDef.new(txt);
        gui.notes.add(notedef);

        txt.labelView.mouseDownAction = { |view,x,y,modifier,button,clickcnt|
            if ( button == 1, { // right mouse
                ~runOnAppClock.value({gui.noteOff(view.string.asInteger,0)});
            });

            if ( button == 0, { // left mouse
                var padNr = (20..35).select { |idx2|
                    gui.padTaken(idx2).not
                }.scramble.first;

                if ( padNr.notNil, {
                    notedef.synth.set(\padNr, padNr);
                    notedef.synth.getn(0, RcSynthDef.argCount, { |ary|
                        var pad;
                        ~runOnAppClock.value({
                            pad = gui.setupPad(padNr,ary,notedef);
                            gui.padCtrl.padOn(pad.padIdx);
                            gui.setActivePad(pad);
                            notedef.hide();
                        });
                    });
                });
            });
        };
    };
}.value();

// checkbox to send OSC commands to some host/port for further usage.
// Done of VJ software that requires some input from the scape.
// Dial values for all samples is set to the server and also the
// location in the sample.
{
    var cb = CheckBox.new( mainWindow, Rect(1250, 1110, 180, 25),
        "Send OSC to: ");
    var textField = EZText(mainWindow, Rect(1380, 1110, 210, 25), nil,
        initVal: "192.168.178.53:12345",
        labelWidth: 0, layout: \horz, margin: 2@3).
    setColors(Color.black, Color.white, textBackground: Color.white);

    cb.value = false;
    cb.action = { |checkbox|
        var hostPort = textField.value.split($:);

        if ( checkbox.value, {
            var darkPlace = NetAddr(hostPort[0], hostPort[1].asInteger);

            rcRecorder.oscEndpoint = darkPlace;
            checkbox.setBgColor(Color.red);

            SystemClock.sched(0.05, {
                try { gui.asOSC(darkPlace); } {};
                if ( rcRecorder.oscEndpoint.notNil, 0.05, nil );
            });
        }, {
            checkbox.setBgColor(Color.white);
            rcRecorder.oscEndpoint = nil;
        });
    };
}.value();

// load presets if they exist.
if ( File.exists( presetsFile ), {
    ~runOnAppClock.value({
        randomBlackBoard["currChannel"] = RcPresets.loadFromPath(
            presetsFile,
            noteRemapPresets,
            allSamples,
            ~addSample
        );

        gui.updateMappingTable(
            noteRemapPresets,
            randomBlackBoard["currChannel"]
        );
    });
});

// setup the midi out so that the pad lights can be be turned off.
{
    // assigning a variable if the midi controller is not connected
    // makes the gui.padCtrl assignment work - don't ask why, it just
    // does. (in the RcPadCtrl.new call there is a try block that catches
    // an exception and that exception - even though it's in a try block -
    // causes the instance variable assignment to fail).
    var tmp = RcPadCtrl.new;
    gui.padCtrl = tmp;

    SystemClock.sched( 1.0, {
        gui.padCtrl.allOff;
    });
}.value();
