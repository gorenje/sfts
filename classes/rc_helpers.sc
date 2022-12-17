RcHelpers {

    *drpdwnSetValue { |val, drpList, callValueAction=true|
        if ( val > 64, {
            drpList.value = (drpList.value + 1).min(drpList.items.size-1);
        }, {
            drpList.value = 0.max(drpList.value - 1);
        });
        if ( callValueAction, { drpList.valueAction_(drpList.value) });
    }

    *addStaticText { |view,txtStr|
        var txt = StaticText.new(view, 250@25);
        txt.string = txtStr;
        txt.stringColor = Color.white;

    }

    *setValueIfEnabled { |linkMidi,checkbox|
        if ( linkMidi.enabled, { linkMidi.valueAction = checkbox.value; });
    }

    *toggleValueIfEnabled { |linkMidi|
        if ( linkMidi.enabled, { linkMidi.valueAction = linkMidi.value.not; });
    }
}