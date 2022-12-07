RcPresets {
    classvar <>loadButton;
    classvar <>saveButton;
    classvar <>presetsFile;

    var <>srcNoteNum;
    var <>srcChannel;
    var <>destNoteNum;
    var <>filename;
    var <>dialValues;
    var <>synthName;
    var <>commentText;

    var <>pendHash;

    *setupGui { |mainWindow, noteRemapPresets, allSamples, addSample,
                  presFile, rndBlackBoard|
        presetsFile = presFile;

        loadButton = Button(mainWindow, Rect(1250, 650, 150, 25));
        loadButton.states = [["Load Presets", Color.black, Color.white]];
        loadButton.action = {
            Dialog.openPanel( { |path|
                RcPresets.loadFromPath( path, noteRemapPresets,
                    allSamples, addSample )
            }, path: presetsFile)
        };

        saveButton = Button(mainWindow, Rect(1450, 650, 150, 25));
        saveButton.states = [["Save Presets", Color.black, Color.white]];
        saveButton.action = {
            Dialog.savePanel( { |path|
                RcPresets.saveToPath(
                    noteRemapPresets,
                    path,
                    rndBlackBoard["currChannel"]
                );
            }, path: presetsFile)
        };
    }

    *newFromYaml { |yml, allYaml|
        var obj = super.new.init(nil,nil,nil);

        obj.srcNoteNum  = yml["note"].asInteger;
        obj.srcChannel  = yml["chan"].asInteger;
        obj.destNoteNum = yml["destnotenum"].asInteger;
        obj.commentText = yml["comment"];
        obj.synthName   = yml["synth"];
        obj.filename    = yml["filename"];
        obj.dialValues  = yml["values"].collect { |a| a.asInteger };

        if ( yml["pendulums"].notNil, {
            obj.pendHash = Dictionary.new;

            yml["pendulums"].do { |pendId|
                obj.pendHash[pendId] = RcPendulumBase.newFromYaml(
                    allYaml[pendId]
                );
            };
        });

        ^obj;
    }

    *newOnlyDest { |num|
        var obj = super.new.init(nil,nil,nil);
        obj.destNoteNum = num;
        ^obj;
    }

    *empty {
        ^RcPresets.new
    }

    *loadFromPath { |path, presetsMap, allSamples, loadSample|
        var dataPresets = path.parseYAMLFile;
        dataPresets["allPresets"].do { |psetId|
            var presets = RcPresets.newFromYaml(dataPresets[psetId],
                dataPresets);

            if ( presets.filename.notNil, {
                if ( File.exists(presets.filename), {
                    format("%\r",presets.filename).post;
                    loadSample.value(presets.filename);

                    allSamples.do { |sample,idx|
                        if ( sample.filename == presets.filename, {
                            presets.destNoteNum = idx;
                        });
                    };
                }, {
                    presets.commentText = "FILE MISSING";
                });
            });

            presetsMap.add([presets.srcChannel,presets.srcNoteNum] -> presets)
        };

        if ( dataPresets["currentChannel"].isNil, { ^0 }, {
            ^dataPresets["currentChannel"].asInteger;
        })
    }

    *saveToPath { |presetsMap, path, currentChannel|
        var fle = File.open( path, "w+" );

        fle.write( "# -*- fundamental -*-\n" );
        fle.write( "# Sounds from the Scape Presets file\n" );
        fle.write( "# \n" );
        fle.write( format("currentChannel: %\n", currentChannel.asInteger ) );
        fle.write( format("allPresets: [%]\n",
            presetsMap.values.collect { |a| a.yamlId }.join(",") ) );
        fle.write( presetsMap.values.collect { |p| p.asYaml }.join("\n") );
        fle.close;

        format("Saved presets to %",path).postln;
    }

    *overwriteDefault { |presetsMap, currentChannel|
        RcPresets.saveToPath( presetsMap, presetsFile, currentChannel );
        AppClock.sched(0, {
            saveButton.states = [["Save Presets", Color.white, Color.green]];
            AppClock.sched(1, {
                saveButton.states = [["Save Presets", Color.black, Color.white]];
                nil
            });
            nil
        });
    }

    *new { |srcN,srcC,currPad|
        ^super.new.init(srcN,srcC,currPad);
    }

    init { |srcN,srcC,currPad|
        srcNoteNum  = srcN;
        srcChannel  = srcC;

        if ( currPad.notNil, {
            destNoteNum = currPad.noteNum;
            dialValues  = currPad.knobs.collect { |knb| knb.value };
            synthName   = currPad.synth.defName;
            filename    = currPad.filename;
            commentText = currPad.commentText.value;

            pendHash = Dictionary.new;

            currPad.activePendulums.do { |pend,idx|
                var yamlData = pend.asYaml(format( "%_%", this.pendId, idx));
                var yamlHash = yamlData.parseYAML;
                var pendId   = yamlHash.keys.asArray.first;

                pendHash[pendId] = RcPendulumBase.newFromYaml(yamlHash[pendId]);
            };
        }, {
            dialValues = []
        });
    }

    basename {
        if ( filename.notNil, { ^filename.split($/).reverse.first; });
    }

    comment {
        if ( commentText.ascii[0] == 45, {
            ^format( "% (%)", this.basename, synthName);
        }, {
            ^format( "% (%)", commentText, synthName);
        });
    }

    pendId {
        ^format( "%%", srcChannel.asDigits.padWithZero(3).join,
                       srcNoteNum.asDigits.padWithZero(3).join);
    }
    yamlIdBase {
        ^format("%,%", srcChannel.asInteger, srcNoteNum.asInteger );
    }
    yamlId {
        ^format("\"%\"", this.yamlIdBase );
    }

    setUpPendulums { |pad|
        if ( pendHash.notNil, {
            pendHash.values.do { |pend|
                var yamlData = pend.asYaml(format( "%", this.pendId));
                var yamlHash = yamlData.parseYAML;
                var pendId   = yamlHash.keys.asArray.first;
                var newPend  = RcPendulumBase.newWithPad(yamlHash[pendId],pad);

                if ( newPend.notNil, {
                    pad.pushPendulum(newPend.startFromYamlLoad);
                });
            }
        });
    }

    asYaml {
        var outStr = format("%:\n" ++
            "  chan: %\n"          ++
            "  note: %\n"          ++
            "  filename:%\n"       ++
            "  comment: \"%\"\n"   ++
            "  values: [%]\n"      ++
            "  destnotenum: %\n"   ++
            "  synth: %\n",
            this.yamlId,
            srcChannel.asInteger,
            srcNoteNum.asInteger,
            if ( filename.isNil, { "" }, { " \""++filename++"\"" }),
            commentText,
            dialValues.collect { |a| a.asInteger }.join(","),
            destNoteNum.asInteger,
            synthName);

        if ( if ( pendHash.notNil, { pendHash.notEmpty }, { false }), {
            var pendNames = format( "  pendulums: [%]\n",
                pendHash.keys.asArray.collect { |a| a }.join(","));

            var pendData = pendHash.collect { |pend,pendulId|
                pend.asYaml( pendulId.replace("pend") )
            }.values.asArray.join("\n");

            ^(format("%%%\n", outStr, pendNames, pendData));
        }, { ^outStr; });
    }

    asString {
        ^format( "Note: % Chan: % Dest: % Synth: % Vales: [%] Comment: % Fiename: %",
            srcNoteNum,
            srcChannel,
            destNoteNum,
            synthName,
            dialValues.collect { |a| a.asInteger }.join(","),
            commentText,
            filename)
    }
}