+ Array {
	repeatItems { |cnt=3|
		var newAry = List.new;
		this.do { |i|
			cnt.do {
				newAry.add( i )
			}
		};
		^newAry.asArray;
	}

    padWithZero { |size = 3|
        if ( size <= this.size, {
            ^this;
        }, {
            var l = List.newFrom(this);
            while( { size > l.size }, {
                l.addFirst(0);
            });
            ^l.asArray;
        });
    }
}