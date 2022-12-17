RcSynthDef {
    var <>synthLookup;

    *new { |a,b|
        ^super.new.init(a,b);
    }

    *argCount { ^14 }

    *argsToArgs { |args|
        ^[
            \arg0,      args[0],
            \arg1,      args[1],
            \arg2,      args[2],
            \arg3,      args[3],
            \arg4,      args[4],
            \arg5,      args[5],
            \arg6,      args[6],
            \arg7,      args[7],
            \vel,       args[8],
            \num,       args[9],
            \out,       args[10],
            \bufnum,    args[11],
            \sampleIdx, args[12],
            \padNr,     args[13]
        ];
    }

    *addInitVals { |initVals|
        var initArgs = List.new;

        if ( if ( initVals.notNil, { initVals.notEmpty }, { false } ), {
            7.do { |idx|
                initArgs.add("arg" ++ (idx+1)).add( initVals[idx+1] );
            };
        }, {
            initArgs.add( "arg1" ).add( 64 );
        });

        ^initArgs;
    }

    def01 { |synth_name|
        // play samples
        SynthDef.new(synth_name, { |arg0=32,  arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            // rate divide by 128 since we want 32 to be 0.25, mulitple 4 -> 1
            var rate      = (arg2 / 128) * 4;
            var roomsize  = arg3 / 127;
            var freqShift = [-1024,1024,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * rate,
                startPos: 0, loop: 1);

            phasor = Phasor.ar(1,
                rate * BufRateScale.kr(bufnum), 0, BufFrames.ir(bufnum));
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            #left, right = LinPan2.ar(output, pos: pan); //, level: vol);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);


            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def11 { |synth_name|
        // Play samples backwards and superslow
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=96, arg3=64,
            arg4=0, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            // rate is 1:1 when dial is set at 96
            var rate      = (arg2 / 96) * (1 / 0.75);
            var roomsize  = arg3 / 127;
            var freqShift = [0,2048,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * -1 * rate,
                startPos: BufFrames.ir(bufnum) - 1, loop: 1);

            phasor = Phasor.ar(1,
                rate * BufRateScale.kr(bufnum) * -1, BufFrames.ir(bufnum)-1,0);
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            output = FreqShift.ar(output, freq: freqShift);

            #left, right = LinPan2.ar(output, pos: pan);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreeVerb.ar(
                output * EnvGate.new(i_level:0, fadeTime: 1),
                mix:  mix,
                room: roomsize,
                damp: damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def10 { |synth_name|
        // play samples extra slow.
        SynthDef.new(synth_name, { |arg0=32,  arg1=64, arg2=96, arg3=64,
            arg4=0, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            // rate is 1:1 when dial is set at 96
            var rate      = (arg2 / 96) * (1 / 0.75);
            var roomsize  = arg3 / 127;
            var freqShift = [0,2048,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * rate,
                startPos: 0,
                loop: 1);

            phasor = Phasor.ar(1,
                rate * BufRateScale.kr(bufnum), 0, BufFrames.ir(bufnum));
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            output = FreqShift.ar(output, freq: freqShift);

            #left, right = LinPan2.ar(output, pos: pan);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreeVerb.ar(
                output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix,
                room:roomsize,
                damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def07 { |synth_name|
        SynthDef.new(synth_name, { |arg0=32,  arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            var rate      = arg2 / 127;
            var roomsize  = arg3 / 127;
            var freqShift = [-1024,1024,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;
            var sinRate;

            sinRate = FSinOsc.kr(XLine.kr(0.2, 8, arg2), 0, 3, rate);

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * sinRate,
                startPos: 0,
                loop: 1);

            phasor = Phasor.ar(1,
                sinRate * BufRateScale.kr(bufnum), 0, BufFrames.ir(bufnum));
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);



            #left, right = LinPan2.ar(output, pos: pan); //, level: vol);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def09 { |synth_name|
        // play sample with reverb and distortion
        SynthDef.new(synth_name, { |arg0=32,  arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=0, arg6=0, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            var rate      = (arg2 / 128) * 4;
            var roomsize  = arg3 / 127;
            var freqShift = [-1024,1024,\lin].asSpec.map(dialSpec.unmap(arg4));
            var lowgain   = [0.0001,0.3,\lin].asSpec.map(dialSpec.unmap(arg5));
            var highgain  = [0.0001,0.3,\lin].asSpec.map(dialSpec.unmap(arg6));
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;
            var sinRate;

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * rate,
                startPos: 0, loop: 1);

            phasor = Phasor.ar(1,
                rate * BufRateScale.kr(bufnum), 0, BufFrames.ir(bufnum));
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            output = AnalogVintageDistortion.ar(output,
                drivegain: vol,
                bias: 0,
                lowgain: lowgain,
                highgain: highgain,
                shelvingfreq: freqShift,
                oversample: 0);

            #left, right = LinPan2.ar(output, pos: pan); //, level: vol);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreqShift.ar(output, freq: freqShift);


            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: 1, room:roomsize, damp:0);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def08 { |synth_name|
        SynthDef.new(synth_name, { |arg0=32,  arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            var rate      = arg2 / 127;
            var roomsize  = arg3 / 127;
            var freqShift = [-1024,1024,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;
            var sinRate;

            sinRate = LFNoise2.kr(XLine.kr(1, 20, arg2), 2, mul: rate);

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * sinRate,
                startPos: 0,
                loop: 1);

            phasor = Phasor.ar(1,
                sinRate * BufRateScale.kr(bufnum), 0, BufFrames.ir(bufnum));
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            #left, right = LinPan2.ar(output, pos: pan); //, level: vol);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def04 { |synth_name|
        // Play samples backwards
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = (arg0 / 127) * 4;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            // rate divide by 128 since we want 32 to be 0.25, mulitple 4 -> 1
            var rate      = (arg2 / 128) * 4;
            var roomsize  = arg3 / 127;
            var freqShift = [-1024,1024,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));

            var left, right, output, phasor;

            output = PlayBuf.ar(2, bufnum,
                rate: BufRateScale.kr(bufnum) * -1 * rate,
                startPos: BufFrames.ir(bufnum) - 1, loop: 1);

            phasor = Phasor.ar(1,
                rate * BufRateScale.kr(bufnum) * -1,BufFrames.ir(bufnum)-1,0);
            SendReply.kr(Impulse.kr(10),
                '/pos', [padNr, phasor/BufFrames.ir(bufnum)]);

            #left, right = LinPan2.ar(output, pos: pan); //, level: vol);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def02 { |synth_name|
        // sin oscillations
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = arg0 / 127;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            var phase     = (((arg2+1) / 32) - 2) * 2pi;
            var roomsize  = arg3 / 127;
            var freqShift = [-256,256,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));
            var freq      = [20,20000,\lin].asSpec.map(dialSpec.unmap(num));

            var left, right, output;

            left = SinOsc.ar(freq, phase: phase * 0.9, mul:vol) *
                                        EnvGate.new(i_level:0, fadeTime: 1);
            right = SinOsc.ar(freq, phase: phase * 1.1, mul: vol) *
                                        EnvGate.new(i_level:0, fadeTime: 1);

            output = BiPanB2.ar(left, right, azimuth: elevation);

            output = LinPan2.ar(output, pos: pan); //, level: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def03 { |synth_name|
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = arg0 / 127;
            var pan       = [0,10,\lin].asSpec.map(dialSpec.unmap(arg1));
            var phase     = [1,2000,\lin].asSpec.map(dialSpec.unmap(arg2));
            var roomsize  = arg3 / 127;
            var freqShift = [-256,256,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));
            var freq      = [20,2000,\lin].asSpec.map(dialSpec.unmap(num));

            var left, right, output;

            left = AnalogFoldOsc.ar(
                freq: freq,
                amp: SinOsc.ar(LFSaw.kr(0.1) * 4).range(1,0, pan),
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            right = AnalogFoldOsc.ar(
                freq: freq,
                amp: SinOsc.ar(LFSaw.kr(0.1) * 5).range(0.0,pan),
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            left = LPF.ar(left, phase);
            right = LPF.ar(right, phase);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain:vol);

            output = LinPan2.ar(output, pos: 0); //, level: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def05 { |synth_name|
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = arg0 / 127;
            var pan       = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg1));
            var phase     = (((arg2+1) / 32) - 2) * 2pi;
            var roomsize  = arg3 / 127;
            var freqShift = [-256,256,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));
            var freq      = [20,20000,\lin].asSpec.map(dialSpec.unmap(num));

            var left, right, output;

            left = VosimOsc.ar(
                freq: freq,
                form1freq: SinOsc.kr([0.1,0.5]).exprange(100,1000),
                form2freq: SinOsc.kr(0.10).exprange(100,1000),
                shape: SinOsc.kr(phase)
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            right = VosimOsc.ar(
                freq: freq,
                form1freq: SinOsc.kr([0.5,0.1]).exprange(100,1000),
                form2freq: SinOsc.kr(0.39).exprange(100,1000),
                shape: SinOsc.kr(phase)
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = LinPan2.ar(output, pos: pan); //, level: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }

    def06 { |synth_name|
        SynthDef.new(synth_name, { |arg0=32, arg1=64, arg2=32, arg3=64,
            arg4=64, arg5=64, arg6=64, arg7=64,
            vel=nil, num=nil, out=0, bufnum=nil,
            sampleIdx=inf, padNr=nil|

            var dialSpec = [0,127,\lin].asSpec;

            var vol       = arg0 / 127;
            var pan       = [0,4000,\lin].asSpec.map(dialSpec.unmap(arg1));
            var phase     = [0,40,\lin].asSpec.map(dialSpec.unmap(arg2));
            var roomsize  = arg3 / 127;
            var freqShift = [-256,256,\lin].asSpec.map(dialSpec.unmap(arg4));
            var damp      = arg5 / 127;
            var mix       = arg6 / 127;
            var elevation = [-1,1,\lin].asSpec.map(dialSpec.unmap(arg7));
            var freq      = [10,9000,\lin].asSpec.map(dialSpec.unmap(num));

            var left, right, output;

            left = VosimOsc.ar(
                freq: SinOsc.kr(SinOsc.kr(freq)).exprange(10,900),
                form1freq: SinOsc.kr([0.1,0.5]).exprange(100,pan),
                form2freq: SinOsc.kr(0.10).exprange(100,1000),
                shape: SinOsc.kr(phase)
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            right = VosimOsc.ar(
                freq: SinOsc.kr(SinOsc.kr(freq)).exprange(10,800),
                form1freq: SinOsc.kr([0.5,0.1]).exprange(100,pan),
                form2freq: SinOsc.kr(0.39).exprange(100,1000),
                shape: SinOsc.kr(phase)
            ) * EnvGate.new(i_level:0, fadeTime: 1);

            output = BiPanB2.ar(left, right, azimuth: elevation, gain: vol);

            output = LinPan2.ar(output, pos: 0, level: vol);

            output = FreqShift.ar(output, freq: freqShift);

            output = FreeVerb.ar(output * EnvGate.new(i_level:0, fadeTime: 1),
                mix: mix, room:roomsize, damp:damp);

            Out.ar(out, output * EnvGate.new);
        }).add;
    }


    init { |playOsc, playSample|
        synthLookup = List.new();

        synthLookup.add(["sample-synth",
            "Sample Forward",           playSample, { |a| this.def01(a) } ]);
        synthLookup.add(["sample-synth-backward",
            "Sample Backward",  playSample, { |a| this.def04(a) } ]);
        synthLookup.add(["sample-synth-sin",
            "Sample Sin",       playSample, { |a| this.def07(a) } ]);
        synthLookup.add(["sample-synth-distort",
            "Sample Distort",   playSample, { |a| this.def09(a) } ]);
        synthLookup.add(["sample-synth-zigzag",
            "Sample ZigZag",    playSample, { |a| this.def08(a) } ]);
        synthLookup.add(["sample-synth-superslow",
            "Sample Superslow", playSample, { |a| this.def10(a) } ]);
        synthLookup.add(["sample-synth-backward-superslow",
            "Sample Back-Superslow", playSample, { |a| this.def11(a) } ]);

        synthLookup.add(["sinosc-synth",
            "Sin Osc",          playOsc,    { |a| this.def02(a) } ]);
        synthLookup.add(["analogosc-synth",
            "Analog Osc",       playOsc,    { |a| this.def03(a) } ]);
        synthLookup.add(["vosimosc",
            "Vosim Osc",        playOsc,    { |a| this.def05(a) } ]);
        synthLookup.add(["vosimosc-two",
            "Vosim Osc II",     playOsc,    { |a| this.def06(a) } ]);

        synthLookup.do { |a| a[3].value(a[0]) };
    }
}