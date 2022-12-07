RcDebug {
    *new { |midi_in|
        ^super.new.init(midi_in);
    }

    init { |midi_in|
        midi_in.addFuncTo(\noteOn, { |src, chan, num, vel|
            var lne = "NoteON s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\noteOff, { |src, chan, num, vel|
            var lne = "NoteOff s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\polytouch, { |src, chan, num, vel|
            var lne = "PolyTouch s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\touch, { |src, chan, num, vel|
            var lne = "Touch s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\control, { |src, chan, num, vel|
            var lne = "Control s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\program, { |src, chan, num, vel|
            var lne = "Program s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\bend, { |src, chan, num, vel|
            var lne = "Bend s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\sysex, { |src, chan, num, vel|
            var lne = "Sysex s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\sysrt, { |src, chan, num, vel|
            var lne = "Sysrt s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\invalid, { |src, chan, num, vel|
            var lne = "Invalid s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });

        midi_in.addFuncTo(\smpte, { |src, chan, num, vel|
            var lne = "Smpte s: ";
            lne = lne + src + ", c: " + chan + ", n: " + num + ", v: " + vel;
            lne.postln;
        });
    }

}
