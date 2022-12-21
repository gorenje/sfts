RcClock {
    classvar <>instance;

    var <>txtClock;
    var <>startTime;

    *new { |mainWindow|
        if ( instance.notNil, { ^instance }, {
            instance = super.new.init(mainWindow);
            ^instance;
        })
    }

    *reset {
        instance.reset();
    }

    *hrsMinsSecs { |startSeconds, endSeconds|
        var rVal = [0,0,0];
        var diffSecs = endSeconds - startSeconds;

        rVal[2] = diffSecs.mod(60).asInteger;
        rVal[1] = ((diffSecs - rVal[2]).mod(3600) / 60).asInteger;
        rVal[0] = (diffSecs / 3600).asInteger;

        ^rVal;
    }

    *clkString { |startSeconds, endSeconds|
        var clk = RcClock.hrsMinsSecs(startSeconds, endSeconds);

        ^format("%:%:%",
            clk[0].asString.padLeft(2,"0"),
            clk[1].asString.padLeft(2,"0"),
            clk[2].asString.padLeft(2,"0"))
    }

    init { |mainWindow|
        txtClock = RcHelpers.buttonBW("00:00:00",mainWindow,
            Rect( 253, 10, 100, 25 ), { RcClock.reset });

        this.reset();
        this.loop_();
    }

    reset {
        startTime = Date.localtime.rawSeconds.asInteger;
    }

    loop_ {
        SystemClock.sched(0.0, {
            AppClock.sched(0, {
                txtClock.states = [
                    [ RcClock.clkString(
                        this.startTime,
                        Date.localtime.rawSeconds.asInteger
                    ), Color.black, Color.white ]
                ];
            });
            1
        });
    }

}