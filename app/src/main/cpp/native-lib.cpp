#include <jni.h>

#include <string>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <ifaddrs.h>
#include <netinet/in.h>

/**
 * Returns a list of available interfaces with their type and IP address in the following format:
 *  "type/interface name/IP address"
 *  Ex:
 *      "v4/wlan0/142.250.184.238"
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_myapplication_MainActivity_getIPAdddresses(
            JNIEnv* env,
            jobject /* this */) {
    struct ifaddrs *ifap, *ifa;
    struct sockaddr_in *sa;
    std::vector<std::string> addrs;

    getifaddrs (&ifap);
    for ( ifa = ifap;
        ifa != NULL ; ifa = ifa->ifa_next ) {
        if ( ifa->ifa_addr != NULL ) {
            if (ifa->ifa_addr->sa_family == AF_INET6) {
                struct sockaddr_in6* sa = (struct sockaddr_in6 *) ifa->ifa_addr;
                uint16_t* wordAddr = reinterpret_cast<uint16_t*>(&sa->sin6_addr.in6_u.u6_addr8[0]);
                // skip loopback and if low word is 0x0
                if (strcmp(ifa->ifa_name, "lo") != 0 && wordAddr != 0) {
                    char str[INET6_ADDRSTRLEN];
                    inet_ntop(AF_INET6, &sa->sin6_addr, str, INET6_ADDRSTRLEN);
                    std::string addr = std::string("v6/") + ifa->ifa_name + "/" + str;
                    addrs.push_back(addr);
                }
            } else if (ifa->ifa_addr->sa_family == AF_INET) {
                struct sockaddr_in *sa = (struct sockaddr_in *) ifa->ifa_addr;
                std::string addr = std::string("v4/") + ifa->ifa_name;
                std::string ip = inet_ntoa(sa->sin_addr);
                // skip private networks and loopback
                if (ip.substr(0, 3) != "10." &&
                    ip.substr(0,4) != "172." &&
                    ip.substr(0,4) != "192." &&
                    ip != "127.0.0.1") {
                    addr += "/" + ip;
                    addrs.push_back(addr);
                }
            }
        }
    }
    freeifaddrs(ifap);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray stringArray = env->NewObjectArray(addrs.size(), stringClass, nullptr);
    for (int i = 0; i < addrs.size(); ++i) {
        jstring str = env->NewStringUTF(addrs[i].c_str());
        env->SetObjectArrayElement(stringArray, i, str);
        env->DeleteLocalRef(str);
    }
    return stringArray;
}

