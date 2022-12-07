//
// Discontinued functionality but kept here for reference.
//

/*
randomBlackBoard.add("randomstep" -> nil);

~randomUpdateInterval = { |knob|
    randomBlackBoard["randomstep"] = knob.value;
};

~randomDialChange = {
    var knob = (1..8).scramble.first;
    var val = 128.rand;
    // freqShift is rather extreme, therefore limit this to a specific
    // value range.
    if ( knob == 5, { val = (57..71).scramble.first });
    ~controlSetSynthValue.value(nil,nil,knob,val);
};

~randomSelectorChange = {
    var pad = gui.pads.select { |a| a.isDefined }.scramble.first;
    if ( pad.notNil, { gui.setActivePad(pad) });
};

~randomPadFreeze = {
    var lstPadsFrozen = ~queryPadsFrozen.value();

    var padNr = (20..35).select { |idx|
        lstPadsFrozen.indexOf(idx).isNil
    }.scramble.first;

    if ( padNr.isNil, {
        ~randomPadUnfreeze.value()
    }, {
        ~controlPads.value(nil,nil,padNr,1);
    });
};

~randomPadUnfreeze = {
    var padNr = ~queryPadsFrozen.value().scramble.first;

    if ( padNr.isNil, {
        ~randomPadFreeze.value()
    }, {
        ~controlPads.value(nil,nil,padNr,0);
    });
};

~randomNoteOn = {
    var lstNotesOn = ~queryNotesOn.value();

    var note = (0..(allSamples.size-1)).select { |idx|
        lstNotesOn.indexOf(idx).isNil
    }.scramble.first;

    if ( lstNotesOn.size > 9, {
        ~randomNoteOff.value()
    }, {
        ~noteOn.value(nil,nil,note,128.rand);
    });
};

~randomNoteOff = {
    var note = ~queryNotesOn.value().scramble.first;

    if ( note.notNil && (~queryNotesOn.value().size > 1), {
        ~noteOff.value(nil,nil,note,128.rand);
    }, { ~randomNoteOn.value() });
};

~randomAction = {
    if ( gui.currentSynth.isNil, {~randomNoteOn.value()});

    switch( 30.rand,
        10, { ~randomNoteOn.value() },
        11, { ~randomNoteOff.value() },
        12, { ~randomPadUnfreeze.value() },
        13, { ~randomPadFreeze.value() },

        14, { ~randomSelectorChange.value() },
        15, { ~randomSelectorChange.value() },
        16, { ~randomSelectorChange.value() },

        17, { ~randomSampleSelection.value() },
        18, { ~randomSampleSelection.value() },

        { ~randomDialChange.value() }
    );
};

~randomSampleSelection = {
    // avoid the oscillations ... they aren't cool
    ~runOnAppClock.value({
        gui.synthSelection.valueAction_([0,3].scramble.first);
    });
};
*/