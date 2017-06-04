#include <jni.h>
#include <string>

extern "C"
jstring
Java_com_hsfl_speakshot_cpp_Test_test(JNIEnv* env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
