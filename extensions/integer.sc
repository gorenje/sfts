+ Integer {
	freq { |lmt=108|
		^[20,20000,\lin].asSpec.map([0,lmt,\lin].asSpec.unmap(this));
	}

    clampTo { |a=0, b=127|
        ^this.nearestInList(Interval(a,b).asArray)
    }
}
