RcConfig {
    var <>loadButton;
    var <>saveButton;
    var <>mergeButton;

    var <>guiMgr;
    var <>optsDir;
    var <>s;
    var <>addSample;

    *new { |oDir, gu, server|
        ^super.new.init(oDir, gu, server);
    }

    init { |oDir, gu, server|
        optsDir = oDir;
        guiMgr  = gu;
        s       = server;
    }

    setupGui { |mainWindow|
        var view = FlowView(mainWindow, Rect(10,10,235,29));
        view.background = Color.grey;

        RcHelpers.addStaticText(view, "Scape:", 60@25);

        loadButton = RcHelpers.buttonBW(
            "Load", view, 50@25, { this.loadWithDialog }
        );

        saveButton = RcHelpers.buttonBW(
            "Save", view, 50@25, { this.saveWithDialog }
        );

        mergeButton = RcHelpers.buttonBW(
            "Merge", view, 50@25, { this.mergeWithDialog }
        );
    }

    filename { |tstamp=nil|
        ^(optsDir ++ format("/scape.%.yaml",
            if ( tstamp.notNil, { tstamp }, { Date.localtime.stamp })));
    }

    loadWithDialog {
        Dialog.openPanel( { |path|
            RcClock.reset;
            s.volume.volume = guiMgr.loadConfigFromFile(path, addSample);
        }, path: optsDir);
    }

    saveWithDialog {
        Dialog.savePanel( { |path|
            guiMgr.saveConfigToFile(path, s.volume.volume);
        }, path: this.filename);
    }

    mergeWithDialog {
        Dialog.openPanel( { |path|
            guiMgr.mergeConfigFile(path, addSample);
        }, path: this.filename);
    }

    saveWithoutDialog { |tstamp=nil|
        var path = this.filename(tstamp);
        guiMgr.saveConfigToFile(path, s.volume.volume);

        AppClock.sched(0, {
            saveButton.states = [["Save", Color.white, Color.green]];
            AppClock.sched(1, {
                saveButton.states = [["Save", Color.black, Color.white]];
                nil
            });
            nil
        });
    }
}
