RcMidiMessageBus {
    var <>listeners;

    *new {
        ^super.new.init();
    }

    connectTo { |midiin|
        midiin.addFuncTo(\control, { |src, chan, num, val|
            this.push( [src, chan, num, val] )
        });
    }

    init {
        listeners = FunctionList.new([]);
    }

    push { |msg|
        listeners.valueArray([Date.localtime.rawSeconds] ++ msg)
    }

    add_listener { |f|
        listeners.addFunc(f)
    }

    remove_listener { |f|
        listeners.removeAllOccurances(f)
    }
}