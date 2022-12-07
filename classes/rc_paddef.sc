
RcPadDefColours {
    classvar <padColours;

    *initClass {
        padColours = [
        [
            Color.new(red: 0, green: 191/255, blue: 1, alpha: 0.8),
            Color.new(red: 0, green: 191/255, blue: 1, alpha: 0.8),
            Color.new(red: 152/255, green: 251/255, blue: 152/255, alpha: 0.8),
            Color.new(red: 152/255, green: 251/255, blue: 152/255, alpha: 0.8)
        ],
        [
            Color.new(238/255,130/255,238/255, alpha: 0.8),
            Color.new(238/255,130/255,238/255, alpha: 0.8),
            Color.new(255/255,255/255,240/255, alpha: 0.8),
            Color.new(255/255,255/255,240/255, alpha: 0.8)
        ],
        [
            Color.new(255/255,99/255,71/255, alpha: 0.8),
            Color.new(255/255,99/255,71/255, alpha: 0.8),
            Color.new(255/255,255/255,102/255, alpha: 0.8),
            Color.new(255/255,255/255,102/255, alpha: 0.8)
        ],
        [
            Color.cyan(alpha: 0.8),
            Color.cyan(alpha: 0.8),
            Color.new(red: 0, green: 191/255, blue: 1, alpha: 0.8),
            Color.new(red: 0, green: 191/255, blue: 1, alpha: 0.8)
        ]
        ];
    }
}

RcPadDef {
    var <>mainView;
    var <>commentText;
    var <>knobs;
    var <>typeText;
    var <>fileText;
    var <>synth;
    var <>noteNum;
    var <>pendulums;
    var <>myColour;
    var <>filename;
    var <>sampleIdx;
    var <>padIdx;
    var <>midiPadNum;
    var <>ranger;
    var <>samplePos;
    var <>plotter;

    *new { |main_view, comment_view, rowNum, colNum|
        ^super.new.init(main_view, comment_view, rowNum, colNum);
    }

	*asOSCZero { |idx|
		^[RcPadDef.oscPadId(idx), -1,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            "zero", 0.0];
	}

	*oscPadId { |idx|
		^format( "/pad%", idx.asInteger ).asSymbol;
	}

    init { |mView, cView, rowNum, colNum|
        knobs       = List.new();
        pendulums   = List.new();
        commentText = cView;
        mainView    = mView;
        synth       = nil;
        sampleIdx   = nil;
        myColour    = RcPadDefColours.padColours.value()[rowNum][colNum];
        padIdx      = (rowNum * 4) + colNum;

        mainView.background = nil;
    }

    setup { |values, noteText, synthType|
        8.do { |idx| knobs[idx].value = values[idx] };
        commentText.value = noteText;
        commentText.setColors(textBackground: Color.new(1,0.7,0.7));
        mainView.background = myColour;
        typeText.value_(typeText.items.indexOfEqual(synthType));
    }

    knobIdx { |srchKnb|
        knobs.do { |knb,idx| if ( knb == srchKnb, { ^idx; }); };
        ^nil;
    }

    activate { |knbs|
        // mainView.background = Color.new(34/255, 139/255, 34/255);
        mainView.background = myColour.scaleByAlpha;
        8.do { |idx| knbs[idx].value = knobs[idx].value };
    }

    inactive {
        if ( this.isDefined, { mainView.background = myColour },
            {mainView.background = nil } );
    }

    isDefined {
        ^synth.notNil;
    }

    isActive {
        ^(mainView.background == Color.green);
    }

    pushPendulum { |pend|
        pendulums.add(pend)
    }

    activePendulums {
        ^pendulums.reject { |pend,idx| pend.isStopped }
    }

    synthUpdate { |newSynth|
        this.synth = newSynth;
        this.activePendulums.do { |pend| pend.synth = newSynth };
    }

    reset {
        this.inactive;
        pendulums.do { |pend| pend.stop; };
        pendulums = List.new();
        mainView.background = nil;
        commentText.value = "";
        commentText.setColors(textBackground: Color.white);
        fileText.value = "";
        typeText.value_(0);
        synth = nil;
        knobs.do { |k| k.value = 0 };
        AppClock.sched(1.3, {
            ranger.value = 0.0;
        });
        { plotter.value = [0,0,0,0,0]; }.defer;
        filename = nil;
    }

    asOSC { |pNum|
        var rVal = List.new;

        rVal.add( RcPadDef.oscPadId(pNum) );

        rVal.add( if ( sampleIdx.isNil,
			{ noteNum.asInteger },
			{ sampleIdx.asInteger })
		);

        knobs.do { |a| rVal.add( a.value.asInteger / 127 ) };
        rVal.add( synth.defName );
        rVal.add( samplePos );

        ^rVal.asArray;
    }

    asYaml { |pNum|
        var initValues = [];
        var padYaml;
        var pendYaml;

        // overwrite initial dial value if the pendulum begins at a specific
        // value, regardless if the current dial value.
        initValues = knobs.collect { |a| a.value.asInteger };
        this.activePendulums.do { |pend|
            var idx = pend.argNum - 1;
            initValues[idx] = pend.initValue( initValues[idx] ).asInteger;
        };

        padYaml = format("pad%:\n" ++
            "  padnum: %\n"      ++
            "  note: %\n"        ++
            "  filename:%\n"     ++
            "  comment: \"%\"\n" ++
            "  values: [%]\n"    ++
            "  synth: %\n"       ++
            "  pendulums: [%]\n",
            pNum.asInteger,
            pNum.asInteger,
            noteNum.asInteger,
            if ( filename.isNil, { "" }, { " \""++filename++"\"" }),
            commentText.value,
            initValues.join(","),
            synth.defName,
            this.activePendulums.collect { |a,idx|
                format("pend%_%",pNum,idx)
            }.join(","));

        pendYaml = this.activePendulums.collect { |pend,idx|
            pend.asYaml( format("%_%",pNum,idx) );
        }.join("\n");

        ^(padYaml ++ pendYaml);
    }
}