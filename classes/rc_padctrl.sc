/*
You can change the Pad color by sending this SysEx message to "Arturia MiniLab mkII" MIDI output port:
 F0 00 20 6B 7F 42  02 00 10 7n cc F7

where:
  n is the pad number, 0 to F, corresponding to Pad1 to Pad16
  cc is the color:
    00 - black -- is off
    01 - red
    04 - green
    05 - yellow
    10 - blue
    11 - magenta
    14 - cyan
    7F - white

So this becomes in SuperCollidor:

MIDIClient.init
o=MIDIOut.new(0)
o.connect
o.sysex( Int8Array[0xF0, 0x00, 0x20, 0x6B, 0x7F,
                                   0x42, 0x02, 0x00, 0x10, 0x72, 0x11, 0xF7])

--> on state is set by doing this:

o.sysex( Int8Array[0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x02, 0x00, 0x00, 0x76, 0x7F, 0xF7])

===> parameter 0 is being set to 0x7f (127) for pad 7 (0x76)

this comes from this blog - https://www.untergeek.de/2014/11/taming-arturias-beatstep-sysex-codes-for-programming-via-ipad/

basically you can obtain the values using 0x01 in place of 0x02 and leave off the value (i.e. cc) ... using the midi log from the midi control center it's possible to see what comes back
*/

RcPadCtrl {

    var <>padColors;
    var <>midiOut;

    *magic {
        ^Int8Array[0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42];
    }
    *padNrs {
        ^(0x70..0x7F);
    }

    *new {
        ^super.new.init();
    }

    init {
        var thObj = this;

        midiOut = nil;
        padColors = Dictionary.new;

        try {
            MIDIIn.addFuncTo(\sysex, { |src, values|
                if ((values[8] == 0x11) && RcPadCtrl.padNrs.indexOf(values[9]).notNil,{
                    thObj.padColors[ values[9] ] = values[10];
                });
            });

            midiOut = MIDIOut.new(0);
            midiOut.connect;
            this.initColors;
        } {};
    }

    initColors {
        if ( midiOut.isNil, { ^nil });

        RcPadCtrl.padNrs.do { |padNr|
            midiOut.sysex( RcPadCtrl.magic ++ Int8Array[0x01,
                0x00, 0x11, padNr, 0xF7]);
        };
    }

    setSlot { |slot, padNr, value|
        if ( midiOut.isNil, { ^nil });

        midiOut.sysex( RcPadCtrl.magic ++ Int8Array[0x02,
            0x00, slot, padNr, value, 0xF7]);

    }

    allOff {
        RcPadCtrl.padNrs.do { |padNr|
            this.setSlot( 0x00, padNr, 0x00 );
            this.setSlot( 0x10, padNr, 0x00 );
        };
    }

    padOff { |padNr|
        // pad numbers weird here because the controler is rotated by
        // 180 degrees, 0x77 is the midpoint, either subtract if less than
        // 8 or add if greater than 8 -- 8 being the padNumber in the yaml
        // file, which goes from 0 to 15, left to right. Our pads go quasi from
        // right to left and upside down!
        var midiPadNr = 0x77 + if ( padNr < 8, padNr * -1, 16 - padNr );

        this.setSlot( 0x00, midiPadNr, 0x00 );
        this.setSlot( 0x10, midiPadNr, 0x00 );
    }

    padOn { |padNr|
        // pad numbers weird here because the controler is rotated by
        // 180 degrees, 0x77 is the midpoint, either subtract if less than
        // 8 or add if greater than 8 -- 8 being the padNumber in the yaml
        // file, which goes from 0 to 15, left to right. Our pads go quasi from
        // right to left and upside down!
        var midiPadNr = 0x77 + if ( padNr < 8, padNr * -1, 16 - padNr );

        this.setSlot( 0x00, midiPadNr, 0x7F );
        this.setSlot( 0x10, midiPadNr, this.padColors[midiPadNr] );
    }

    allOn { |func=nil|
        SystemClock.sched(0.01, {
            if ( (this.padColors.size == 16) || midiOut.isNil, {
                RcPadCtrl.padNrs.do { |padNr|
                    this.setSlot( 0x00, padNr, 0x7F );
                    this.setSlot( 0x10, padNr, this.padColors[padNr] );
                };

                if ( func.notNil && midiOut.notNil, func );
                nil;
            }, { 0.01 });
        });
    }
}