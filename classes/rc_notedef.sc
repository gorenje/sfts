RcNoteDef {
    var <>synth;
    var <>ezText;
    var <>isFree;
    var <>noteNum;
    var <>synthType;
    var <>buffer;
    var <>filename;
    var <>presetData;

    *new { |ez_text|
        ^super.new.init(ez_text);
    }

    init { |ez_text|
        ezText = ez_text;
        this.clear_();
    }

    show { |labelString, textValue, synth|
        synth                   = synth;
        noteNum                 = labelString;
        isFree                  = false;
        ezText.value            = textValue;
        ezText.labelView.string = labelString;
        ezText.visible_(true);
    }

    hide {
        ezText.visible_(false);
        this.clear_();
    }

    hideAndRelease {
        synth.set(\fadeTime, 2);
        synth.release;
        synth.free;
        if ( buffer.notNil, { buffer.free; buffer = nil; });
        this.hide();
    }

    visible {
        ^ezText.visible;
    }

    label {
        ^ezText.labelView.string;
    }

    text {
        ^ezText.value;
    }

    clear_ {
        synth      = nil;
        isFree     = true;
        buffer     = nil;
        noteNum    = nil;
        filename   = nil;
        presetData = nil;
    }
}
