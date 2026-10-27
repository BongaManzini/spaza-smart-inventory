CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -O2 -Isrc

SRCS = src/main.cpp src/PaymentGateway.cpp src/ReceiptPrinter.cpp src/InventoryEngine.cpp
OBJS = $(SRCS:.cpp=.o)
TARGET = spaza_terminal

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) -o $(TARGET) $(OBJS)

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

.PHONY: all clean
