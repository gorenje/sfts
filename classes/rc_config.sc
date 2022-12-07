RcConfig {
    var <>loadButton;
    var <>saveButton;
    var <>guiMgr;
    var <>optsDir;
    var <>s;
    var <>addSample;

    *new { |oDir, gu, server|
        ^super.new.init(oDir, gu, server);
    }

    init { |oDir, gu, server|
        optsDir = oDir;
        guiMgr = gu;
        s = server;
    }

    setupGui { |mainWindow|
        loadButton = Button(mainWindow, Rect(10, 10, 120, 25));
        loadButton.states = [["Load Scape", Color.black, Color.white]];
        loadButton.action = { this.loadWithDialog };

        saveButton = Button(mainWindow, Rect(150, 10, 120, 25));
        saveButton.states = [["Save Scape", Color.black, Color.white]];
        saveButton.action = { this.saveWithDialog };
    }

    filename { |tstamp=nil|
        ^(optsDir ++ format("/scape.%.yaml",
            if ( tstamp.notNil, { tstamp }, { Date.localtime.stamp })));
    }

    loadWithDialog {
        Dialog.openPanel( { |path|
            s.volume.volume = guiMgr.loadConfigFromFile(path, addSample);
        }, path: optsDir);
    }

    saveWithDialog {
        Dialog.savePanel( { |path|
            guiMgr.saveConfigToFile(path, s.volume.volume);
        }, path: this.filename);
    }

    saveWithoutDialog { |tstamp=nil|
        var path = this.filename(tstamp);
        guiMgr.saveConfigToFile(path, s.volume.volume);

        AppClock.sched(0, {
            saveButton.states = [["Save Scape", Color.white, Color.green]];
            AppClock.sched(1, {
                saveButton.states = [["Save Scape", Color.black, Color.white]];
                nil
            });
            nil
        });
    }
}
