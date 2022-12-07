// Variables defined with builtin classes

var sampleHome       = "./samples";
var randomBlackBoard = Dictionary.new;
var synthLookup      = List.new;
var defaultSoundDev  = 1;
var rcRecorder       = nil;
var noteRemapPresets = Dictionary.new;
var optsDir          = Platform.userHomeDir ++ "/.sfts";
var presetsFile      = optsDir ++ "/presets.yaml";

var mainWindow       = Window("Sound <---- Scape",
    bounds: Rect(0,0,1810,1150),
    resizable: false).front;

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

// Variables defined by home-made classes. This is intended so that one
// day when there is a way to compile and include class files, I won't need
// the Makefile to copy the classes/*.sc files to the Extensions folder.
//    does not work --> thisProcess.interpreter.compileFile("%/classes/rc_config.sc".format(thisProcess.nowExecutingPath.dirname)).();
//    does not work --> "%/classes/rc_config.sc".format(thisProcess.nowExecutingPath.dirname).importPaths
//    does not work --> "./classes/rc_config.sc"..relativePath
// there simply does not seem to be a way to compile and import class Files.
gui              = RcGui.new;
midiBus          = RcMidiMessageBus.new;
pendSettings     = RcPendulumSettings.new(RcPendulumTimer);
pendulumClaess   = RcPendulumBase.dropDownList;
cfgMgr           = RcConfig.new(optsDir, gui, s);

randomBlackBoard.add("currChannel" -> 0);


// Written for an Arturia MiniLab Mark 2 with a very specific midi controller
// mapping.
try {
    MIDIIn.connectAll;
} {};

if ( File.exists( optsDir ).not, { File.mkdir( optsDir )});

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

~runOnAppClock = { |func| AppClock.sched(0, { func.value(); nil }); };

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
                (0..7).do { |idx| gui.knobs[idx].value = ary[idx]; };
            });
        });
    }, { (0..7).do { |idx| gui.knobs[idx].value = 0; }; });
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
            var initArgs  = List.newUsing([
                "num",       num,
                "vel",       vel,
                "bufnum",    buffer.bufnum,
                "sampleIdx", sampleIdx,
                "arg0",      vel,
            ]);

            if ( if ( initVals.notNil, { initVals.notEmpty }, { false } ), {
                7.do { |idx|
                    initArgs.add("arg" ++ (idx+1)).add( initVals[idx+1] );
                };
            }, {
                initArgs.add( "arg1" ).add( 64 );
            });

            note.synth     = Synth.new(synthLookup[synthType].first,
                                       initArgs.asArray);
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
        var initArgs = List.newUsing([
            "num",    num,
            "vel",    vel,
            "bufnum", nil,
            "arg0",   vel,
        ]);

        if ( if ( initVals.notNil, { initVals.notEmpty }, { false } ), {
            7.do {|idx| initArgs.add("arg" ++ (idx+1)).add( initVals[idx+1] );};
        }, {
            initArgs.add( "arg1" ).add( 64 );
        });

        note.synth = Synth.new(synthLookup[gui.synthType].first,
            initArgs.asArray);

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
            ~runOnAppClock.value({
                gui.releasePadByMidiControlNum(num);
            });
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

        var newval = gui.currentDial(num-1).value;

        newval = newval + (val - 64);

        if ( newval < 0, { newval = 0 });
        if ( newval > 127, { newval = 127 });

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

~midiOneButtonPressed = { |src, chan, num, val|
    if ( num == 17, {
        ~runOnAppClock.value({
            gui.sampleSelector.valueAction_(gui.sampleSelector.value);
        });
    });
};

~midiOneButtonDialed = { |src, chan, num, val|
    if ( (num == 9) && (val != 64), {
        ~runOnAppClock.value({
            if ( val > 64, {
                gui.sampleSelector.value = (gui.sampleSelector.value + 1).min(gui.sampleSelector.items.size-1);
            }, {
                gui.sampleSelector.value = 0.max(gui.sampleSelector.value - 1);
            });
        });
    });
};

~midiNineButtonPressed = { |src, chan, num, val|
    if ( num == 18, {
        if ( pendSettings.isLoaded, {
            pendSettings.go;
        }, {
            if ( gui.lastDialedDial.notNil && gui.currentSynth.notNil, {
                gui.pushPendulum(pendSettings.newPendulum(
                    midiBus,
                    gui.lastDialedDial,
                    gui.currentSynth,
                    gui.currentPad.knobIdx(gui.lastDialedDial) + 1
                ));
            });
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

~midiTenButtonDialed = { |src, chan, num, val|
    if ( (num == 14) && (val != 64), {
        ~runOnAppClock.value({
            if (  val > 64, {
                if ( gui.synthSelection.value != (gui.synthSelection.items.size-1), {
                    gui.synthSelection.value = (gui.synthSelection.value + 1).min(gui.synthSelection.items.size-1);
                });
            }, {
                if ( gui.synthSelection.value != 0, {
                    gui.synthSelection.value = 0.max(gui.synthSelection.value - 1);
                });
            });
            gui.synthSelection.valueAction_(gui.synthSelection.value);
        });
    });
};

~midiBusFiller = { |src, chan, num, val|
    midiBus.push( [src, chan, num, val] )
};


// this is the pendulum type dialer
midiBus.add_listener( { |tstamp, src, chan, num, val|
    if ( (num == 11) && (val != 64), {
        ~runOnAppClock.value({
            if (  val > 64, {
                if ( gui.pendulumSelection.value == (gui.pendulumSelection.items.size-1),
                    {
                        gui.pendulumSelection.value = 0;
                    }, {
                        gui.pendulumSelection.value = (gui.pendulumSelection.value + 1).min(gui.pendulumSelection.items.size-1);
                    });
            }, {
                if ( gui.pendulumSelection.value == 0, {
                    gui.pendulumSelection.value = gui.pendulumSelection.items.size-1;
                },{
                    gui.pendulumSelection.value = 0.max(gui.pendulumSelection.value - 1);
                });
            });
            gui.pendulumSelection.valueAction_(gui.pendulumSelection.value);
        });
    });
});

MIDIIn.addFuncTo(\noteOff, ~noteOff);
MIDIIn.addFuncTo(\noteOn,  ~noteOn);

MIDIIn.addFuncTo(\control, ~midiBusFiller);
MIDIIn.addFuncTo(\control, ~controlSetSynthValue);
MIDIIn.addFuncTo(\control, ~midiStartRecord);
MIDIIn.addFuncTo(\control, ~midiSelectActivePad);
MIDIIn.addFuncTo(\control, ~controlPads);

MIDIIn.addFuncTo(\control, ~midiOneButtonPressed);
MIDIIn.addFuncTo(\control, ~midiOneButtonDialed);
MIDIIn.addFuncTo(\control, ~midiNineButtonPressed);
MIDIIn.addFuncTo(\control, ~midiPendulumSettings);

MIDIIn.addFuncTo(\control, ~midiTenButtonDialed);
MIDIIn.addFuncTo(\control, ~midiGlobalVolume);

MIDIIn.addFuncTo(\control, { |src, chan, num, val|
    if ( num == 64, {
        // this sets the preset channel - in this case 32 presets channles,
        // each with 9 notes.
        ~runOnAppClock.value({
            randomBlackBoard.add("currChannel" -> [0,31,\lin].asSpec.map(
                [0,127,\lin].asSpec.unmap(val)).asInteger);
            gui.updateMappingTable( noteRemapPresets,
                randomBlackBoard["currChannel"]);
        });
    });
});

randomBlackBoard.add("shiftpressed" -> false);
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
// RcDebug.new(MIDIIn);

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

    var openAllSamplesButton = Button(mainWindow, Rect(450, 10, 120, 25));
    openAllSamplesButton.states = [
        ["Open All Samples", Color.black, Color.white]
    ];
    openAllSamplesButton.action =  {
        gui.pads.collect { |a| a.filename }.reject { |a| a.isNil }.asSet.do { |a|
            format( "open -a Audacity \"%\"", a ).unixCmd
        };
    };

    meterView.view.fixedHeight = 500;

    s.volume.gui(volView);
}.value();

// store and save setup
{
    cfgMgr.setupGui( mainWindow );
}.value();

// buttons and samples being played
{
    var view, exitButton, loadButton;
    var outDevSelector, inDevSelector, sampleRateButton, serverWinButton;

    rcRecorder = RcRecorder.new(s,
        Button(mainWindow, Rect(1650, 10, 120, 25)),
        ~addSample,
        cfgMgr
    );

    exitButton = Button(mainWindow, Rect(1650, 40, 120, 25));
    exitButton.states = [["Quit", Color.black, Color.white]];
    exitButton.action = {
        var w = Window("Really Quit?",
            resizable: false,
            bounds: Rect((Window.screenBounds.width)/2,
                (Window.screenBounds.height-180)/2, 330, 230)).front;

        var cancelBut = Button(w, Rect(10, 180, 120, 25));
        var exitBut = Button(w, Rect(200, 180, 120, 25));
        var txt = StaticText.new(w, Rect(130, 60, 200, 30));

        txt.string = "Really Quit?";

        w.alwaysOnTop = true;

        cancelBut.states = [["Cancel", Color.black, Color.white]];
        cancelBut.action = { w.close; };

        exitBut.states = [["Yes Quit", Color.black, Color.white]];
        exitBut.action = {
            rcRecorder.quitting;
            try { gui.padCtrl.allOff; } {};
            SystemClock.sched(0.2, {
                s.quit( { 0.exit; } );
            });
        };
    };

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


    serverWinButton = Button(mainWindow, Rect(640, 10, 120, 25));
    serverWinButton.states = [["Server Window", Color.black, Color.white]];
    serverWinButton.action = { s.makeWindow; };

    loadButton = Button(mainWindow, Rect(290, 10, 150, 25));
    loadButton.states = [["Load Sample", Color.black, Color.white]];
    loadButton.action = {
        FileDialog({ |paths|
            paths.do { |path| ~addSample.value(path); }
        }, {}, fileMode: 3, stripResult: false);
    };

    // title for the config
    gui.titleField = EZText(
        mainWindow, Rect(777, 40, 450, 25), "title",
        initVal: nil, labelWidth: 40, layout: \horz
    )
    .setColors(Color.white, Color.black, textBackground: Color.white)
    .enabled_(true)
    .visible_(true);


    // sample selector and synth type selector
    view = FlowView(mainWindow, Rect(777, 77, 450, 67), margin: 3@5);
    view.background = Color.grey;

	{
		var txt = StaticText.new(view, 285@25);
		txt.string = "Select sample VVV  select synth >>";
		txt.stringColor = Color.white;
	}.value();


    gui.synthSelection = PopUpMenu(view, 150@25);
    gui.synthSelection.items = synthLookup.collect { |a| a[1] }.repeatItems(3);
    gui.synthSelection.value = 0;
    gui.synthSelection.action = { |button|
        ~runOnAppClock.value({
            gui.synthType = (button.value / 3).asInteger;
        })
    };

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

    // pendulum settings
    view = FlowView(mainWindow, Rect(10, 112, 750, 32), margin: 3@3);
    view.background = Color.grey;

    gui.pendulumSelection = PopUpMenu(view, 180@24);
    gui.pendulumSelection.items = pendulumClaess.collect { |a| a[0] };
    gui.pendulumSelection.action = { |button|
        pendSettings.klzPendulum = pendulumClaess[button.value][1];
    };

    ~psBut = { |name, initVal|
        EZKnob(view, 180@24, name,
            ControlSpec.new(minval: 0, maxval: 59,
                warp: 'lin', step: 0.1, default: initVal, units: "secs"),
            unitWidth: 0, initVal: initVal, layout:\horz, labelWidth: 80)
        .setColors(Color.grey,Color.white)
        .font_(Font("Helvetica",11));
    };

    pendSettings.dials.start    = ~psBut.value( "Start", 0 );
    pendSettings.dials.duration = ~psBut.value( "Duration", 0 );
    pendSettings.dials.end      = ~psBut.value( "End", 0 );

    // dial settings
    view = FlowView(mainWindow, Rect(10, 50, 750, 60));

    8.do { arg idx;
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
    var view2 = FlowView(mainWindow, Rect(10, 160, 1220, 700));
    // view2.background = Color.grey;

    // this defines the 16 pads, 4 rows of 4 pads.
    4.do { |idx|
        view2.startRow;

        // there is a gap between the top 2 rows and the bottom 2 rows.
        if ( idx == 2, { 8.do { view2.startRow } } );

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
            2.do { |cidx|
                view.startRow;

                4.do { |kjdx|

                    var knob_action = { |knob|
                        var argidx = (cidx * 4) + kjdx;
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

                if ( cidx == 0,
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
                        paddef.fileText = EZText(view, 150@20, nil,
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
                    ~runOnAppClock.value({
                        paddef.ranger.value = msg[4];
                    });
                });
            }, '/pos');

        };
    }
}.value();

// setup presets info
{
    var view = FlowView(mainWindow, Rect(1250, 340, 530, 300));
    var loadButton, saveButton;

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

			SystemClock.sched(0.05, {
				try { gui.asOSC(darkPlace); } {};
				if ( rcRecorder.oscEndpoint.notNil, 0.05, nil );
			});
		}, {
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