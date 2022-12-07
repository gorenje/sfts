RcGui {
    var <>notes, <>pads, <>knobs, <>synthType, <>knobNameLookup, <>dialValues;
    var <>synthSelection;
    var <>pendulumSelection;
    var <>sampleSelector;
    var <>lastDialedDial;
    var >synthLookup, >knobs, >allSamples;

    var midiToPadIdx, activePad, playingNotes;

    var <>chanValue, <>mappingTable;

    var <>titleField;
    var <>padCtrl;

    *new {
        ^super.new.init();
    }

    *appClockRun { |func|
        AppClock.sched(0, { func.value(); nil });
    }

    init {
        notes          = List.new();
        // pads are setup by the GUI code, so the first X pads are filled
        // in outside of this class.
        pads           = List.new();
        knobs          = List.new();
        knobNameLookup = Dictionary.new;
        dialValues     = Array.newClear(indexedSize: 8);
        synthType      = 0;
        playingNotes   = nil!1024;

        // Dial naming lookup
        knobNameLookup.add( 0 -> "Volume (32 = 1-to-1)");
        knobNameLookup.add( 1 -> "Pan (64 = mid)");
        knobNameLookup.add( 2 -> "Rate (32 = 1-to-1)");
        knobNameLookup.add( 3 -> "Room Size");
        knobNameLookup.add( 4 -> "Freq Shift (64 = 1-to-1)");
        knobNameLookup.add( 5 -> "Damp");
        knobNameLookup.add( 6 -> "Mix");
        knobNameLookup.add( 7 -> "Front <--> Back");

        midiToPadIdx = Dictionary.new;
        // Pads 1 to 4, Bank A
        midiToPadIdx.add( 20 -> 4 );
        midiToPadIdx.add( 21 -> 5 );
        midiToPadIdx.add( 22 -> 6 );
        midiToPadIdx.add( 23 -> 7 );
        // Pads 5 to 8, Bank A
        midiToPadIdx.add( 24 -> 0 );
        midiToPadIdx.add( 25 -> 1 );
        midiToPadIdx.add( 26 -> 2 );
        midiToPadIdx.add( 27 -> 3 );
        // Pads 1 to 4, Bank B
        midiToPadIdx.add( 28 -> 12 );
        midiToPadIdx.add( 29 -> 13 );
        midiToPadIdx.add( 30 -> 14 );
        midiToPadIdx.add( 31 -> 15 );
        // Pads 5 to 8, Bank B
        midiToPadIdx.add( 32 -> 8 );
        midiToPadIdx.add( 33 -> 9 );
        midiToPadIdx.add( 34 -> 10 );
        midiToPadIdx.add( 35 -> 11 );
    }

    setupPad { |midiControlNum, synthValues, note|
        var pad = pads[midiToPadIdx[midiControlNum]];
        if ( pad.notNil, {
            pad.setup(synthValues, note.text, note.synthType);

            // sampleIdx is location 12 in the values array...
            if (allSamples[synthValues[12]].notNil, {
                var sample = allSamples[synthValues[12]];
                pad.fileText.value = sample.basename;
                pad.commentText.value = [sample.comment,sample.duration].join(", ");
                pad.sampleIdx = synthValues[12];
            });

            pad.synth = note.synth;
            pad.noteNum = note.noteNum;
            pad.filename = note.filename;

            if ( note.presetData.notNil, {
                note.presetData.setUpPendulums(pad);
            });

            if ( note.buffer.notNil, {
                AppClock.sched(0.2.rand, {
                    note.buffer.loadToFloatArray(action: { arg array;
                        { pad.plotter.value = array; }.defer;
                    });
                });
            });
        });
        ^pad;
    }

    padTaken { |midiControlNum|
        var pad = pads[midiToPadIdx[midiControlNum]];
        ^(if ( pad.notNil, { pad.isDefined }, { false } ));
    }

    setActivePad { |pad|
        pads.do { |p| p.inactive };
        activePad = nil;
        if ( this.playingNote.isNil, {
            pad.activate(knobs);
            activePad = pad;
        });
    }

    releasePadByMidiControlNum { |midiControlNum|
        this.releasePad( pads[midiToPadIdx[midiControlNum]] );
    }

    releasePad { |pad|
        if ( pad.notNil, {
            pad.synth.set(\fadeTime, 2);
            pad.synth.release;
            pad.reset;
            if ( activePad == pad, { activePad = nil });
        });
    }

    pushPendulum { |pend|
        if ( this.playingNote.isNil && activePad.notNil, {
            activePad.pushPendulum(pend)
        });
    }

    dialChanged { |idx, value|
        if ( this.playingNote.isNil && activePad.notNil, {
            activePad.knobs[idx].value = value;
            lastDialedDial = activePad.knobs[idx]
        }, { lastDialedDial = nil });

    }

    noteOn { |note, num, vel, text|
        pads.do { |p| p.inactive };
        activePad = nil;
        playingNotes[num] = note.show(num, text, note.synth);
    }

    noteOff { |num, vel|
        if ( playingNotes[num].notNil, {
            if (playingNotes[num].synth.notNil,{
                playingNotes[num].hideAndRelease;
            });
            playingNotes[num] = nil;
        });
    }

    playingNote {
        ^notes.reject { |n| n.isFree }.first;
    }

    obtainNote {
        var note = notes.select { |n| n.isFree }.first;
        if(note.notNil, { note.isFree = false; });
        ^note;
    }

    currentSynth {
        var note = this.playingNote;
        if ( note.isNil, {
            if ( activePad.notNil, { ^activePad.synth }, { ^nil });
        }, { ^note.synth });
    }

    currentDial { |idx|
        var note = this.playingNote;
        if ( note.isNil, {
            if ( activePad.notNil, { ^activePad.knobs[idx] }, { ^knobs[idx] });
        }, { ^knobs[idx] });
    }

    currentPad {
        ^activePad;
    }

    asOSC { |netAddr|
        pads.do { |p,idx|
			try {
				if ( p.isDefined, {
                    netAddr.sendMsg( *p.asOSC(idx) );
				}, {
                    netAddr.sendMsg( *RcPadDef.asOSCZero(idx) );
				});
			} {};
        };
    }

    asYaml {
        var content = List.new;
        var allPadIds = List.new;

        pads.do { |p,idx|
            if ( p.isDefined, {
                content.add( p.asYaml(idx) );
                allPadIds.add( format("pad%",idx) );
            });
        };

        ^( format("allPadIds: [%]\n", allPadIds.asArray.join(",")) ++
            content.asArray.join("\n") );
    }

    saveConfigToFile { |path, server_volume|
        var fle = File.open( path, "w+" );

        var title = titleField.value.replace("\"","").replace(
            "\t", " ").replace("\n", " ").replace("\r", " ");

        fle.write( "# -*- fundamental -*-\n" );
        fle.write( "# Sounds from the Scape Config file\n" );
        fle.write( "# \n" );
        fle.write( format( "serverVolume: %\n", server_volume ) );
        fle.write( format( "title: \"%\"\n", title) );
        fle.write( this.asYaml );
        fle.close;

        "Saved Config to %\n".postf(path);
    }

    padIdxToMidi { |idx|
        ^midiToPadIdx.findKeyForValue(
            idx.asInteger
        );
    }

    loadConfigFromFile { |path, loadSample|
        var dataDict = path.parseYAMLFile;
        var padMidiBaseIdx = 20;

        try { padCtrl.allOff; } {};

        format( "Last loaded config: %", path ).postln;

        titleField.value = dataDict.atFail("title", path.basename);
        if ( titleField.value == "", {
            titleField.value = path.basename;
        });

        // this will also stop all the pendulums attached to the pad.
        // this might take a few milliseconds, hence below we pause for
        // fractual second to all everything to catch up.
        pads.do { |p| this.releasePad(p) };
        playingNotes.reject { |n| n.isNil }.do { |n|
            if ( n.synth.notNil, { n.hideAndRelease; })
        };

        dataDict["allPadIds"].do { |pdid|
            if ( dataDict[pdid]["filename"].notNil, {
                loadSample.value( dataDict[pdid]["filename"] );
            });
        };

        dataDict["allPadIds"].do { |pdid|
            var padData = dataDict[pdid];

            var synthDef = synthLookup.reject { |a|
                a.first != padData["synth"]
            }.first;
            var padMidiNum = midiToPadIdx.findKeyForValue(
                padData["padnum"].asInteger
            );

            var initVals = padData["values"].collect { |a| a.asInteger };
            var synthValues = initVals ++ (0!6);

            var note = nil;

            this.synthType = synthLookup.indexOf( synthDef );

            if ( padData["filename"].isNil, {
                note = synthDef[2].value(padData["note"].asInteger,
                    initVals[0],initVals);
                synthValues[12] = inf;
            }, {
                var sampleIdx = allSamples.collect { |a|
                    a.filename
                }.indexOfEqual( padData["filename"] );
                synthValues[12] = sampleIdx;
                note = synthDef[2].value( sampleIdx, initVals[0],initVals);
            });

            // give the gui and the app one secound to clean up all the
            // pendulums that got stopped at the beginning of this method.
            AppClock.sched(0.7 + 0.3.rand, {
                var pad = this.setupPad( padMidiNum, synthValues, note );

                note.synth.set(\padNr, padMidiNum);
                note.hide();

                try { this.padCtrl.padOn( padData["padnum"].asInteger ) } {};

                padData["pendulums"].do { |pendId|
                    var pend = RcPendulumBase.newWithPad(dataDict[pendId],pad);
                    if ( pend.notNil, {
                        pad.pushPendulum(pend.startFromYamlLoad);
                    });
                };
                nil
            });
        };

        ^dataDict["serverVolume"].asFloat;
    }

    updateMappingTable { |keymappings, chan|
        var cnt = 0;
        chanValue.string = "Current Presets Channel: " ++ (chan+1);

        mappingTable.values.do { |txt| txt.value = ""; };

        mappingTable.keys.do { |num|
            var key = [chan,num];
            var val = keymappings.atFail( key, nil );
            var fld = mappingTable[num];

            if ( val.notNil, {
                fld.value = val.comment;
                fld.setColors(textBackground: Color.yellow,
                    textStringColor: Color.black)
            }, {
                fld.setColors(textBackground: Color.white,
                    textStringColor: Color.black)
            });
        };
    }
}