MAIN    = MicroTESK.SpecGuide.tex
SOURCE  = $(MAIN) intro.tex language.tex appendix.tex biblio.tex
INCLUDE = memfonts.sty memlays.sty memsty.sty titlepages.sty memnoidxnum.tex

SRC_DIR = src
INC_DIR = ../../../include/memoir/doc-src/

SRC_FILES = $(addprefix $(SRC_DIR), $(SOURCE))
INC_FILES = $(addprefix $(INC_DIR), $(INCLUDE))

all: $(SOURCE) $(INCLUDE)
	pdflatex $(MAIN)
	pdflatex $(MAIN)

$(SOURCE):
	cp $(SRC_DIR)/$@ $@

$(INCLUDE):
	cp $(INC_DIR)/$@ $@

clean:
	rm -rf *.*

