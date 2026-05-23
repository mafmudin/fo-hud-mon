#include "proc_reader.h"
#include <fstream>
#include <sstream>

namespace devhud {

std::string readProcFile(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) return "";
    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

std::map<std::string, std::string> parseProcStatus() {
    std::map<std::string, std::string> result;
    std::ifstream file("/proc/self/status");
    std::string line;

    while (std::getline(file, line)) {
        size_t colonPos = line.find(':');
        if (colonPos == std::string::npos) continue;

        std::string key = line.substr(0, colonPos);
        std::string value = line.substr(colonPos + 1);

        size_t start = value.find_first_not_of(" \t");
        if (start != std::string::npos)
            value = value.substr(start);

        result[key] = value;
    }
    return result;
}

} // namespace devhud
