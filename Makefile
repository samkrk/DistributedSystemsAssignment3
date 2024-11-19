# Variables
JAVAC = javac
JAVA = java
SRC_DIR = src
BIN_DIR = bin
LIB_DIR = lib
MAIN_CLASS = main.Main
CLASSPATH = $(BIN_DIR):$(LIB_DIR)/gson-2.8.9.jar:$(LIB_DIR)/junit-4.13.2.jar:$(LIB_DIR)/hamcrest-core-1.3.jar

# List of Json Config Files
CONFIG_FILE = src/main/resources/test1.json
# CONFIG_FILE = src/main/resources/test2.json
# CONFIG_FILE = src/main/resources/test3.json
# CONFIG_FILE = src/main/resources/test4.json

# Compile the Java files
compile:
	mkdir -p $(BIN_DIR)
	$(JAVAC) -cp $(CLASSPATH) -d $(BIN_DIR) $(SRC_DIR)/main/*.java

# Run the Main class
run: compile
	$(JAVA) -cp $(CLASSPATH) $(MAIN_CLASS) $(CONFIG_FILE)

# Run the Junit tests
test: compile
	javac -cp $(CLASSPATH) -d $(BIN_DIR) src/test/*.java
	java -cp $(CLASSPATH) org.junit.runner.JUnitCore test.PaxosTest

# Clean up the compiled files
clean:
	rm -rf $(BIN_DIR)/*
