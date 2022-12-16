+ CheckBox {
    setBgColor { |clr|
        this.palette = this.palette.setColor(clr, 'base', 'inactive');
        this.palette = this.palette.setColor(clr, 'base', 'active');
    }
}