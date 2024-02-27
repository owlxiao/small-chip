BUILD_DIR = ./build

export PATH := $(PATH):$(abspath ./utils)

verilog:
	mkdir -p $(BUILD_DIR)
	mill -i playground[chisel].runMain Elaborate -td $(BUILD_DIR)

help:
	mill -i __.test.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

idea:
	mill -i mill.scalalib.GenIdea/idea

.PHONY: test verilog help compile bsp reformat checkformat clean