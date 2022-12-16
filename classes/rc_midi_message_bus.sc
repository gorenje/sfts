RcMidiMessageBus {
    var <>listeners;

    *new {
        ^super.new.init();
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
        listeners.removeFunc(f)
    }
}