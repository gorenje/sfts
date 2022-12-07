RcSample {
    var <>filename;
    var <>comment;
    var <>basename;
    var <>duration;

    *new { |fname,bname|
        ^super.new.init(fname,bname);
    }

    init { |fname,bname|
        filename = fname;
        basename = bname;
        comment  = "-";
        duration = "0s";
    }

    == { |a|
        ^(a.filename == this.filename);
    }
}