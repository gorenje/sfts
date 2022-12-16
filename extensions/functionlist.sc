+ FunctionList {
    removeAllOccurances { |funct|
        array.removeAllSuchThat({ |item|
            item == funct
        });
    }
}