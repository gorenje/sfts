// Class designed to be a state counter for a Midi dial. This ensures that
// dropdown values are scrolled through too quickly.
RcGuiCounter {
    var <>value;
    var <>limit;
    var <>initValue;

    *new { |val,lmt|
        ^super.new.init(val,lmt);
    }

    init { |init_val, lmt_val|
        this.value = init_val;
        this.limit = lmt_val;
        this.initValue = init_val;
    }

    reset {
        this.value = this.initValue;
        ^this;
    }

    += { |v|
        this.value = this.value + v;
        ^this;
    }

    -= { |v|
        this.value = this.value - v;
        ^this;
    }

    == { arg aMagnitude; ^(this.value == aMagnitude) }
    != { arg aMagnitude; ^(this == aMagnitude).not }

    < { arg aMagnitude; ^(this.value < aMagnitude) }
    > { arg aMagnitude; ^aMagnitude < this.value }
    <= { arg aMagnitude; ^(aMagnitude < this.value).not }
    >= { arg aMagnitude; ^(this.value < aMagnitude).not }

    belowLimit {
        ^(this < this.limit.neg);
    }
    pastLimit {
        ^(this > this.limit);
    }

    willSomethingChange { |val, funct|
        this += if ( val > 64, 1, -1);

        if ( ((val > 64) && this.pastLimit) || ((val < 64) && this.belowLimit),{
            ~runOnAppClock.value({
                funct.value(val);
                this.reset;
            });
        });
    }
}
