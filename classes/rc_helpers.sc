RcHelpers {
    *drpdwnSetValue { |val, drpList, callValueAction=true|
        if ( val > 64, {
            drpList.value = (drpList.value + 1).min(drpList.items.size-1);
        }, {
            drpList.value = 0.max(drpList.value - 1);
        });
        if ( callValueAction, { drpList.valueAction_(drpList.value) });
    }

    *addStaticText { |view,txtStr,loc=nil|
        var txt = StaticText.new(view, if ( loc.isNil, { 250@25; }, { loc }));
        txt.string = txtStr;
        txt.stringColor = Color.white;
        ^txt;
    }

    *setValueIfEnabled { |linkMidi,checkbox|
        if ( linkMidi.enabled, { linkMidi.valueAction = checkbox.value; });
    }

    *toggleValueIfEnabled { |linkMidi|
        if ( linkMidi.enabled, { linkMidi.valueAction = linkMidi.value.not; });
    }

    *buttonBW{ |txt,view,sze,action|
        var btn = Button(view, sze);
        btn.states = [[txt, Color.black, Color.white]];
        btn.action = action;
        ^btn;
    }

    *modalView { |txt|
        var vw = Window(txt,
            resizable: false,
            bounds: Rect(
                Window.screenBounds.width / 2,
                (Window.screenBounds.height-180) / 2,
                330, 230
            )
        ).front;
        vw.alwaysOnTop = true;
        ^vw;
    }

    *messageView { |message|
        var w = RcHelpers.modalView("Message");

        RcHelpers.addStaticText(
            w, message, Rect(20, 20, 300, 150)
        ).stringColor = Color.black;

        RcHelpers.buttonBW("Ok", w, Rect(100, 180, 120, 25), { w.close; });
    }

    *confirmExit { |action|
        var w = RcHelpers.modalView("Really Quit?");

        RcHelpers.addStaticText(
            w, "Really Quit?", Rect(130, 60, 200, 30)
        ).stringColor = Color.black;

        RcHelpers.buttonBW("Cancel", w, Rect(10, 180, 120, 25), { w.close; });
        RcHelpers.buttonBW("Yes Quit", w, Rect(200, 180, 120, 25), action );
    }
}