cmake_minimum_required(VERSION 3.12)
project(SpazaTerminal LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(src)

add_executable(spaza_terminal
    src/main.cpp
    src/PaymentGateway.cpp
    src/ReceiptPrinter.cpp
    src/InventoryEngine.cpp
)
