RcRecorder {
    var <>startTime;
    var <>guiButton;
    var <>server;
    var <>btnStates;
    var <>currPath;
    var <>onComplete;
    var <>oscEndpoint;
    var <>configMgr;

    *new { |srv, gui, onRecordComplete, cfgMgr|
        ^super.new.init(srv,gui,onRecordComplete, cfgMgr);
    }

    init { |srv,gui, onRecordComplete, cfgMgr|
        btnStates = [
            ["Start Recording", Color.black, Color.white],
            ["Stop Recording", Color.white, Color.red]
        ];

        server     = srv;
        guiButton  = gui;
        currPath   = nil;
        configMgr  = cfgMgr;
        onComplete = onRecordComplete;

        guiButton.states = btnStates;
        guiButton.action = { this.toggle };
    }

    loop_ {
        this.startTime = Date.localtime.rawSeconds.asInteger;

        SystemClock.sched(0.0, {
            AppClock.sched(0, {
                guiButton.states = [
                    btnStates[0],
                    [format("Stop %",
                        RcClock.clkString( this.startTime,
                            Date.localtime.rawSeconds.asInteger)),
                        Color.white, Color.red]
                ];
                guiButton.value = 1;
            });

            if ( this.server.isRecording, { 1.0 }, {
                AppClock.sched(0, {
                    guiButton.states = btnStates;
                    guiButton.value = 0;
                });
                nil
            });
        });
    }

    toggle {
        if ( server.isRecording, {
            server.stopRecording;
            this.sendOsc( ["/recStop"] );

            AppClock.sched(0, {
                guiButton.states = btnStates;
                guiButton.value = 0;

                if ( this.currPath.notNil, {
                    if ( this.onComplete.notNil, {
                        this.onComplete.value(this.currPath);
                    });
                    this.currPath = nil;
                });
            });
        }, {
            server.record;
            this.sendOsc( ["/recStart"] );

            AppClock.sched(1.0, {
                format("Recording to %",server.recorder.path).postln;

                this.currPath = server.recorder.path;
                this.sendOsc( ["/recFilename", this.currPath.basename] );
                this.configMgr.saveWithoutDialog(
                    this.currPath.basename.replace(".aiff").replace("SC_","RC_")
                );
            });
            this.go;
        });
    }

    go {
        SystemClock.sched(0.0, {
            if ( server.isRecording, {
                this.loop_();
                nil
            }, {
                0.2
            });
        });
    }

    sendOsc { |ary|
        if ( oscEndpoint.notNil, {
            try { oscEndpoint.sendMsg( *ary ) } {};
        });
    }

    quitting {
        if ( server.isRecording, { server.stopRecording; });
        this.sendOsc(["/quit"])
    }
}
